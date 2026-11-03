package com.example.daycalendar;
import java.time.*;
import java.time.format.DateTimeFormatter;
record CalendarEvent(long id, String title, String location, long begin, long end, boolean allDay, int color) {
        String time() {
            if (allDay) return "終日";
            DateTimeFormatter f = DateTimeFormatter.ofPattern("HH:mm");
            ZoneId zone = ZoneId.systemDefault();
            return f.format(Instant.ofEpochMilli(begin).atZone(zone)) + " – "
                    + f.format(Instant.ofEpochMilli(end).atZone(zone));
        }
    }
