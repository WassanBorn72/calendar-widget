package com.example.daycalendar;

import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.io.*;
import java.time.*;
import java.util.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class WidgetColorTest {
    private final Context context=RuntimeEnvironment.getApplication();
    private final LocalDate day=LocalDate.of(2026,11,4);
    private RemoteViews row(int color){
        var event=new CalendarEvent(1,"Color sample","Room A",day.atTime(10,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),day.atTime(11,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),false,color);
        return WidgetNativeTimeline.slice(context,320,20,day,DayTimeline.layout(Collections.singletonList(event),day,ZoneId.systemDefault()),day.plusDays(1).atStartOfDay(ZoneId.systemDefault()));
    }
    private View apply(RemoteViews views){View view=views.apply(context,new FrameLayout(context));layout(view);return view;}
    private static void layout(View view){view.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(view.getLayoutParams().height,View.MeasureSpec.EXACTLY));view.layout(0,0,960,view.getMeasuredHeight());}
    private static Bitmap draw(View view){Bitmap b=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);view.draw(new Canvas(b));return b;}
    @Test public void paletteKeepsDistinctHuesAndDarkTextContrast(){
        int red=EventColors.fill(0xffd50000),blue=EventColors.fill(0xff4285f4),green=EventColors.fill(0xff0b8043);
        assertTrue(Color.red(red)>Color.green(red)&&Color.red(red)>Color.blue(red));
        assertTrue(Color.blue(blue)>Color.red(blue)&&Color.blue(blue)>Color.green(blue));
        assertTrue(Color.green(green)>Color.red(green)&&Color.green(green)>Color.blue(green));
        for(int color:new int[]{0xffd50000,0xff4285f4,0xff0b8043,0xfff6bf26,0xff8e24aa,0xff000000,0xffffffff})
            assertTrue("Muted time text must retain 4.5:1 contrast",contrast(EventColors.fill(color),EventColors.SECONDARY_TEXT)>=4.5);
    }
    private static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    private static double luminance(int color){double[] c={Color.red(color)/255.0,Color.green(color)/255.0,Color.blue(color)/255.0};for(int i=0;i<3;i++)c[i]=c[i]<=.04045?c[i]/12.92:Math.pow((c[i]+.055)/1.055,2.4);return .2126*c[0]+.7152*c[1]+.0722*c[2];}
    @Test public void nativeBackgroundAndAccentUseTheEventPaletteWithoutBitmaps() throws Exception {
        for(int color:new int[]{0xffd50000,0xff4285f4,0xff0b8043}) {
            RemoteViews views=row(color);View view=apply(views);Bitmap bitmap=draw(view);
            assertEquals(EventColors.fill(color),bitmap.getPixel(220*3,18*3));
            assertEquals(0,(int)RemoteViews.class.getDeclaredMethod("estimateMemoryUsage").invoke(views));
        }
    }
    @Test public void changedEventColorReplacesOldTintWhenRowIsReused(){
        View reused=apply(row(0xffd50000));RemoteViews blue=row(0xff4285f4);blue.reapply(context,reused);layout(reused);
        assertTrue(draw(apply(blue)).sameAs(draw(reused)));
        assertEquals(EventColors.fill(0xff4285f4),draw(reused).getPixel(220*3,18*3));
    }
    @Test @Config(sdk={26,35},qualifiers="xxhdpi") public void allDayCardsUseTheSamePaletteOnOldAndNewAndroid(){
        RemoteViews card=new RemoteViews(context.getPackageName(),R.layout.widget_event);
        EventCardViews.color(card,R.id.card_fill,R.id.card_outline,R.id.card_accent,0xff8e24aa);
        card.setTextViewText(R.id.title,"All day sample");View view=card.apply(context,new FrameLayout(context));
        view.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));view.layout(0,0,960,view.getMeasuredHeight());
        assertEquals(EventColors.fill(0xff8e24aa),draw(view).getPixel(220*3,12*3));
    }
    @Test public void renderColorPreview() throws IOException {
        Bitmap image=Bitmap.createBitmap(960,108*3*5,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);
        int[] colors={0xffd50000,0xff4285f4,0xff0b8043,0xfff6bf26,0xff8e24aa};
        for(int i=0;i<colors.length;i++)for(int slice=20;slice<=22;slice++) {
            var event=new CalendarEvent(1,"Color sample","Room A",day.atTime(10,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),day.atTime(11,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),false,colors[i]);
            RemoteViews views=WidgetNativeTimeline.slice(context,320,slice,day,DayTimeline.layout(Collections.singletonList(event),day,ZoneId.systemDefault()),day.plusDays(1).atStartOfDay(ZoneId.systemDefault()));
            View view=apply(views);canvas.save();canvas.translate(0,(i*3+slice-20)*108);view.draw(canvas);canvas.restore();
        }
        File dir=new File(System.getProperty("calendar.widget.screenshots","build/render-previews"));if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create preview directory");
        try(OutputStream out=new FileOutputStream(new File(dir,"android-event-colors.png"))){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}
    }
}
