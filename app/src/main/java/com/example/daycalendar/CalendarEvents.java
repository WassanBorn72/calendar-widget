package com.example.daycalendar;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

final class CalendarEvents {
    static boolean permitted(Context c) {
        return c.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }
    static List<CalendarEvent> load(Context c, LocalDate date) {
        if (!permitted(c)) throw new SecurityException("カレンダーへのアクセスを許可してください");
        ZoneId zone = ZoneId.systemDefault();
        long start = date.atStartOfDay(zone).toInstant().toEpochMilli();
        long end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        // All-day instances use UTC boundaries. Query a wider interval, then filter
        // all-day events by UTC date and timed events by local-day intersection.
        Uri.Builder uri = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(uri, start - 86400000L);
        ContentUris.appendId(uri, end + 86400000L);
        String[] projection = {"event_id", "title", "eventLocation", "begin", "end", "allDay", "calendar_color"};
        List<CalendarEvent> result = new ArrayList<>();
        try (Cursor rows = c.getContentResolver().query(uri.build(), projection,
                "visible = ? AND deleted = ?", new String[]{"1", "0"}, "allDay DESC, begin ASC")) {
            if (rows == null) throw new IllegalStateException("カレンダーを読み込めませんでした");
            while (rows.moveToNext()) {
                long b = rows.getLong(3), e = rows.getLong(4);
                boolean all = rows.getInt(5) != 0;
                boolean included;
                if (all) {
                    LocalDate first = Instant.ofEpochMilli(b).atZone(ZoneOffset.UTC).toLocalDate();
                    LocalDate last = Instant.ofEpochMilli(e).atZone(ZoneOffset.UTC).toLocalDate();
                    included = !date.isBefore(first) && date.isBefore(last);
                } else included = b < end && (e > start || (b == e && b >= start));
                if (included) result.add(new CalendarEvent(rows.getLong(0),
                        rows.isNull(1) || rows.getString(1).trim().isEmpty() ? "（タイトルなし）" : rows.getString(1),
                        rows.isNull(2) ? "" : rows.getString(2), b, e, all, rows.getInt(6)));
            }
        }
        return result;
    }
}
