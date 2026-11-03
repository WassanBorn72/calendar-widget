package com.example.daycalendar;

import static org.junit.Assert.*;
import org.junit.Test;

public class MinuteRefreshLoopTest {
    static class Scheduler implements MinuteRefreshLoop.Scheduler {
        Runnable task; long delay;
        public void post(Runnable task, long delay) { this.task=task;this.delay=delay; }
        public void cancel(Runnable task) { if(this.task==task)this.task=null; }
        void fire() { Runnable next=task;task=null;assertNotNull(next);next.run(); }
    }
    @Test public void alignsToMinuteAndDoesNotDoubleRefreshForTimeTick() {
        Scheduler scheduler=new Scheduler();long[] now={61234};int[] refreshes={0};
        MinuteRefreshLoop loop=new MinuteRefreshLoop(scheduler,()->now[0],()->true,()->refreshes[0]++);
        loop.start();assertEquals(1,refreshes[0]);assertEquals(58766,scheduler.delay);
        now[0]=120000;scheduler.fire();assertEquals(2,refreshes[0]);assertEquals(60000,scheduler.delay);
        loop.resync(false);assertEquals(2,refreshes[0]);
        loop.stop();assertNull(scheduler.task);
    }
    @Test public void sleepsWithoutWakeupsAndCatchesUpWhenScreenTurnsOn() {
        Scheduler scheduler=new Scheduler();long[] now={60000};boolean[] screen={true};int[] refreshes={0};
        MinuteRefreshLoop loop=new MinuteRefreshLoop(scheduler,()->now[0],()->screen[0],()->refreshes[0]++);
        loop.start();screen[0]=false;loop.resync(true);assertNull(scheduler.task);
        now[0]=309000;screen[0]=true;loop.resync(true);
        assertEquals(2,refreshes[0]);assertEquals(51000,scheduler.delay);
    }
    @Test public void clockChangesRepositionImmediatelyAndStopCancelsPendingWork() {
        Scheduler scheduler=new Scheduler();long[] now={120000};int[] refreshes={0};
        MinuteRefreshLoop loop=new MinuteRefreshLoop(scheduler,()->now[0],()->true,()->refreshes[0]++);
        loop.start();now[0]=30000;loop.resync(true);
        assertEquals(2,refreshes[0]);assertEquals(30000,scheduler.delay);
        Runnable stale=scheduler.task;loop.stop();stale.run();assertEquals(2,refreshes[0]);assertNull(scheduler.task);
    }
    @Test public void refreshCrossingMinuteBoundaryCatchesUpWithoutLosingAMinute() {
        Scheduler scheduler=new Scheduler();long[] now={119900};int[] refreshes={0};
        MinuteRefreshLoop loop=new MinuteRefreshLoop(scheduler,()->now[0],()->true,()->{refreshes[0]++;now[0]+=2000;});
        loop.start();assertEquals(0,scheduler.delay);scheduler.fire();
        assertEquals(2,refreshes[0]);assertEquals(56100,scheduler.delay);
    }
}
