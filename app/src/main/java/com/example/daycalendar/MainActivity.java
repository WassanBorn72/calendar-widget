package com.example.daycalendar;

import android.Manifest;
import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.widget.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private LocalDate date = LocalDate.now();
    private LinearLayout body;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private int generation;
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size, int color) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setPadding(0, dp(7), 0, dp(7)); return v;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) date = LocalDate.parse(state.getString("date", date.toString()));
    }
    @Override protected void onResume() {
        super.onResume(); MinuteUpdateService.startIfEnabled(this); render(); DayWidget.refreshAll(this);
    }
    @Override protected void onSaveInstanceState(Bundle out) { out.putString("date", date.toString()); super.onSaveInstanceState(out); }
    private void render() {
        int request = ++generation;
        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(24), dp(40), dp(24), dp(24)); body.setBackgroundColor(Color.rgb(245,243,237));
        scroll.setFillViewport(true); scroll.addView(body); setContentView(scroll);
        body.addView(text("DAILY / CALENDAR", 12, 0xff53655e));
        TextView heading = text(date.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.JAPANESE)), 28, 0xff183b32);
        heading.setTypeface(null, Typeface.BOLD); body.addView(heading);
        LinearLayout nav = new LinearLayout(this);
        button(nav, "前日", () -> {date = date.minusDays(1); render();});
        button(nav, "今日", () -> {date = LocalDate.now(); render();});
        button(nav, "翌日", () -> {date = date.plusDays(1); render();}); body.addView(nav);
        TextClock clock = new TextClock(this); clock.setFormat12Hour("HH:mm"); clock.setFormat24Hour("HH:mm");
        clock.setTextSize(24); clock.setTextColor(0xff233e36); body.addView(clock);
        Switch minuteUpdates = new Switch(this);
        minuteUpdates.setText("時刻と赤線を毎分更新"); minuteUpdates.setTextSize(16);
        minuteUpdates.setChecked(MinuteUpdateService.enabled(this));
        minuteUpdates.setEnabled(CalendarEvents.permitted(this));
        minuteUpdates.setPadding(0, dp(12), 0, dp(12)); body.addView(minuteUpdates);
        body.addView(text("ON にすると常駐通知を表示します。画面消灯中は休止し、点灯時に最新時刻へ更新します。", 12, 0xff53655e));
        minuteUpdates.setOnCheckedChangeListener((view, checked) -> {
            if (!checked) { MinuteUpdateService.disable(this); return; }
            if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2); return;
            }
            if (!MinuteUpdateService.notificationsAllowed(this)) {
                MinuteUpdateService.setEnabled(this, false);
                Toast.makeText(this, "毎分更新の通知を設定で許可してください", Toast.LENGTH_LONG).show();
                startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName()));
                return;
            }
            MinuteUpdateService.setEnabled(this, true);
            if (!MinuteUpdateService.startIfEnabled(this)) {
                MinuteUpdateService.setEnabled(this, false);
                Toast.makeText(this, "毎分更新を開始できませんでした。アプリを開き直してください", Toast.LENGTH_LONG).show();
                render();
            }
        });
        if (!MinuteUpdateService.notificationsAllowed(this)) button(body, "通知設定を開く", () ->
                startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName())));
        body.addView(text("端末で同期・表示されているカレンダーの予定", 12, 0xff53655e));
        if (!CalendarEvents.permitted(this)) {
            body.addView(text("Google カレンダーを端末の設定で同期してから、読み取りを許可してください。予定は端末内で処理します。", 16, 0xff183b32));
            button(body, "カレンダーへのアクセスを許可", () -> requestPermissions(new String[]{Manifest.permission.READ_CALENDAR}, 1));
            button(body, "アプリの権限設定を開く", () -> startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:" + getPackageName()))));
            return;
        }
        TextView loading = text("読み込み中…", 16, 0xff53655e); body.addView(loading);
        LocalDate selected = date;
        worker.execute(() -> {
            try {
                var events = CalendarEvents.load(this, selected);
                runOnUiThread(() -> {
                    if (request != generation || isDestroyed()) return;
                    body.removeView(loading);
                    if (events.isEmpty()) body.addView(text("予定のない日。ひと息つきましょう。\n予定が表示されない場合は、端末の Google アカウントのカレンダー同期と表示設定を確認してください。", 17, 0xff53655e));
                    for (var event : events) {
                        if (!event.allDay()) continue;
                        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
                        card.setPadding(dp(16), dp(10), dp(16), dp(10));
                        GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.WHITE); bg.setCornerRadius(dp(16));
                        bg.setStroke(Math.max(1, Math.round(.5f*getResources().getDisplayMetrics().density)), TimelineView.eventBorderColor(event.color())); card.setBackground(bg);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(10), 0, 0); body.addView(card, lp);
                        card.addView(text(event.time(), 13, 0xff245e4c));
                        TextView title = text(event.title(), 20, 0xff183b32); title.setTypeface(null, Typeface.BOLD); card.addView(title);
                        if (!event.location().trim().isEmpty()) card.addView(text(event.location(), 13, 0xff53655e));
                        card.setOnClickListener(v -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id()));
                            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.begin());
                            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end());
                            try { startActivity(intent); } catch (ActivityNotFoundException e) { Toast.makeText(this, "カレンダーアプリがありません", Toast.LENGTH_SHORT).show(); }
                        });
                    }
                    body.addView(new TimelineView(this, selected, events, this::openEvent));
                    button(body, "更新", () -> { render(); DayWidget.refreshAll(this); });
                    button(body, "ホーム画面にウィジェットを追加", this::pinWidget);
                    body.addView(text("ホーム画面のウィジェット一覧からも追加できます。ウィジェットは常に今日の予定を表示します。", 12, 0xff53655e));
                });
            } catch (RuntimeException e) {
                runOnUiThread(() -> { if (request == generation && !isDestroyed()) loading.setText("読み込めませんでした。権限と同期設定を確認し、アプリを開き直してください。"); });
            }
        });
    }
    private void openEvent(CalendarEvent event) {
        Intent intent = new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id()));
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.begin());
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end());
        try { startActivity(intent); } catch (ActivityNotFoundException e) { Toast.makeText(this, "カレンダーアプリがありません", Toast.LENGTH_SHORT).show(); }
    }
    private void button(LinearLayout parent, String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setOnClickListener(v -> action.run()); parent.addView(b);
    }
    private void pinWidget() {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        if (manager.isRequestPinAppWidgetSupported()) manager.requestPinAppWidget(new ComponentName(this, DayWidget.class), null, null);
        else Toast.makeText(this, "ホーム画面のウィジェット一覧から追加してください", Toast.LENGTH_LONG).show();
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 2) {
            boolean granted = results.length > 0 && results[0] == android.content.pm.PackageManager.PERMISSION_GRANTED;
            MinuteUpdateService.setEnabled(this, granted);
            if (granted) MinuteUpdateService.startIfEnabled(this);
            else Toast.makeText(this, "常駐通知を表示するには通知の許可が必要です", Toast.LENGTH_LONG).show();
        }
        render(); DayWidget.refreshAll(this);
    }
    @Override protected void onDestroy() { generation++; worker.shutdown(); super.onDestroy(); }
}
