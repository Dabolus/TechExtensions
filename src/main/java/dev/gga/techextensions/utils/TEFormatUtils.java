package dev.gga.techextensions.utils;

public final class TEFormatUtils {
    private TEFormatUtils() {}

    /** Formats a quantity compactly: 999 → "999", 16000 → "16.0K", 2500000 → "2.5M". */
    public static String formatNumber(long value) {
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000.0);
        } else if (value >= 1_000) {
            return String.format("%.1fK", value / 1_000.0);
        }
        return Long.toString(value);
    }
}
