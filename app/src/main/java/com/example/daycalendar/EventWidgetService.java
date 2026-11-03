package com.example.daycalendar;
import android.content.Intent;
import android.widget.*;
import java.time.LocalDate;
import java.util.*;

public class EventWidgetService extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(Intent intent) { return new Factory(intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, 0)); }
    private class Factory implements RemoteViewsFactory {
        private List<CalendarEvent> events = Collections.emptyList();
        private String error;
        private LocalDate day = LocalDate.now();
        private final int widgetId;
        private List<CalendarEvent> allDay = Collections.emptyList();
        Factory(int id) { widgetId = id; }
        public void onCreate() { }
        public void onDataSetChanged() {
            try { day = LocalDate.now(); events = CalendarEvents.load(EventWidgetService.this, day);
                allDay = new ArrayList<>(); for (var e : events) if(e.allDay()) allDay.add(e);
                error = null; }
            catch (RuntimeException e) { events = Collections.emptyList(); error = "アプリを開いて権限・同期設定を確認してください"; }
        }
        public void onDestroy() { events = Collections.emptyList(); }
        public int getCount() { return error == null ? 24 + allDay.size() : 1; }
        public RemoteViews getViewAt(int position) {
            RemoteViews v = new RemoteViews(getPackageName(), R.layout.widget_event);
            if (error != null) {
                v.setTextViewText(R.id.time, "読み込みできません"); v.setTextViewText(R.id.title, error); v.setTextViewText(R.id.location, "");
            } else if (position >= allDay.size()) {
                int hour = position-allDay.size();
                if(hour < 0 || hour > 23) return null;
                var manager = android.appwidget.AppWidgetManager.getInstance(EventWidgetService.this);
                int width = manager.getAppWidgetOptions(widgetId).getInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 240)-32;
                width = Math.max(140, Math.min(600, width));
                RemoteViews row = new RemoteViews(getPackageName(), R.layout.widget_hour);
                int imageId = hour == 23 ? R.id.last_hour_image : R.id.hour_image;
                row.setViewVisibility(R.id.hour_image, hour == 23 ? android.view.View.GONE : android.view.View.VISIBLE);
                row.setViewVisibility(R.id.last_hour_image, hour == 23 ? android.view.View.VISIBLE : android.view.View.GONE);
                row.setImageViewBitmap(imageId, TimelineView.hourBitmap(width, hour, day, events));
                row.setContentDescription(imageId, hour + "時から" + (hour+1) + "時の予定");
                return row;
            } else {
                if (position < 0) return null;
                var e = allDay.get(position);
                v.setTextViewText(R.id.time, e.time()); v.setTextViewText(R.id.title, e.title()); v.setTextViewText(R.id.location, e.location());
            }
            return v;
        }
        public RemoteViews getLoadingView() { return null; }
        public int getViewTypeCount() { return 2; }
        public long getItemId(int position) { return position; }
        public boolean hasStableIds() { return false; }
    }
}
