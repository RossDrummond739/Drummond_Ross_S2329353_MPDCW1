package org.me.gcu.drummond_ross_s2329353.model;

/** Planned duration of a situation, from its validity start/end times. Used for colour coding. */
public enum DurationClass {
    SHORT("Short (up to 3 days)"),
    MEDIUM("Medium (up to 4 weeks)"),
    LONG("Long (over 4 weeks)"),
    UNKNOWN("Duration not given");

    public static final long SHORT_MAX_HOURS = 72;
    public static final long MEDIUM_MAX_HOURS = 28 * 24;

    private final String label;

    DurationClass(String label) { this.label = label; }

    public String getLabel() { return label; }

    public static DurationClass fromHours(long hours) {
        if (hours < 0) return UNKNOWN;
        if (hours <= SHORT_MAX_HOURS) return SHORT;
        if (hours <= MEDIUM_MAX_HOURS) return MEDIUM;
        return LONG;
    }
}
