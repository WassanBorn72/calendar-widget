#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
output=$(mktemp -d)
trap 'rm -rf "$output"' EXIT
javac -d "$output" app/src/main/java/com/example/daycalendar/CalendarEvent.java app/src/main/java/com/example/daycalendar/DayTimeline.java tests/DayTimelineTest.java
java -cp "$output" com.example.daycalendar.DayTimelineTest
