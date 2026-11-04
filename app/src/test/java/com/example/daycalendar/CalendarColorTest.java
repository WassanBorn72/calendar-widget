package com.example.daycalendar;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.Manifest;
import android.content.*;
import android.content.pm.ProviderInfo;
import android.database.*;
import android.net.Uri;
import android.provider.CalendarContract;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;
import java.time.*;
import java.util.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,35})
public class CalendarColorTest {
    public static class Provider extends ContentProvider {
        Integer eventColor,displayColor,calendarColor;
        LocalDate day;
        public boolean onCreate(){return true;}
        public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){
            MatrixCursor cursor=new MatrixCursor(projection);Object[] row=new Object[projection.length];
            for(int i=0;i<projection.length;i++)row[i]=switch(projection[i]) {
                case "event_id" -> 1L;
                case "title" -> "Color sample";
                case "eventLocation" -> "";
                case "begin" -> day.atTime(10,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                case "end" -> day.atTime(11,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                case "allDay" -> 0;
                case "calendar_color" -> calendarColor;
                case "eventColor" -> eventColor;
                case "displayColor" -> displayColor;
                default -> throw new AssertionError("Unexpected projection "+projection[i]);
            };
            cursor.addRow(row);return cursor;
        }
        public String getType(Uri u){return null;}
        public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
        public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
        public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
    }
    private Context context;private Provider provider;
    @Before public void setUp(){
        context=RuntimeEnvironment.getApplication();
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.READ_CALENDAR);
        provider=new Provider();provider.day=LocalDate.now();
        ProviderInfo info=new ProviderInfo();info.authority=CalendarContract.AUTHORITY;
        provider.attachInfo(context,info);ShadowContentResolver.registerProviderInternal(CalendarContract.AUTHORITY,provider);
    }
    private int load(Integer event,Integer display,Integer calendar){
        provider.eventColor=event;provider.displayColor=display;provider.calendarColor=calendar;
        var events=CalendarEvents.load(context,provider.day);assertEquals(1,events.size());return events.get(0).color();
    }
    @Test public void explicitEventColorWinsOverCalendarColor(){assertEquals(0xffd50000,load(0xffd50000,0xff4285f4,0xff4285f4));}
    @Test public void displayColorAndCalendarColorAreFallbacks(){
        assertEquals(0xff8e24aa,load(null,0xff8e24aa,0xff4285f4));
        assertEquals(0xff4285f4,load(null,null,0xff4285f4));
    }
    @Test public void missingAndBlackColorsAreHandledWithoutLosingBlack(){
        assertEquals(EventColors.DEFAULT,load(null,null,null));
        assertEquals(0xff000000,load(0,null,0xff4285f4));
        assertEquals(0xffff0000,load(0x80ff0000,null,null));
    }
}
