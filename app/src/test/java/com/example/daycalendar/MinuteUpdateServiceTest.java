package com.example.daycalendar;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.Manifest;
import android.app.Notification;
import android.content.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ServiceController;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class MinuteUpdateServiceTest {
    private Context context;
    private ServiceController<MinuteUpdateService> controller;
    @Before public void prepare() {
        context=RuntimeEnvironment.getApplication();
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.READ_CALENDAR,Manifest.permission.POST_NOTIFICATIONS);
        MinuteUpdateService.setEnabled(context,true);
    }
    @After public void cleanUp() {
        if(controller!=null)controller.destroy();
        MinuteUpdateService.setEnabled(context,false);
    }
    @Test public void optInStartsForegroundNotificationWithStopAction() {
        controller=Robolectric.buildService(MinuteUpdateService.class).create();
        MinuteUpdateService service=controller.get();
        service.onStartCommand(new Intent(context,MinuteUpdateService.class),0,1);
        assertTrue(MinuteUpdateService.isRunning());
        Notification notification=shadowOf(service).getLastForegroundNotification();
        assertNotNull(notification);assertTrue((notification.flags & Notification.FLAG_ONGOING_EVENT)!=0);
        assertEquals("停止",notification.actions[0].title);
        assertEquals(MinuteUpdateService.CHANNEL,notification.getChannelId());
        service.onStartCommand(new Intent(context,MinuteUpdateService.class).setAction(MinuteUpdateService.STOP),0,2);
        assertFalse(MinuteUpdateService.isRunning());assertFalse(MinuteUpdateService.enabled(context));
        assertTrue(shadowOf(service).isStoppedBySelf());
    }
    @Test public void disabledSettingDoesNotStartForegroundWork() {
        MinuteUpdateService.setEnabled(context,false);
        controller=Robolectric.buildService(MinuteUpdateService.class).create();
        MinuteUpdateService service=controller.get();service.onStartCommand(null,0,1);
        assertFalse(MinuteUpdateService.isRunning());assertNull(shadowOf(service).getLastForegroundNotification());
        assertTrue(shadowOf(service).isStoppedBySelf());
    }
    @Test public void notificationDenialPreventsBackgroundStart() {
        shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.POST_NOTIFICATIONS);
        assertFalse(MinuteUpdateService.startIfEnabled(context));
    }
}
