package org.me.gcu.drummond_ross_s2329353.util;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Parsing and formatting of the ISO-8601 timestamps used in DATEX II (e.g. 2026-08-10T11:37:42+01:00). */
public final class DateUtils {
    private static final ZoneId UK = ZoneId.of("Europe/London");
    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.UK);
    private static final DateTimeFormatter SHORT_DATE =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.UK);

    private DateUtils() { }

    /** Returns null (never throws) when the text isn't a valid timestamp. */
    public static OffsetDateTime parse(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return OffsetDateTime.parse(text.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static String format(OffsetDateTime t) {
        return t == null ? "-" : t.atZoneSameInstant(UK).format(DISPLAY);
    }

    public static String formatDate(OffsetDateTime t) {
        return t == null ? "-" : t.atZoneSameInstant(UK).format(SHORT_DATE);
    }

    public static String formatTime(long epochMillis) {
        return java.time.Instant.ofEpochMilli(epochMillis).atZone(UK).format(TIME);
    }

    public static String formatDateTime(long epochMillis) {
        return java.time.Instant.ofEpochMilli(epochMillis).atZone(UK).format(DISPLAY);
    }

    /** "3 days", "5 hours", "2 years 1 month"… */
    public static String humanDuration(Duration d) {
        if (d == null || d.isNegative()) return "-";
        long hours = d.toHours();
        if (hours < 1) return d.toMinutes() + " min";
        if (hours < 48) return hours + (hours == 1 ? " hour" : " hours");
        long days = d.toDays();
        if (days < 60) return days + " days";
        long months = days / 30;
        if (months < 24) return months + " months";
        return (days / 365) + " years";
    }

    /** "queuingTraffic" -> "Queuing traffic", "RoadOrCarriagewayOrLaneManagement" -> "Road or carriageway or lane management". */
    public static String humanise(String camel) {
        if (camel == null || camel.isEmpty()) return "";
        String spaced = camel.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ').toLowerCase(Locale.UK);
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
