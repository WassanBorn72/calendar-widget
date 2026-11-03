package com.example.daycalendar;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/** A screen-aware, wall-clock aligned loop. Does not wake a sleeping device. */
final class MinuteRefreshLoop {
    interface Scheduler {
        void post(Runnable task, long delayMillis);
        void cancel(Runnable task);
    }
    private final Scheduler scheduler;
    private final LongSupplier clock;
    private final BooleanSupplier interactive;
    private final Runnable refresh;
    private final Runnable timer = () -> resync(false);
    private boolean active;
    private long lastMinute = Long.MIN_VALUE;

    MinuteRefreshLoop(Scheduler scheduler, LongSupplier clock, BooleanSupplier interactive, Runnable refresh) {
        this.scheduler = scheduler; this.clock = clock; this.interactive = interactive; this.refresh = refresh;
    }
    static long delayUntilNextMinute(long millis) { return 60000 - Math.floorMod(millis, 60000); }
    void start() { active = true; resync(true); }
    void stop() { active = false; scheduler.cancel(timer); }
    void resync(boolean force) {
        scheduler.cancel(timer);
        if (!active || !interactive.getAsBoolean()) return;
        long minute = Math.floorDiv(clock.getAsLong(), 60000);
        if (force || minute != lastMinute) { refresh.run(); lastMinute = minute; }
        long now = clock.getAsLong();
        scheduler.post(timer, Math.floorDiv(now, 60000) == lastMinute ? delayUntilNextMinute(now) : 0);
    }
}
