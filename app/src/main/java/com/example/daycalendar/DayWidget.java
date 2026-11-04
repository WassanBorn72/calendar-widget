package com.example.daycalendar;
import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.util.SizeF;
import java.util.*;
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
        android.os.Bundle options = m.getAppWidgetOptions(id);
        if (Build.VERSION.SDK_INT >= 31) {
            ArrayList<SizeF> sizes = options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
            if (sizes != null && !sizes.isEmpty()) {
                Map<SizeF, RemoteViews> variants = new LinkedHashMap<>();
                for (SizeF size : collectionSizes(sizes)) {
                    if (size.getWidth() > 0 && size.getHeight() > 0)
                        variants.put(size, createViews(c, id, Math.round(size.getWidth())));
                }
                if (!variants.isEmpty()) { m.updateAppWidget(id, new RemoteViews(variants)); return; }
            }
        }
        boolean landscape = c.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        int width = options.getInt(landscape ? AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH : AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 272);
        m.updateAppWidget(id, createViews(c, id, width));
    }
    // The platform divides its collection parcel budget among responsive sizes.
    // Four representative widths retain small/large layouts without starving the last rows.
    static List<SizeF> collectionSizes(List<SizeF> sizes) {
        TreeMap<Integer,SizeF> byWidth=new TreeMap<>();
        for(SizeF size:sizes) if(size.getWidth()>0 && size.getHeight()>0) {
            int width=Math.round(size.getWidth());
            SizeF previous=byWidth.get(width);
            if(previous==null || size.getHeight()<previous.getHeight())byWidth.put(width,size);
        }
        List<SizeF> unique=new ArrayList<>(byWidth.values());
        if(unique.size()<=4)return unique;
        List<SizeF> selected=new ArrayList<>();
        for(int i=0;i<4;i++)selected.add(unique.get(i*(unique.size()-1)/3));
        return selected;
    }
    private static RemoteViews createViews(Context c, int id, int outerWidthDp) {
        int widthDp = Math.max(128, Math.min(2048, outerWidthDp-32));
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
        v.setTextViewText(R.id.date, LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日 EEE", Locale.JAPANESE)));
        Intent service = new Intent(c, EventWidgetService.class);
        service.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        service.putExtra(WidgetRaster.WIDTH_DP, widthDp);
        service.setData(Uri.parse("daycalendar://widget/" + id + "/" + widthDp + (Build.VERSION.SDK_INT>=31?"/native-v2":"/raster")));
        v.setRemoteAdapter(R.id.events, service);
        String updated = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        v.setTextViewText(R.id.all_day, "赤線 " + updated + " 時点 · " + (MinuteUpdateService.isRunning() ? "毎分更新中" : "アプリで毎分更新をON"));
        v.setEmptyView(R.id.events, R.id.empty);
        v.setTextViewText(R.id.empty, CalendarEvents.permitted(c) ? "今日の予定はありません" : "タップしてカレンダーの読み取りを許可");
        PendingIntent open = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.date, open);
        v.setOnClickPendingIntent(R.id.clock, open);
        v.setOnClickPendingIntent(R.id.all_day, open);
        v.setOnClickPendingIntent(R.id.empty, open);
        Intent refresh = new Intent(c, DayWidget.class).setAction("com.example.daycalendar.REFRESH");
        v.setOnClickPendingIntent(R.id.refresh, PendingIntent.getBroadcast(c, 1, refresh, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        return v;
    }
}
