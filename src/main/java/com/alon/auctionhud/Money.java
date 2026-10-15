package com.alon.auctionhud;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses and formats amounts like 1,500 / 20k / 15M / 1.5B / 2T. */
public final class Money {
    private static final Pattern INPUT =
            Pattern.compile("^\\$?\\s*(\\d[\\d,]*(?:\\.\\d+)?)\\s*([kKmMbBtT])?\\s*\\$?$");
    private static final DecimalFormat DF = new DecimalFormat("0.##");

    private Money() {}

    public static long parse(String number, String suffix) {
        try {
            BigDecimal v = new BigDecimal(number.replace(",", ""));
            v = v.multiply(BigDecimal.valueOf(multiplier(suffix)));
            return v.longValue();
        } catch (Exception e) {
            return -1;
        }
    }

    /** For user input in commands. Returns -1 if invalid. */
    public static long parseInput(String s) {
        Matcher m = INPUT.matcher(s.trim());
        if (!m.matches()) return -1;
        return parse(m.group(1), m.group(2));
    }

    private static long multiplier(String suffix) {
        if (suffix == null || suffix.isEmpty()) return 1L;
        return switch (Character.toLowerCase(suffix.charAt(0))) {
            case 'k' -> 1_000L;
            case 'm' -> 1_000_000L;
            case 'b' -> 1_000_000_000L;
            case 't' -> 1_000_000_000_000L;
            default -> 1L;
        };
    }

    public static String format(long v) {
        if (v >= 1_000_000_000_000L) return DF.format(v / 1e12) + "T";
        if (v >= 1_000_000_000L) return DF.format(v / 1e9) + "B";
        if (v >= 1_000_000L) return DF.format(v / 1e6) + "M";
        if (v >= 1_000L) return DF.format(v / 1e3) + "K";
        return Long.toString(v);
    }
}
