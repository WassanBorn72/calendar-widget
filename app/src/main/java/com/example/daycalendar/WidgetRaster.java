package com.example.daycalendar;

/** Physical-pixel raster sizes, bounded per RemoteViews row for Binder safety. */
final class WidgetRaster {
    static final int SLICE_HEIGHT = 36;
    static final int SLICE_COUNT = 49;
    static final int MAX_BITMAP_BYTES = 512 * 1024;
    static final String WIDTH_DP = "timeline_width_dp";

    record Size(int width, int height) { }

    static int sliceHeight(int index) {
        if (index < 0 || index >= SLICE_COUNT) throw new IllegalArgumentException("Invalid timeline slice");
        return index == SLICE_COUNT - 1 ? 24 : SLICE_HEIGHT;
    }

    static Size size(int widthDp, int heightDp, float density) {
        if (widthDp <= 0 || heightDp <= 0 || !Float.isFinite(density) || density <= 0)
            throw new IllegalArgumentException("Positive size and density required");
        double scale = Math.min(density, Math.sqrt(MAX_BITMAP_BYTES / (4.0 * widthDp * heightDp)));
        return new Size(Math.max(1, (int)(widthDp * scale)), Math.max(1, (int)(heightDp * scale)));
    }
}
