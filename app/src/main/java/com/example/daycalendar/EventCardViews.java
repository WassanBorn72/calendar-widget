package com.example.daycalendar;
import android.widget.RemoteViews;

final class EventCardViews {
    private EventCardViews() { }
    static void color(RemoteViews view,int fillId,int outlineId,int accentId,int color) {
        // Tint resource shapes, not bitmaps: keep the full-day transfer bitmap-free.
        view.setInt(fillId,"setColorFilter",EventColors.fill(color));
        view.setInt(outlineId,"setColorFilter",EventColors.border(color));
        view.setInt(accentId,"setBackgroundColor",EventColors.accent(color));
    }
}
