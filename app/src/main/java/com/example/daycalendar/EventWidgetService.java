package com.example.daycalendar;
import android.content.Intent;
import android.os.Build;
import android.widget.*;
import java.time.*;
import java.util.*;

public class EventWidgetService extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(Intent intent) { return new Factory(intent.getIntExtra(WidgetRaster.WIDTH_DP, 240)); }
    List<CalendarEvent> loadEvents(LocalDate day) { return CalendarEvents.load(this, day); }
    private class Factory implements RemoteViewsFactory {
        private List<CalendarEvent> events = Collections.emptyList();
        private String error;
        private LocalDate day = LocalDate.now();
        private final int widthDp;
        private List<DayTimeline.Block> blocks = Collections.emptyList();
        private ZonedDateTime updatedAt = ZonedDateTime.now();
        private List<CalendarEvent> allDay = Collections.emptyList();
        Factory(int width) { widthDp = Math.max(128, Math.min(2048, width)); }
        public void onCreate() { }
        public void onDataSetChanged() {
            try { updatedAt = ZonedDateTime.now(); day = updatedAt.toLocalDate(); events = loadEvents(day);
                blocks = DayTimeline.layout(events, day, updatedAt.getZone());
                allDay = new ArrayList<>(); for (var e : events) if(e.allDay()) allDay.add(e);
                error = null; }
            catch (RuntimeException e) { events = Collections.emptyList(); error = "アプリを開いて権限・同期設定を確認してください"; }
        }
        public void onDestroy() { events = Collections.emptyList(); }
        public int getCount() { return error == null ? WidgetRaster.SLICE_COUNT + allDay.size() : 1; }
        public RemoteViews getViewAt(int position) {
            RemoteViews v = new RemoteViews(getPackageName(), R.layout.widget_event);
            if (error != null) {
                v.setTextViewText(R.id.time, "読み込みできません"); v.setTextViewText(R.id.title, error); v.setTextViewText(R.id.location, "");
            } else if (position >= allDay.size()) {
                int slice = position-allDay.size();
                if(slice < 0 || slice >= WidgetRaster.SLICE_COUNT) return null;
                RemoteViews row;
                int descriptionId;
                if (Build.VERSION.SDK_INT >= 31) {
                    row=WidgetNativeTimeline.slice(EventWidgetService.this,widthDp,slice,day,blocks,updatedAt);
                    descriptionId=R.id.slice_root;
                } else {
                    row=new RemoteViews(getPackageName(), R.layout.widget_hour);
                    boolean last=slice==WidgetRaster.SLICE_COUNT-1;
                    descriptionId=last?R.id.last_hour_image:R.id.hour_image;
                    row.setViewVisibility(R.id.hour_image,last?android.view.View.GONE:android.view.View.VISIBLE);
                    row.setViewVisibility(R.id.last_hour_image,last?android.view.View.VISIBLE:android.view.View.GONE);
                    row.setImageViewBitmap(descriptionId,TimelineView.sliceBitmap(widthDp,
                            getResources().getDisplayMetrics().density,slice,day,blocks,updatedAt));
                }
                int start = Math.max(0, (slice*WidgetRaster.SLICE_HEIGHT-12)*60/TimelineView.HOUR);
                int end = Math.min(1440, (slice*WidgetRaster.SLICE_HEIGHT+WidgetRaster.sliceHeight(slice)-12)*60/TimelineView.HOUR);
                StringBuilder description = new StringBuilder(String.format(Locale.JAPAN,
                        "%02d:%02d〜%02d:%02d", start/60, start%60, end/60, end%60));
                for(var block : blocks) if(block.start < end && block.end > start)
                    description.append(" ").append(block.event.time()).append(" ").append(block.event.title());
                row.setContentDescription(descriptionId, description);
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
        public long getItemId(int position) {
            if (error != null) return Long.MIN_VALUE;
            return position < allDay.size() ? allDay.get(position).id() : -1L-(position-allDay.size());
        }
        public boolean hasStableIds() { return true; }
    }
}
