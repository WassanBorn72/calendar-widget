package com.example.daycalendar;

import java.time.*;
import java.util.*;

/** Wall-clock minutes keep all 24 hour rows the same height, even across DST. */
final class DayTimeline {
    static final class Block {
        final CalendarEvent event;
        final double start, end;
        int lane, columns;
        Block(CalendarEvent event, double start, double end) {
            this.event = event; this.start = start; this.end = end;
        }
    }
    static List<Block> layout(List<CalendarEvent> events, LocalDate day, ZoneId zone) {
        List<Block> blocks = new ArrayList<>();
        for (var event : events) {
            if (event.allDay()) continue;
            double start = minute(event.begin(), day, zone), end = minute(event.end(), day, zone);
            // A repeated DST hour can have an earlier wall-clock end. Keep its
            // absolute duration visible rather than drawing a negative rectangle.
            if (end < start) end = Math.min(1440, start + (event.end()-event.begin())/60000.0);
            if (end >= start && start < 1440 && end > 0) blocks.add(new Block(event, start, end));
        }
        blocks.sort(Comparator.comparingDouble((Block b) -> b.start).thenComparingDouble(b -> -b.end));
        List<Block> group = new ArrayList<>(); double groupEnd = -1;
        for (Block block : blocks) {
            if (block.start >= groupEnd) { assignLanes(group); group.clear(); groupEnd = -1; }
            group.add(block); groupEnd = Math.max(groupEnd, block.end);
        }
        assignLanes(group); return blocks;
    }
    private static double minute(long millis, LocalDate day, ZoneId zone) {
        var local = Instant.ofEpochMilli(millis).atZone(zone);
        if (local.toLocalDate().isBefore(day)) return 0;
        if (local.toLocalDate().isAfter(day)) return 1440;
        return local.getHour()*60 + local.getMinute() + local.getSecond()/60.0;
    }
    private static void assignLanes(List<Block> group) {
        List<Double> ends = new ArrayList<>();
        for (Block b : group) {
            int lane = 0; while (lane < ends.size() && ends.get(lane) > b.start) lane++;
            if (lane == ends.size()) ends.add(b.end); else ends.set(lane, b.end);
            b.lane = lane;
        }
        for (Block b : group) b.columns = ends.size();
    }
}
