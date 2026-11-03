package com.example.daycalendar;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;

/** One full day, or one hourly slice for a RemoteViews bitmap. Coordinates are dp. */
final class TimelineView extends View {
    static final int HOUR = 72;
    private final LocalDate day;
    private final List<DayTimeline.Block> blocks;
    private final Consumer<CalendarEvent> onEvent;
    private final float density;
    private final Runnable tick = new Runnable() {
        public void run() { invalidate(); postDelayed(this, 60000 - System.currentTimeMillis()%60000); }
    };
    TimelineView(Context c, LocalDate day, List<CalendarEvent> events, Consumer<CalendarEvent> onEvent) {
        super(c); this.day = day; this.onEvent = onEvent;
        blocks = DayTimeline.layout(events, day, ZoneId.systemDefault());
        density = getResources().getDisplayMetrics().density;
        StringBuilder description = new StringBuilder("0時から24時。1時間あたり72dpのタイムライン。");
        for (var b : blocks) description.append(b.event.time()).append(" ").append(b.event.title()).append("。 ");
        setContentDescription(description);
    }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); post(tick); }
    @Override protected void onDetachedFromWindow() { removeCallbacks(tick); super.onDetachedFromWindow(); }
    @Override protected void onMeasure(int w, int h) { setMeasuredDimension(MeasureSpec.getSize(w), (int)((24*HOUR+24)*density)); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); canvas.save(); canvas.scale(density, density);
        drawTimeline(canvas, getWidth()/density, day, blocks, 0, 24*HOUR+24);
        canvas.restore();
    }
    static Bitmap hourBitmap(int width, int hour, LocalDate day, List<CalendarEvent> events) {
        int height = HOUR + (hour == 23 ? 24 : 0);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap.eraseColor(0xfffafbf7);
        Canvas c = new Canvas(bitmap); c.translate(0, -hour*HOUR);
        drawTimeline(c, width, day, DayTimeline.layout(events, day, ZoneId.systemDefault()), hour*HOUR, (hour+1)*HOUR+24);
        return bitmap;
    }
    private static void drawTimeline(Canvas c, float width, LocalDate day, List<DayTimeline.Block> blocks, float from, float to) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        for (int h=0;h<=24;h++) {
            float y=12+h*HOUR;
            if (y < from-12 || y > to) continue;
            p.setColor(0xffdce3d7);p.setStrokeWidth(1);c.drawLine(44,y,width,y,p);
            p.setColor(0xff788575);p.setTextSize(10);c.drawText(String.format(Locale.ROOT,"%02d:00",h),0,y+4,p);
            if(h<24){p.setColor(0xffe9ece4);c.drawLine(44,y+HOUR/2f,width,y+HOUR/2f,p);}
        }
        for (var b : blocks) {
            float top = (float)(12+b.start*HOUR/60), bottom=(float)(12+b.end*HOUR/60);
            if(bottom < from || top > to) continue;
            float laneWidth=(width-48)/b.columns,left=46+laneWidth*b.lane,right=left+laneWidth-3;
            int color=b.event.color()==0?0xff36674e:b.event.color();
            p.setColor(Color.rgb((Color.red(color)+765)/4,(Color.green(color)+765)/4,(Color.blue(color)+765)/4));
            c.drawRect(left,top,right,Math.max(top+1,bottom),p);
            p.setColor(color);c.drawRect(left,top,left+3,Math.max(top+1,bottom),p);
            c.save();c.clipRect(left+5,top,right-3,bottom);
            p.setColor(0xff233e36);p.setTextSize(10);
            c.drawText(b.event.time(),left+6,top+12,p);
            p.setTextSize(12);p.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));
            c.drawText(b.event.title(),left+6,top+27,p);
            p.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.NORMAL));p.setTextSize(10);
            c.drawText(b.event.location(),left+6,top+42,p);c.restore();
        }
        if(day.equals(LocalDate.now())) {
            LocalTime now=LocalTime.now();float y=12+(now.getHour()*60+now.getMinute()+now.getSecond()/60f)*HOUR/60;
            p.setColor(0xffce5e50);p.setStrokeWidth(2);c.drawLine(44,y,width,y,p);c.drawCircle(44,y,3,p);
            p.setColor(0xfffafbf7);c.drawRect(0,y-8,41,y+8,p);
            p.setColor(0xffce5e50);p.setTextSize(10);c.drawText(String.format(Locale.ROOT,"%02d:%02d",now.getHour(),now.getMinute()),0,y+4,p);
        }
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_DOWN)return true;
        if(event.getAction()==MotionEvent.ACTION_UP){
            float x=event.getX()/density,y=event.getY()/density;
            for(var b:blocks){
                float laneWidth=(getWidth()/density-48)/b.columns,left=46+laneWidth*b.lane;
                if(x>=left && x<=left+laneWidth-3 && y>=12+b.start*HOUR/60 && y<=12+b.end*HOUR/60){
                    performClick();onEvent.accept(b.event);return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
