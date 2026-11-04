package com.example.daycalendar;

import static org.junit.Assert.*;
import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.io.*;
import java.time.*;
import java.util.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class WidgetFullDayTest {
    public static class CalendarService extends EventWidgetService {
        @Override List<CalendarEvent> loadEvents(LocalDate day) {
            ZoneId zone=ZoneId.systemDefault();
            List<CalendarEvent> events=new ArrayList<>();
            events.add(new CalendarEvent(1,"All day","",day.atStartOfDay(zone).toInstant().toEpochMilli(),day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),true,0xff769885));
            for(int hour=0;hour<23;hour++) events.add(new CalendarEvent(10+hour,"Meeting at "+hour,"Room A",day.atTime(hour,0).atZone(zone).toInstant().toEpochMilli(),day.atTime(hour,30).atZone(zone).toInstant().toEpochMilli(),false,0xff769885));
            events.add(new CalendarEvent(99,"Last appointment","Home",day.atTime(23,45).atZone(zone).toInstant().toEpochMilli(),day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),false,0xffa38bb5));
            return events;
        }
    }
    private ServiceController<CalendarService> controller;
    private RemoteViewsService.RemoteViewsFactory factory;
    @Before public void loadDay() {
        controller=Robolectric.buildService(CalendarService.class).create();
        factory=controller.get().onGetViewFactory(new Intent().putExtra(WidgetRaster.WIDTH_DP,320));
        factory.onCreate();factory.onDataSetChanged();
    }
    @After public void cleanUp(){factory.onDestroy();controller.destroy();}
    private static int bitmapBytes(RemoteViews view) throws Exception {
        return (int)RemoteViews.class.getDeclaredMethod("estimateMemoryUsage").invoke(view);
    }
    @Test public void fullDaySurvivesFrameworkCollectionMemoryLimitAndParcelBudget() throws Exception {
        // Newer Android stops adding items once the aggregate bitmap cap is reached.
        // 1080x2560 gives 16.6MB: the old 320dp/3x bitmaps reached it near 20:00.
        long cap=6L*1080*2560;
        long oldDayBytes=0;
        for(int i=0;i<WidgetRaster.SLICE_COUNT;i++) {
            var size=WidgetRaster.size(320,WidgetRaster.sliceHeight(i),3);
            oldDayBytes+=4L*size.width()*size.height();
        }
        assertTrue("The previous raster collection exceeds the display budget",oldDayBytes>cap);
        long used=0;int delivered=0;
        RemoteViews.RemoteCollectionItems.Builder items=new RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(factory.getViewTypeCount());
        for(int position=0;position<factory.getCount();position++) {
            RemoteViews row=factory.getViewAt(position);assertNotNull(row);
            used+=bitmapBytes(row);if(used>=cap)break;
            items.addItem(factory.getItemId(position),row);delivered++;
        }
        assertEquals("All-day row and all 49 time slices must arrive",50,delivered);
        assertEquals("The modern collection must carry no raster bitmaps",0,used);
        RemoteViews.RemoteCollectionItems collection=items.build();
        assertEquals(50,collection.getItemCount());
        Parcel parcel=Parcel.obtain();
        try {
            collection.writeToParcel(parcel,0);
            // Four responsive sizes must fit Android's ~800KB shared parcel budget.
            System.out.println("Full-day native collection parcel bytes: "+parcel.dataSize());
            assertTrue("Whole day must fit both size variants: "+parcel.dataSize(),parcel.dataSize()<150000);
        } finally {parcel.recycle();}
    }
    private View inflate(int position) {
        View row=factory.getViewAt(position).apply(controller.get(),new FrameLayout(controller.get()));
        // The widget host honors a row's LayoutParams; directly measuring UNSPECIFIED
        // would incorrectly expand a slice to fit its intentionally clipped event children.
        int height=row.getLayoutParams().height;
        row.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(Math.max(0,height),height>=0?View.MeasureSpec.EXACTLY:View.MeasureSpec.UNSPECIFIED));
        row.layout(0,0,960,row.getMeasuredHeight());return row;
    }
    @Test public void lateEventsAndTerminal24LabelAreInflatedAtFullHeight() {
        Set<String> labels=new HashSet<>();boolean lastAppointment=false;
        for(int slice=40;slice<WidgetRaster.SLICE_COUNT;slice++) {
            View row=inflate(slice+1);
            assertEquals(WidgetRaster.sliceHeight(slice)*3,row.getHeight());
            List<String> text=new ArrayList<>();collectText(row,text);labels.addAll(text);
            if(text.contains("Last appointment"))lastAppointment=true;
        }
        for(String hour:new String[]{"20:00","21:00","22:00","23:00","24:00"})assertTrue(hour,labels.contains(hour));
        assertTrue("23:45 event must exist in the final slices",lastAppointment);
    }
    @Test @Config(sdk={26,35},qualifiers="xxhdpi") public void hostListCanScrollToLastRowAfterRefresh() {
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
        View widget=new RemoteViews(activity.getPackageName(),R.layout.widget).apply(activity,new FrameLayout(activity));
        ListView list=widget.findViewById(R.id.events);
        BaseAdapter adapter=new BaseAdapter(){
            public int getCount(){return factory.getCount();}
            public Object getItem(int p){return p;}
            public long getItemId(int p){return factory.getItemId(p);}
            public boolean hasStableIds(){return factory.hasStableIds();}
            public View getView(int p,View old,ViewGroup parent){return factory.getViewAt(p).apply(activity,parent);}
        };
        list.setAdapter(adapter);activity.setContentView(widget);
        layoutWidget(widget);list.setSelection(factory.getCount()-1);layoutWidget(widget);
        assertEquals(factory.getCount()-1,list.getLastVisiblePosition());
        adapter.notifyDataSetChanged();layoutWidget(widget);
        assertEquals("Minute refresh must preserve the end of the day",factory.getCount()-1,list.getLastVisiblePosition());
        if(Build.VERSION.SDK_INT>=31) {
            List<String> text=new ArrayList<>();collectText(list,text);assertTrue(text.contains("24:00"));
        } else {
            ImageView last=list.getChildAt(list.getChildCount()-1).findViewById(R.id.last_hour_image);
            assertNotNull(last);assertEquals(View.VISIBLE,last.getVisibility());assertNotNull(last.getDrawable());
            assertTrue(last.getContentDescription().toString().contains("24:00"));
        }
        activity.finish();
    }
    private static void layoutWidget(View widget) {
        widget.measure(View.MeasureSpec.makeMeasureSpec(1056,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1200,View.MeasureSpec.EXACTLY));
        widget.layout(0,0,1056,1200);
    }
    private static void collectText(View v,List<String> text) {
        if(v instanceof TextView)text.add(((TextView)v).getText().toString());
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)collectText(((ViewGroup)v).getChildAt(i),text);
    }
    @Test public void clockNearMidnightAndResponsiveSizesCannotDropTheLastRows() {
        LocalDate day=LocalDate.of(2026,11,4);
        RemoteViews end=WidgetNativeTimeline.slice(controller.get(),320,48,day,Collections.emptyList(),day.atTime(23,58).atZone(ZoneId.systemDefault()));
        View view=end.apply(controller.get(),new FrameLayout(controller.get()));
        List<String> labels=new ArrayList<>();collectText(view,labels);
        assertTrue(labels.contains("24:00"));
        assertFalse("Live time is in the header and must not overlap 24:00",labels.contains("23:58"));
        List<android.util.SizeF> sizes=new ArrayList<>();
        for(int i=0;i<16;i++)sizes.add(new android.util.SizeF(200+20*i,400));
        List<android.util.SizeF> selected=DayWidget.collectionSizes(sizes);
        assertEquals(4,selected.size());assertEquals(200,selected.get(0).getWidth(),0);
        assertEquals(500,selected.get(3).getWidth(),0);
    }
    @Test public void renderLateNightPreview() throws IOException {
        Bitmap image=Bitmap.createBitmap(960,8*36*3+24*3,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(image);int top=0;
        for(int slice=40;slice<WidgetRaster.SLICE_COUNT;slice++) {
            View row=inflate(slice+1);canvas.save();canvas.translate(0,top);row.draw(canvas);canvas.restore();top+=row.getHeight();
        }
        File dir=new File(System.getProperty("calendar.widget.screenshots","build/render-previews"));
        if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create preview directory");
        try(OutputStream out=new FileOutputStream(new File(dir,"android-20-to-24.png"))) {assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}
    }
}
