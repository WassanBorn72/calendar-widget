package com.example.daycalendar;

import android.content.Context;
import android.graphics.*;
import android.text.*;
import android.view.*;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;

/** One full day, or one small RemoteViews raster slice. Coordinates are dp. */
// Instantiated only in code with its required day and events, never inflated from XML.
@android.annotation.SuppressLint("ViewConstructor")
final class TimelineView extends View {
    static final int HOUR = 72;
    static final int AXIS = 50;
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
        drawTimeline(canvas, getWidth()/density, day, blocks, 0, 24*HOUR+24, ZonedDateTime.now());
        canvas.restore();
    }
    static Bitmap sliceBitmap(int widthDp, float density, int slice, LocalDate day,
                              List<DayTimeline.Block> blocks, ZonedDateTime updatedAt) {
        int heightDp = WidgetRaster.sliceHeight(slice);
        WidgetRaster.Size size = WidgetRaster.size(widthDp, heightDp, density);
        Bitmap bitmap = Bitmap.createBitmap(size.width(), size.height(), Bitmap.Config.ARGB_8888);
        bitmap.setDensity(Bitmap.DENSITY_NONE);
        bitmap.eraseColor(0xfffafbf7);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(size.width() / (float)widthDp, size.height() / (float)heightDp);
        int offset = slice * WidgetRaster.SLICE_HEIGHT;
        canvas.translate(0, -offset);
        drawTimeline(canvas, widthDp, day, blocks, offset, offset + heightDp, updatedAt);
        return bitmap;
    }
    static int eventBorderColor(int color) {
        return EventColors.border(color);
    }
    private static void drawTimeline(Canvas c, float width, LocalDate day, List<DayTimeline.Block> blocks,
                                     float from, float to, ZonedDateTime updatedAt) {
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.DITHER_FLAG);
        p.setHinting(Paint.HINTING_ON);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        for (int h=0;h<=24;h++) {
            float y=12+h*HOUR;
            p.setColor(0xffbcc9bb);p.setStrokeWidth(1);c.drawLine(AXIS,y,width,y,p);
            p.setColor(0xff53655e);p.setTextSize(11);c.drawText(String.format(Locale.ROOT,"%02d:00",h),0,y+4,p);
            if(h<24){p.setColor(0xffdfe5da);c.drawLine(AXIS,y+HOUR/2f,width,y+HOUR/2f,p);}
        }
        for (var b : blocks) {
            float top = (float)(12+b.start*HOUR/60), bottom=(float)(12+b.end*HOUR/60);
            if(bottom < from || top > to) continue;
            float laneWidth=(width-AXIS-4)/b.columns,left=AXIS+2+laneWidth*b.lane,right=left+laneWidth-3;
            int color=EventColors.accent(b.event.color());
            float visibleBottom = Math.max(top+1, bottom);
            RectF bounds = new RectF(left, top, right, visibleBottom);
            p.setColor(EventColors.fill(color));
            c.drawRoundRect(bounds, 4, 4, p);
            c.save();c.clipRect(left, top, left+2, visibleBottom);
            p.setColor(EventColors.accent(color));c.drawRoundRect(bounds, 4, 4, p);c.restore();
            // Stroke stays inside the exact time bounds: adjacent same-color events
            // retain distinct top/bottom edges without changing their duration.
            float stroke = Math.min(.5f, visibleBottom-top);
            RectF border = new RectF(bounds);border.inset(stroke/2, stroke/2);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(stroke);
            p.setColor(eventBorderColor(color));c.drawRoundRect(border, 3.5f, 3.5f, p);
            p.setStyle(Paint.Style.FILL);
            float textLeft = left+8, textWidth = right-textLeft-6, height = bottom-top;
            if (textWidth <= 0 || height < 10) continue;
            c.save();c.clipRect(left+4,top+2,right-3,bottom-2);
            p.setColor(0xff243f35);p.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            p.setTextSize(Math.min(14, height-6));
            if (height < 34) {
                // A short event gets one readable title, rather than two clipped lines.
                Paint.FontMetrics metrics = p.getFontMetrics();
                String title = TextUtils.ellipsize(b.event.title(), p, textWidth, TextUtils.TruncateAt.END).toString();
                c.drawText(title, textLeft, top+(height-metrics.ascent-metrics.descent)/2, p);
            } else {
                p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));p.setTextSize(11);p.setColor(EventColors.SECONDARY_TEXT);
                c.drawText(TextUtils.ellipsize(b.event.time(), p, textWidth, TextUtils.TruncateAt.END).toString(), textLeft, top+11, p);
                p.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));p.setTextSize(14);p.setColor(0xff243f35);
                StaticLayout title = StaticLayout.Builder.obtain(b.event.title(),0,b.event.title().length(),p,(int)textWidth)
                        .setIncludePad(false).setMaxLines(height >= 58 ? 2 : 1).setEllipsize(TextUtils.TruncateAt.END)
                        .build();
                c.save();c.translate(textLeft,top+15);title.draw(c);c.restore();
                float locationY = top+15+title.getHeight()+14;
                if (!b.event.location().isEmpty() && locationY <= bottom-4) {
                    p.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.NORMAL));p.setTextSize(11);p.setColor(EventColors.SECONDARY_TEXT);
                    c.drawText(TextUtils.ellipsize(b.event.location(),p,textWidth,TextUtils.TruncateAt.END).toString(),textLeft,locationY,p);
                }
            }
            c.restore();
        }
        if(day.equals(updatedAt.toLocalDate())) {
            LocalTime now=updatedAt.toLocalTime();float y=12+(now.getHour()*60+now.getMinute()+now.getSecond()/60f)*HOUR/60;
            p.setColor(0xffc96458);p.setStrokeWidth(1.25f);c.drawLine(AXIS,y,width,y,p);c.drawCircle(AXIS,y,2.5f,p);
            // Current time is shown by the header clock, leaving the hour axis clear.
        }
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_DOWN)return true;
        if(event.getAction()==MotionEvent.ACTION_UP){
            float x=event.getX()/density,y=event.getY()/density;
            for(var b:blocks){
                float laneWidth=(getWidth()/density-AXIS-4)/b.columns,left=AXIS+2+laneWidth*b.lane;
                if(x>=left && x<=left+laneWidth-3 && y>=12+b.start*HOUR/60 && y<=12+b.end*HOUR/60){
                    performClick();onEvent.accept(b.event);return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
