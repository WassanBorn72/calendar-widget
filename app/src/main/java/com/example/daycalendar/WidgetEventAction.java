package com.example.daycalendar;

import android.app.PendingIntent;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import java.time.*;
import java.util.*;

final class WidgetEventAction {
    static final String ID="widget_event_id", BEGIN="widget_event_begin", END="widget_event_end", ALL_DAY="widget_event_all_day";
    static final String DATE="widget_event_date", FROM="widget_event_from", TO="widget_event_to";
    private WidgetEventAction() { }
    static PendingIntent template(Context context,int widgetId) {
        Intent base=new Intent(context,OpenCalendarEventActivity.class)
                .setAction("com.example.daycalendar.OPEN_WIDGET_EVENT")
                .setData(Uri.parse("daycalendar://open-widget/"+widgetId));
        // Collection fill-in extras require mutability. The destination is an explicit,
        // private activity; incoming URLs, components and actions are never forwarded.
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;
        if(Build.VERSION.SDK_INT>=31)flags|=PendingIntent.FLAG_MUTABLE;
        return PendingIntent.getActivity(context,widgetId,base,flags);
    }
    static Intent event(CalendarEvent e) {return new Intent().putExtra(ID,e.id()).putExtra(BEGIN,e.begin()).putExtra(END,e.end()).putExtra(ALL_DAY,e.allDay());}
    static List<CalendarEvent> inRange(List<DayTimeline.Block> blocks,int from,int to) {
        List<CalendarEvent> selected=new ArrayList<>();
        for(var b:blocks)if(b.start<to && (b.end>from || (b.start==b.end && b.start>=from)))selected.add(b.event);
        return selected;
    }
    static Intent slice(LocalDate day,int index,List<DayTimeline.Block> blocks) {
        int from=Math.max(0,(index*WidgetRaster.SLICE_HEIGHT-12)*60/TimelineView.HOUR);
        int to=Math.min(1440,(index*WidgetRaster.SLICE_HEIGHT+WidgetRaster.sliceHeight(index)-12)*60/TimelineView.HOUR);
        List<CalendarEvent> events=inRange(blocks,from,to);
        if(events.isEmpty())return null;
        if(events.size()==1)return event(events.get(0));
        return new Intent().putExtra(DATE,day.toString()).putExtra(FROM,from).putExtra(TO,to);
    }
}
