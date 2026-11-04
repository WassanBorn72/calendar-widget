package com.example.daycalendar;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.app.*;
import android.content.*;
import android.os.*;
import android.provider.CalendarContract;
import android.view.*;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.time.*;
import java.util.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="xxhdpi")
public class WidgetEventTapTest {
    private final Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
    private final LocalDate day=LocalDate.of(2026,11,4);
    private CalendarEvent event(long id,int beginHour,int endHour){return new CalendarEvent(id,"Event "+id,"Room",day.atTime(beginHour,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),day.atTime(endHour,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),false,0xff4285f4);}
    private RemoteViews.RemoteCollectionItems collection(List<CalendarEvent> events){
        RemoteViews row=WidgetNativeTimeline.slice(activity,320,20,day,DayTimeline.layout(events,day,ZoneId.systemDefault()),day.plusDays(1).atStartOfDay(ZoneId.systemDefault()));
        return new RemoteViews.RemoteCollectionItems.Builder().addItem(-21,row).setHasStableIds(true).build();
    }
    private ListView host(RemoteViews.RemoteCollectionItems items){
        RemoteViews widget=new RemoteViews(activity.getPackageName(),R.layout.widget);
        widget.setRemoteAdapter(R.id.events,items);widget.setPendingIntentTemplate(R.id.events,WidgetEventAction.template(activity,56));
        // Robolectric 4.14 ShadowActivity.IntentSenderRequest.send drops fillInIntent.
        // Use the application-context host so the framework's Context path preserves it.
        Context hostContext=RuntimeEnvironment.getApplication();
        android.appwidget.AppWidgetHostView host=new android.appwidget.AppWidgetHostView(hostContext);
        View view=widget.apply(hostContext,host);host.addView(view);activity.setContentView(host);
        host.measure(View.MeasureSpec.makeMeasureSpec(1056,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1200,View.MeasureSpec.EXACTLY));host.layout(0,0,1056,1200);
        return view.findViewById(R.id.events);
    }
    private static List<View> hits(View view){
        List<View> found=new ArrayList<>();if(view.getId()==R.id.native_event_hit)found.add(view);
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)found.addAll(hits(((ViewGroup)view).getChildAt(i)));
        return found;
    }
    @Test public void overlappingNativeEventsEachOpenTheirOwnId(){
        ListView list=host(collection(Arrays.asList(event(101,10,11),event(202,10,12))));
        List<View> targets=hits(list);assertEquals(2,targets.size());
        for(int i=0;i<targets.size();i++){
            assertTrue(targets.get(i).performClick());Intent route=shadowOf(activity).getNextStartedActivity();assertNotNull(route);
            assertEquals(OpenCalendarEventActivity.class.getName(),route.getComponent().getClassName());
            assertEquals(i==0?202:101,route.getLongExtra(WidgetEventAction.ID,-1));
        }
    }
    @Test public void collectionTemplateHasFixedPrivateDestinationAndAllowsFillIn(){
        PendingIntent pending=WidgetEventAction.template(activity,7);
        Intent base=shadowOf(pending).getSavedIntent();
        assertEquals(OpenCalendarEventActivity.class.getName(),base.getComponent().getClassName());
        assertEquals(activity.getPackageName(),base.getComponent().getPackageName());
        assertEquals("daycalendar://open-widget/7",base.getDataString());
        assertFalse(pending.isImmutable());
    }
    @Test public void legacySliceTargetsOneEventOrRequestsChoiceForOverlap(){
        List<DayTimeline.Block> one=DayTimeline.layout(Collections.singletonList(event(101,10,11)),day,ZoneId.systemDefault());
        Intent direct=WidgetEventAction.slice(day,20,one);assertEquals(101,direct.getLongExtra(WidgetEventAction.ID,-1));
        List<DayTimeline.Block> overlap=DayTimeline.layout(Arrays.asList(event(101,10,11),event(202,10,12)),day,ZoneId.systemDefault());
        Intent picker=WidgetEventAction.slice(day,20,overlap);assertFalse(picker.hasExtra(WidgetEventAction.ID));
        assertEquals(day.toString(),picker.getStringExtra(WidgetEventAction.DATE));
        assertEquals(2,WidgetEventAction.inRange(overlap,picker.getIntExtra(WidgetEventAction.FROM,-1),picker.getIntExtra(WidgetEventAction.TO,-1)).size());
        assertNull(WidgetEventAction.slice(day,30,one));
    }
    @Test public void privateRouteForwardsTheSelectedInstanceToGoogle(){
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(android.Manifest.permission.READ_CALENDAR);
        CalendarEvent e=event(202,10,12);Intent route=new Intent(activity,OpenCalendarEventActivity.class).putExtras(WidgetEventAction.event(e));
        OpenCalendarEventActivity launcher=Robolectric.buildActivity(OpenCalendarEventActivity.class,route).create().get();
        Intent google=shadowOf(launcher).getNextStartedActivity();assertNotNull(google);
        assertEquals(CalendarEventLauncher.GOOGLE_CALENDAR,google.getPackage());assertEquals("content://com.android.calendar/events/202",google.getDataString());
        assertEquals(e.begin(),google.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,-1));assertTrue(launcher.isFinishing());
    }
    public static class PickerActivity extends OpenCalendarEventActivity {
        @Override protected void query(Runnable task){task.run();}
        @Override protected List<CalendarEvent> loadEvents(LocalDate day){
            List<CalendarEvent> events=new ArrayList<>();
            for(int id:new int[]{101,202})events.add(new CalendarEvent(id,"Event "+id,"",day.atTime(10,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),day.atTime(11,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),false,0xff4285f4));
            return events;
        }
    }
    private static ListView findList(View view){if(view instanceof ListView)return (ListView)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){ListView found=findList(((ViewGroup)view).getChildAt(i));if(found!=null)return found;}return null;}
    @Test public void legacyPickerHasVisibleChoicesAndOpensTheSelectedEvent(){
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(android.Manifest.permission.READ_CALENDAR);
        Intent route=new Intent(activity,PickerActivity.class).putExtra(WidgetEventAction.DATE,day.toString()).putExtra(WidgetEventAction.FROM,590).putExtra(WidgetEventAction.TO,620);
        PickerActivity picker=Robolectric.buildActivity(PickerActivity.class,route).setup().get();
        ListView list=findList(picker.findViewById(android.R.id.content));assertNotNull(list);assertEquals(2,list.getCount());assertTrue(list.getLayoutParams().height>0);
        list.performItemClick(null,1,1);Intent google=shadowOf(picker).getNextStartedActivity();
        assertEquals("content://com.android.calendar/events/202",google.getDataString());assertTrue(picker.isFinishing());
    }

    @Test @Config(sdk={26,35}) public void mutableTemplateAcceptsTheEventExtrasWhenSentDirectly() throws Exception {
        WidgetEventAction.template(activity,56).send(activity,0,WidgetEventAction.event(event(202,10,12)));
        assertEquals(202,shadowOf(activity).getNextStartedActivity().getLongExtra(WidgetEventAction.ID,-1));
    }

    @Test public void allDayRowClickPreservesItsIdentityAndAllDayFlag(){
        long begin=day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),end=day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        CalendarEvent e=new CalendarEvent(303,"All day","",begin,end,true,0xff4285f4);
        RemoteViews card=new RemoteViews(activity.getPackageName(),R.layout.widget_event);
        card.setTextViewText(R.id.title,e.title());card.setOnClickFillInIntent(R.id.card_root,WidgetEventAction.event(e));
        ListView list=host(new RemoteViews.RemoteCollectionItems.Builder().addItem(e.id(),card).build());
        assertTrue(list.performItemClick(list.getChildAt(0),0,e.id()));
        Intent route=shadowOf(activity).getNextStartedActivity();assertNotNull(route);
        assertEquals(303,route.getLongExtra(WidgetEventAction.ID,-1));assertEquals(begin,route.getLongExtra(WidgetEventAction.BEGIN,-1));assertEquals(end,route.getLongExtra(WidgetEventAction.END,-1));assertTrue(route.getBooleanExtra(WidgetEventAction.ALL_DAY,false));
    }

}
