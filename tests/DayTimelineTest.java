package com.example.daycalendar;
import java.time.*;
import java.util.*;

public class DayTimelineTest {
    static final LocalDate DAY = LocalDate.of(2026, 11, 3);
    static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");
    static CalendarEvent event(int id, LocalDateTime a, LocalDateTime b, boolean all) {
        return new CalendarEvent(id,"test","",a.atZone(ZONE).toInstant().toEpochMilli(),b.atZone(ZONE).toInstant().toEpochMilli(),all,0);
    }
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args) {
        var shortEvent=event(1,DAY.atTime(9,30),DAY.atTime(10,0),false);
        var longEvent=event(2,DAY.atTime(9,45),DAY.atTime(11,15),false);
        var adjacent=event(3,DAY.atTime(11,15),DAY.atTime(12,15),false);
        var overnight=event(4,DAY.minusDays(1).atTime(23,0),DAY.atTime(1,0),false);
        var nextDay=event(5,DAY.atTime(23,0),DAY.plusDays(1).atTime(1,0),false);
        var allDay=event(6,DAY.atStartOfDay(),DAY.plusDays(1).atStartOfDay(),true);
        var blocks=DayTimeline.layout(Arrays.asList(longEvent,shortEvent,adjacent,overnight,nextDay,allDay),DAY,ZONE);
        check(blocks.size()==5,"all-day events must stay outside the hour axis");
        check(blocks.get(0).start==0 && blocks.get(0).end==60,"clip start at midnight");
        var shortBlock=blocks.get(1);var longBlock=blocks.get(2);
        check(shortBlock.end-shortBlock.start==30,"30-minute event duration");
        check((longBlock.end-longBlock.start)/(shortBlock.end-shortBlock.start)==3,"90-minute event is three times as tall");
        check(shortBlock.columns==2 && longBlock.columns==2 && shortBlock.lane!=longBlock.lane,"overlap side by side");
        check(blocks.get(3).columns==1,"adjacent events use full width");
        check(blocks.get(4).end==1440,"clip end at midnight");
        check(DayTimeline.layout(Collections.emptyList(),DAY,ZONE).isEmpty(),"empty day");
        System.out.println("PASS: all-day separation, duration ratio, overlap lanes, adjacency, midnight clipping, empty day");
    }
}
