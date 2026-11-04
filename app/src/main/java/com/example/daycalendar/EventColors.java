package com.example.daycalendar;

/** Shared palette for provider colors, app Canvas, and native RemoteViews. */
final class EventColors {
    static final int SECONDARY_TEXT = 0xff4e5f55;
    static final int DEFAULT = 0xff36674e;
    private static final int INK = 0xff233e36;
    private EventColors() { }

    static int resolve(Integer event, Integer display, Integer calendar) {
        Integer selected = event != null ? event : display != null ? display : calendar;
        return selected == null ? DEFAULT : selected | 0xff000000;
    }
    static int accent(int color) { return color == 0 ? DEFAULT : color | 0xff000000; }
    static int fill(int color) { return mix(accent(color), 0xffffffff, .84f); }
    static int border(int color) { return mix(mix(accent(color), INK, .55f), 0xffffffff, .62f); }
    private static int mix(int a, int b, float ratio) {
        int red=Math.round(((a>>16)&255)*(1-ratio)+((b>>16)&255)*ratio);
        int green=Math.round(((a>>8)&255)*(1-ratio)+((b>>8)&255)*ratio);
        int blue=Math.round((a&255)*(1-ratio)+(b&255)*ratio);
        return 0xff000000 | red<<16 | green<<8 | blue;
    }
}
