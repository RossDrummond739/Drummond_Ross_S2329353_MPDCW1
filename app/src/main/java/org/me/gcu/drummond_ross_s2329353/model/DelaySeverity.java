package org.me.gcu.drummond_ross_s2329353.model;

/** DATEX II delaysType values, ordered from least to most severe. */
public enum DelaySeverity {
    NONE("No delays reported"),
    DELAYS("Delays"),
    LONG("Long delays"),
    VERY_LONG("Very long delays");

    private final String label;

    DelaySeverity(String label) { this.label = label; }

    public String getLabel() { return label; }

    public static DelaySeverity fromXml(String delaysType) {
        if (delaysType == null) return NONE;
        switch (delaysType.trim()) {
            case "veryLongDelays": return VERY_LONG;
            case "longDelays": return LONG;
            case "delays":
            case "delaysOfUncertainDuration":
            case "negligible": return DELAYS;
            default: return NONE;
        }
    }
}
