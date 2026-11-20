package org.me.gcu.drummond_ross_s2329353.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.me.gcu.drummond_ross_s2329353.model.GeoPoint;

/** User preferences: auto-refresh interval, location used for "nearby" search, and search radius. */
public class SettingsRepository {
    public static final int[] REFRESH_OPTIONS_MIN = {1, 5, 15, 30, 60, 120};
    public static final int DEFAULT_REFRESH_MIN = 60;

    /** Fixed locations for the distance search (used when not using the phone's location). */
    public static final String[] PLACE_NAMES = {
            "GCU, Glasgow", "Edinburgh", "Aberdeen", "Dundee", "Inverness", "Stirling", "Perth", "Ayr"};
    public static final GeoPoint[] PLACE_POINTS = {
            new GeoPoint(55.8666, -4.2503), new GeoPoint(55.9533, -3.1883), new GeoPoint(57.1497, -2.0943),
            new GeoPoint(56.4620, -2.9707), new GeoPoint(57.4778, -4.2247), new GeoPoint(56.1165, -3.9369),
            new GeoPoint(56.3950, -3.4308), new GeoPoint(55.4586, -4.6292)};

    private static final String KEY_REFRESH = "refresh_minutes";
    private static final String KEY_RADIUS = "radius_km";
    private static final String KEY_PLACE = "place_index";

    private final SharedPreferences prefs;

    public SettingsRepository(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    public int getRefreshMinutes() { return prefs.getInt(KEY_REFRESH, DEFAULT_REFRESH_MIN); }
    public void setRefreshMinutes(int m) { prefs.edit().putInt(KEY_REFRESH, m).apply(); }

    public int getRadiusKm() { return prefs.getInt(KEY_RADIUS, 20); }
    public void setRadiusKm(int km) { prefs.edit().putInt(KEY_RADIUS, km).apply(); }

    public int getPlaceIndex() { return prefs.getInt(KEY_PLACE, 0); }
    public void setPlaceIndex(int i) { prefs.edit().putInt(KEY_PLACE, i).apply(); }
}
