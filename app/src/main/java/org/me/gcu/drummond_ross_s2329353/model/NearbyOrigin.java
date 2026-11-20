package org.me.gcu.drummond_ross_s2329353.model;

/** The point the "nearby" search measures from: the phone's coarse location or a fixed place. */
public final class NearbyOrigin {
    private final String name;
    private final GeoPoint point;
    private final boolean deviceLocation;

    public NearbyOrigin(String name, GeoPoint point, boolean deviceLocation) {
        this.name = name;
        this.point = point;
        this.deviceLocation = deviceLocation;
    }

    public String getName() { return name; }
    public GeoPoint getPoint() { return point; }
    public boolean isDeviceLocation() { return deviceLocation; }
}
