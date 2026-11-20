package org.me.gcu.drummond_ross_s2329353.model;

import java.util.Locale;

/** Immutable latitude/longitude pair with a haversine distance helper. */
public final class GeoPoint {
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private final double latitude;
    private final double longitude;

    public GeoPoint(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    /** Great-circle distance in kilometres. */
    public double distanceKmTo(GeoPoint other) {
        double dLat = Math.toRadians(other.latitude - latitude);
        double dLon = Math.toRadians(other.longitude - longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.latitude))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    public static GeoPoint midpoint(GeoPoint a, GeoPoint b) {
        return new GeoPoint((a.latitude + b.latitude) / 2.0, (a.longitude + b.longitude) / 2.0);
    }

    @Override
    public String toString() {
        return String.format(Locale.UK, "%.5f, %.5f", latitude, longitude);
    }
}
