package com.example.daycalendar;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.*;
import android.util.Log;

public class MinuteUpdateService extends Service {
    static final String CHANNEL = "minute_updates";
    static final int NOTIFICATION_ID = 41;
    static final String STOP = "com.example.daycalendar.STOP_MINUTE_UPDATES";
    private static final String PREFS = "widget_updates";
    private static volatile boolean running;
    private MinuteRefreshLoop loop;
    private boolean registered;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final BroadcastReceiver changes = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent intent) {
            String action = intent.getAction();
            if (loop != null) loop.resync(!Intent.ACTION_TIME_TICK.equals(action));
        }
    };

    static boolean enabled(Context c) { return c.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("enabled", false); }
    static void setEnabled(Context c, boolean value) { c.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("enabled", value).apply(); }
    static boolean isRunning() { return running; }
    static boolean notificationsAllowed(Context c) {
        if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false;
        NotificationManager m = c.getSystemService(NotificationManager.class);
        NotificationChannel channel = m.getNotificationChannel(CHANNEL);
        return m.areNotificationsEnabled() && (channel == null || channel.getImportance() != NotificationManager.IMPORTANCE_NONE);
    }
    static boolean startIfEnabled(Context c) {
        if (!enabled(c) || !CalendarEvents.permitted(c) || !notificationsAllowed(c)) return false;
        if (running) return true;
        try { c.startForegroundService(new Intent(c, MinuteUpdateService.class)); return true; }
        catch (IllegalStateException | SecurityException e) {
            Log.w("MinuteUpdates", "Foreground start requires opening the app", e); return false;
        }
    }
    static void disable(Context c) {
        setEnabled(c, false); running = false;
        c.stopService(new Intent(c, MinuteUpdateService.class));
        DayWidget.refreshAll(c);
    }
    @Override public void onCreate() {
        super.onCreate();
        NotificationManager manager = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL, "時刻と赤線の毎分更新", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("ホーム画面の時刻と現在時刻線を毎分更新している間に表示します");
        manager.createNotificationChannel(channel);
        PowerManager power = getSystemService(PowerManager.class);
        loop = new MinuteRefreshLoop(new MinuteRefreshLoop.Scheduler() {
            public void post(Runnable task, long delay) { handler.postDelayed(task, delay); }
            public void cancel(Runnable task) { handler.removeCallbacks(task); }
        }, System::currentTimeMillis, power::isInteractive, () -> {
            try { DayWidget.refreshAll(this); }
            catch (RuntimeException e) { Log.w("MinuteUpdates", "Widget refresh could not complete", e); }
        });
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if ((intent != null && STOP.equals(intent.getAction())) || !enabled(this)
                || !CalendarEvents.permitted(this) || !notificationsAllowed(this)) {
            setEnabled(this, false); running = false;
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); DayWidget.refreshAll(this); return START_NOT_STICKY;
        }
        PendingIntent open = PendingIntent.getActivity(this, 10, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 11, new Intent(this, MinuteUpdateService.class).setAction(STOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification).setContentTitle("時刻と赤線を毎分更新中")
                .setContentText("画面点灯中に更新します · タップして設定")
                .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).setShowWhen(false)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(new Notification.Action.Builder(null, "停止", stop).build()).build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(NOTIFICATION_ID, notification);
        running = true;
        if (!registered) {
            IntentFilter filter = new IntentFilter();
            for (String action : new String[]{Intent.ACTION_TIME_TICK, Intent.ACTION_SCREEN_ON, Intent.ACTION_SCREEN_OFF,
                    Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_DATE_CHANGED}) filter.addAction(action);
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(changes, filter, Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(changes, filter);
            registered = true;
        }
        loop.start(); return START_STICKY;
    }
    @Override public void onDestroy() {
        if (loop != null) loop.stop();
        if (registered) unregisterReceiver(changes);
        running = false; DayWidget.refreshAll(this); super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
