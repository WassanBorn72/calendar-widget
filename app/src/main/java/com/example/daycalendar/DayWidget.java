package com.example.daycalendar;
import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.widget.RemoteViews;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DayWidget extends AppWidgetProvider {
    static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, DayWidget.class));
        for (int id : ids) update(c, m, id);
        m.notifyAppWidgetViewDataChanged(ids, R.id.events);
    }
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) update(c, m, id);
        m.notifyAppWidgetViewDataChanged(ids, R.id.events);
    }
    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, android.os.Bundle options) {
        update(c, m, id);
        m.notifyAppWidgetViewDataChanged(new int[]{id}, R.id.events);
    }
    @Override public void onReceive(Context c, Intent intent) {
        super.onReceive(c, intent);
        String a = intent.getAction();
        if ("com.example.daycalendar.REFRESH".equals(a) || Intent.ACTION_DATE_CHANGED.equals(a)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(a) || Intent.ACTION_TIME_CHANGED.equals(a)) refreshAll(c);
    }
    private static void update(Context c, AppWidgetManager m, int id) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
        v.setTextViewText(R.id.date, LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日 EEE", Locale.JAPANESE)));
        Intent service = new Intent(c, EventWidgetService.class);
        service.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        service.setData(Uri.parse("daycalendar://widget/" + id));
        v.setRemoteAdapter(R.id.events, service);
        v.setTextViewText(R.id.all_day, "終日予定は一覧の先頭に表示 · 赤線は更新時刻");
        v.setEmptyView(R.id.events, R.id.empty);
        v.setTextViewText(R.id.empty, CalendarEvents.permitted(c) ? "今日の予定はありません" : "タップしてカレンダーの読み取りを許可");
        PendingIntent open = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.date, open);
        v.setOnClickPendingIntent(R.id.empty, open);
        Intent refresh = new Intent(c, DayWidget.class).setAction("com.example.daycalendar.REFRESH");
        v.setOnClickPendingIntent(R.id.refresh, PendingIntent.getBroadcast(c, 1, refresh, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        m.updateAppWidget(id, v);
    }
}
