package com.example.daycalendar;
import android.content.Intent;
import android.widget.*;
import java.time.LocalDate;
import java.util.*;

public class EventWidgetService extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(Intent intent) { return new Factory(); }
    private class Factory implements RemoteViewsFactory {
        private List<CalendarEvents.Event> events = Collections.emptyList();
        private String error;
        public void onCreate() { }
        public void onDataSetChanged() {
            try { events = CalendarEvents.load(EventWidgetService.this, LocalDate.now()); error = null; }
            catch (RuntimeException e) { events = Collections.emptyList(); error = "アプリを開いて権限・同期設定を確認してください"; }
        }
        public void onDestroy() { events = Collections.emptyList(); }
        public int getCount() { return error == null ? events.size() : 1; }
        public RemoteViews getViewAt(int position) {
            RemoteViews v = new RemoteViews(getPackageName(), R.layout.widget_event);
            if (error != null) {
                v.setTextViewText(R.id.time, "読み込みできません"); v.setTextViewText(R.id.title, error); v.setTextViewText(R.id.location, "");
            } else {
                if (position < 0 || position >= events.size()) return null;
                var e = events.get(position);
                v.setTextViewText(R.id.time, e.time()); v.setTextViewText(R.id.title, e.title()); v.setTextViewText(R.id.location, e.location());
            }
            return v;
        }
        public RemoteViews getLoadingView() { return null; }
        public int getViewTypeCount() { return 1; }
        public long getItemId(int position) { return position; }
        public boolean hasStableIds() { return false; }
    }
}
