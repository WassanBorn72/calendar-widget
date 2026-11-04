package com.example.daycalendar;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.text.*;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.RemoteViews;
import java.time.*;
import java.util.*;

/** Native text and shapes avoid the full-collection bitmap cap on newer hosts. */
@TargetApi(31)
final class WidgetNativeTimeline {
    private static final int DP = TypedValue.COMPLEX_UNIT_DIP;
    private WidgetNativeTimeline() { }

    static RemoteViews slice(Context c, int width, int index, LocalDate day,
                             List<DayTimeline.Block> blocks, ZonedDateTime updatedAt) {
        int offset = index*WidgetRaster.SLICE_HEIGHT, height = WidgetRaster.sliceHeight(index);
        RemoteViews row = new RemoteViews(c.getPackageName(), R.layout.widget_native_slice);
        // Hosts recycle rows with RemoteViews.reapply(). Replace dynamic children
        // before adding this snapshot, otherwise old events and markers accumulate.
        row.removeAllViews(R.id.slice_root);
        row.setViewLayoutHeight(R.id.slice_root, height, DP);
        for (int hour=0;hour<=24;hour++) {
            float y=12+hour*TimelineView.HOUR-offset;
            if (y>=-8 && y<=height+8) {
                line(c,row,width,y,1,0xffbcc9bb);
                label(c,row,String.format(Locale.ROOT,"%02d:00",hour),y-8,0xff53655e);
            }
            if (hour<24 && y+TimelineView.HOUR/2f>=0 && y+TimelineView.HOUR/2f<=height)
                line(c,row,width,y+TimelineView.HOUR/2f,1,0xffdfe5da);
        }
        for (var block : blocks) {
            float top=(float)(12+block.start*TimelineView.HOUR/60), bottom=(float)(12+block.end*TimelineView.HOUR/60);
            if (bottom<offset || top>offset+height) continue;
            float laneWidth=(width-TimelineView.AXIS-4f)/block.columns;
            float left=TimelineView.AXIS+2+laneWidth*block.lane, eventWidth=laneWidth-3, eventHeight=Math.max(1,bottom-top);
            if (eventWidth<=0) continue;
            RemoteViews event=new RemoteViews(c.getPackageName(),R.layout.widget_native_event);
            position(event,R.id.native_event,left,top-offset,eventWidth,eventHeight);
            EventCardViews.color(event,R.id.native_event_fill,R.id.native_event_outline,R.id.native_accent,block.event.color());
            event.setTextViewText(R.id.native_event_title,block.event.title());
            event.setTextViewText(R.id.native_event_time,block.event.time());
            event.setTextViewText(R.id.native_event_location,block.event.location());
            if (eventHeight<34) {
                event.setViewVisibility(R.id.native_event_time,View.GONE);
                event.setViewVisibility(R.id.native_event_location,View.GONE);
                event.setViewVisibility(R.id.native_event_title,eventHeight<10?View.GONE:View.VISIBLE);
                event.setViewLayoutMargin(R.id.native_event_title,RemoteViews.MARGIN_TOP,0,DP);
                event.setViewLayoutHeight(R.id.native_event_title,eventHeight,DP);
                event.setInt(R.id.native_event_title,"setMaxLines",1);
                event.setInt(R.id.native_event_title,"setGravity",Gravity.CENTER_VERTICAL);
                event.setTextViewTextSize(R.id.native_event_title,TypedValue.COMPLEX_UNIT_SP,Math.min(14,eventHeight-6));
            } else {
                int maxLines=eventHeight>=58?2:1;
                event.setInt(R.id.native_event_title,"setMaxLines",maxLines);
                float textWidth=eventWidth-14;
                if (textWidth<=0) {
                    event.setViewVisibility(R.id.native_event_title,View.GONE);
                    event.setViewVisibility(R.id.native_event_time,View.GONE);
                    event.setViewVisibility(R.id.native_event_location,View.GONE);
                } else {
                    float density=c.getResources().getDisplayMetrics().density;
                    TextPaint paint=new TextPaint();paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
                    paint.setTextSize(14*c.getResources().getDisplayMetrics().scaledDensity);
                    StaticLayout title=StaticLayout.Builder.obtain(block.event.title(),0,block.event.title().length(),paint,Math.max(1,(int)(textWidth*density)))
                            .setIncludePad(false).setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END).build();
                    float locationTop=15+title.getHeight()/density+3;
                    event.setViewLayoutMargin(R.id.native_event_location,RemoteViews.MARGIN_TOP,locationTop,DP);
                    if (block.event.location().isEmpty() || locationTop+14>eventHeight-4)
                        event.setViewVisibility(R.id.native_event_location,View.GONE);
                }
            }
            row.addView(R.id.slice_root,event);
        }
        if (day.equals(updatedAt.toLocalDate())) {
            LocalTime now=updatedAt.toLocalTime();
            float y=12+(now.getHour()*60+now.getMinute()+now.getSecond()/60f)*TimelineView.HOUR/60-offset;
            if (y>=-8 && y<=height+8) {
                line(c,row,width,y,1.25f,0xffc96458);
                RemoteViews dot=new RemoteViews(c.getPackageName(),R.layout.widget_native_line);
                position(dot,R.id.native_line,TimelineView.AXIS-2.5f,y-2.5f,5,5);
                dot.setInt(R.id.native_line,"setBackgroundResource",R.drawable.time_dot);
                row.addView(R.id.slice_root,dot);
                // The live clock is in the header; keep fixed hour labels unobscured.
            }
        }
        return row;
    }
    private static void line(Context c, RemoteViews row, int width, float y, float thickness, int color) {
        RemoteViews line=new RemoteViews(c.getPackageName(),R.layout.widget_native_line);
        position(line,R.id.native_line,TimelineView.AXIS,y-thickness/2,width-TimelineView.AXIS,thickness);
        line.setInt(R.id.native_line,"setBackgroundColor",color);
        row.addView(R.id.slice_root,line);
    }
    private static void label(Context c, RemoteViews row, String text, float top, int color) {
        RemoteViews label=new RemoteViews(c.getPackageName(),R.layout.widget_native_label);
        label.setTextViewText(R.id.native_label,text);label.setTextColor(R.id.native_label,color);
        label.setViewLayoutMargin(R.id.native_label,RemoteViews.MARGIN_TOP,top,DP);
        row.addView(R.id.slice_root,label);
    }
    private static void position(RemoteViews view, int id, float x, float y, float width, float height) {
        view.setViewLayoutWidth(id,width,DP);view.setViewLayoutHeight(id,height,DP);
        view.setViewLayoutMargin(id,RemoteViews.MARGIN_LEFT,x,DP);view.setViewLayoutMargin(id,RemoteViews.MARGIN_TOP,y,DP);
    }
}
