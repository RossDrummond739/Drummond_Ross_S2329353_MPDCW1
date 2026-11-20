package org.me.gcu.drummond_ross_s2329353.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One end ("from" / "to") of a TPEG linear location, or a single TPEG point.
 * Names are broken down by their DATEX II descriptor type.
 */
public class LocationPoint {
    private String pointType;        // xsi:type, e.g. TpegJunction, TpegNonJunctionPoint
    private Double latitude;
    private Double longitude;
    private String junctionName;     // tpegJunctionPointDescriptorType = junctionName (e.g. "J19")
    private String roadName;         // linkName / tpegIlcName1 (e.g. "M8", "A74(M)")
    private String areaName;         // tpegIlcName2 / townName
    private String placeName;        // nonLinkedPointName / intersectionName (e.g. "M8 J18 Charing Cross")
    private final List<String> otherNames = new ArrayList<>();

    public String getPointType() { return pointType; }
    public void setPointType(String pointType) { this.pointType = pointType; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getJunctionName() { return junctionName; }
    public void setJunctionName(String junctionName) { this.junctionName = junctionName; }
    public String getRoadName() { return roadName; }
    public void setRoadName(String roadName) { this.roadName = roadName; }
    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }
    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }
    public List<String> getOtherNames() { return otherNames; }
    public void addOtherName(String name) { if (name != null && !otherNames.contains(name)) otherNames.add(name); }

    public boolean hasCoordinates() { return latitude != null && longitude != null; }

    public GeoPoint toGeoPoint() { return hasCoordinates() ? new GeoPoint(latitude, longitude) : null; }

    /** Best human readable description of the point. */
    public String describe() {
        if (placeName != null) return placeName;
        StringBuilder sb = new StringBuilder();
        if (roadName != null) sb.append(roadName);
        if (junctionName != null) sb.append(sb.length() > 0 ? " " : "").append(junctionName);
        if (areaName != null) sb.append(sb.length() > 0 ? " (" + areaName + ")" : areaName);
        if (sb.length() == 0 && !otherNames.isEmpty()) sb.append(otherNames.get(0));
        return sb.length() == 0 ? "Unnamed point" : sb.toString();
    }
}
