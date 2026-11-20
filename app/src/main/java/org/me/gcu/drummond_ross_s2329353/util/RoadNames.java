package org.me.gcu.drummond_ross_s2329353.util;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String helpers for Scottish road references such as "M8", "A74(M)", "A74M(S)" or "B957".
 * Used to break the free-text descriptions down into searchable road names.
 */
public final class RoadNames {
    // M/A/B followed by 1-4 digits, optionally an "(M)" / "M" motorway suffix and "(S)".
    private static final Pattern ROAD = Pattern.compile(
            "(?<![A-Za-z0-9])([MAB])\\s?([0-9]{1,4})(\\(M\\)|M(?![A-Za-z]))?(\\(S\\))?(?![0-9A-Za-z])");

    private RoadNames() { }

    /** All road references found in free text, in canonical form, in order of appearance. */
    public static Set<String> extract(String text) {
        Set<String> roads = new LinkedHashSet<>();
        if (text == null) return roads;
        Matcher m = ROAD.matcher(text);
        while (m.find()) {
            roads.add(canonical(m.group(1) + m.group(2) + (m.group(3) != null ? "(M)" : "")));
        }
        return roads;
    }

    /** Canonical display form, e.g. "a74m" -> "A74(M)", " m 8 " -> "M8". */
    public static String canonical(String road) {
        if (road == null) return null;
        String key = normalise(road);
        Matcher m = Pattern.compile("^([MAB])([0-9]{1,4})(M)?(S)?$").matcher(key);
        if (!m.matches()) return road.trim().toUpperCase(Locale.UK);
        return m.group(1) + m.group(2) + (m.group(3) != null ? "(M)" : "");
    }

    /** Upper-case with everything except letters and digits removed: "A74(M)" -> "A74M". */
    public static String normalise(String s) {
        return s == null ? "" : s.toUpperCase(Locale.UK).replaceAll("[^A-Z0-9]", "");
    }

    /**
     * True when the road matches the user's query. "M8" matches "M8" but not "M80";
     * "A74" matches "A74" and "A74(M)".
     */
    public static boolean matches(String road, String query) {
        String r = normalise(road);
        String q = normalise(query);
        if (q.isEmpty() || r.isEmpty()) return false;
        if (r.equals(q)) return true;
        return r.startsWith(q) && !Character.isDigit(r.charAt(q.length()));
    }

    /** Sort key so roads order as M, A, B then numerically (M8 before M74). */
    public static String sortKey(String road) {
        String n = normalise(road);
        if (n.isEmpty()) return "z";
        char cls = n.charAt(0);
        String order = cls == 'M' ? "0" : cls == 'A' ? "1" : cls == 'B' ? "2" : "3";
        String digits = n.replaceAll("[^0-9]", "");
        String pad = digits.isEmpty() ? "9999" : String.format(Locale.UK, "%04d", Integer.parseInt(digits));
        return order + pad + n;
    }
}
