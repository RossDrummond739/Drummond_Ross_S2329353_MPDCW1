package org.me.gcu.drummond_ross_s2329353.model;

import org.me.gcu.drummond_ross_s2329353.util.DateUtils;
import org.me.gcu.drummond_ross_s2329353.util.RoadNames;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One DATEX II situationRecord (an incident or a roadworks entry) after parsing.
 * Holds the basic data shown in lists plus the additional impact / validity / location data.
 */
public class Situation {
    // Identification
    private FeedType feedType;
    private String situationId;
    private String recordId;
    private String version;
    private String recordType;           // xsi:type, e.g. MaintenanceWorks
    private String subType;              // e.g. roadMaintenanceType=roadworks
    private SituationCategory category = SituationCategory.OTHER;

    // Description
    private String comment;
    private OffsetDateTime creationTime;
    private OffsetDateTime versionTime;
    private String probability;
    private String sourceName;
    private String sourceType;
    private String complianceOption;
    private Boolean urgentRoadworks;

    // Validity and impact
    private final Validity validity = new Validity();
    private final Impact impact = new Impact();

    // Location
    private String locationType;         // groupOfLocations xsi:type (Linear / Point)
    private String direction;            // tpegDirection
    private String linearLocationType;   // tpegLinearLocationType
    private LocationPoint from;
    private LocationPoint to;
    private LocationPoint point;         // for single point locations

    // Derived
    private final Set<String> roads = new LinkedHashSet<>();
    /** Any XML fields not explicitly handled, so newly added feed data is still shown (never crashes). */
    private final Map<String, String> otherData = new LinkedHashMap<>();

    /** Holds the validity period of the situation. */
    public static class Validity {
        private String status;
        private OffsetDateTime start;
        private OffsetDateTime end;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public OffsetDateTime getStart() { return start; }
        public void setStart(OffsetDateTime start) { this.start = start; }
        public OffsetDateTime getEnd() { return end; }
        public void setEnd(OffsetDateTime end) { this.end = end; }

        public Duration getDuration() {
            return (start != null && end != null) ? Duration.between(start, end) : null;
        }

        public DurationClass getDurationClass() {
            Duration d = getDuration();
            return d == null ? DurationClass.UNKNOWN : DurationClass.fromHours(d.toHours());
        }
    }

    /** Holds the impact block (lanes and delays). */
    public static class Impact {
        private Integer operationalLanes;
        private Integer lanesRestricted;
        private String delaysType;
        private Integer delayTimeSeconds;

        public Integer getOperationalLanes() { return operationalLanes; }
        public void setOperationalLanes(Integer v) { this.operationalLanes = v; }
        public Integer getLanesRestricted() { return lanesRestricted; }
        public void setLanesRestricted(Integer v) { this.lanesRestricted = v; }
        public String getDelaysType() { return delaysType; }
        public void setDelaysType(String v) { this.delaysType = v; }
        public Integer getDelayTimeSeconds() { return delayTimeSeconds; }
        public void setDelayTimeSeconds(Integer v) { this.delayTimeSeconds = v; }
        public DelaySeverity getDelaySeverity() { return DelaySeverity.fromXml(delaysType); }

        public boolean isEmpty() {
            return operationalLanes == null && lanesRestricted == null && delaysType == null && delayTimeSeconds == null;
        }
    }

    /** Further processing once all XML for the record has been read. */
    public void finishParsing() {
        category = SituationCategory.classify(recordType, subType,
                impact.getLanesRestricted(), impact.getOperationalLanes());
        roads.clear();
        for (LocationPoint p : new LocationPoint[]{from, to, point}) {
            if (p != null && p.getRoadName() != null) {
                Set<String> found = RoadNames.extract(p.getRoadName());
                roads.addAll(found.isEmpty() ? Set.of(p.getRoadName().trim()) : found);
            }
        }
        roads.addAll(RoadNames.extract(comment));
        for (LocationPoint p : new LocationPoint[]{from, to, point}) {
            if (p != null) roads.addAll(RoadNames.extract(p.getPlaceName()));
        }
    }

    // ---- Convenience accessors used by the UI ----

    /** Title shown in the list: the type of the situation. */
    public String getTypeLabel() {
        if (feedType == FeedType.ROADWORKS) {
            return category == SituationCategory.ROADWORKS ? "Roadworks" : "Roadworks · " + category.getLabel();
        }
        return category == SituationCategory.OTHER && recordType != null
                ? DateUtils.humanise(recordType) : category.getLabel();
    }

    public String getPrimaryRoad() {
        return roads.isEmpty() ? null : roads.iterator().next();
    }

    public DelaySeverity getDelaySeverity() { return impact.getDelaySeverity(); }

    /** All coordinates of the situation (from, to, or a single point). */
    public List<GeoPoint> getGeoPoints() {
        List<GeoPoint> pts = new ArrayList<>();
        for (LocationPoint p : new LocationPoint[]{from, to, point}) {
            if (p != null && p.hasCoordinates()) pts.add(p.toGeoPoint());
        }
        return pts;
    }

    public boolean hasCoordinates() { return !getGeoPoints().isEmpty(); }

    /** Midpoint of from/to, or the single point; null when no coordinates. */
    public GeoPoint getCentre() {
        List<GeoPoint> pts = getGeoPoints();
        if (pts.isEmpty()) return null;
        if (pts.size() == 1) return pts.get(0);
        return GeoPoint.midpoint(pts.get(0), pts.get(1));
    }

    /** Shortest distance from origin to any point of the situation (km), or +infinity. */
    public double distanceKmFrom(GeoPoint origin) {
        double best = Double.POSITIVE_INFINITY;
        List<GeoPoint> pts = getGeoPoints();
        GeoPoint c = getCentre();
        if (c != null) pts.add(c);
        for (GeoPoint p : pts) best = Math.min(best, origin.distanceKmTo(p));
        return best;
    }

    public boolean isOnRoad(String query) {
        for (String r : roads) if (RoadNames.matches(r, query)) return true;
        return false;
    }

    // ---- Plain getters / setters ----
    public FeedType getFeedType() { return feedType; }
    public void setFeedType(FeedType feedType) { this.feedType = feedType; }
    public String getSituationId() { return situationId; }
    public void setSituationId(String situationId) { this.situationId = situationId; }
    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }
    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }
    public SituationCategory getCategory() { return category; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public OffsetDateTime getCreationTime() { return creationTime; }
    public void setCreationTime(OffsetDateTime creationTime) { this.creationTime = creationTime; }
    public OffsetDateTime getVersionTime() { return versionTime; }
    public void setVersionTime(OffsetDateTime versionTime) { this.versionTime = versionTime; }
    public String getProbability() { return probability; }
    public void setProbability(String probability) { this.probability = probability; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getComplianceOption() { return complianceOption; }
    public void setComplianceOption(String complianceOption) { this.complianceOption = complianceOption; }
    public Boolean getUrgentRoadworks() { return urgentRoadworks; }
    public void setUrgentRoadworks(Boolean urgentRoadworks) { this.urgentRoadworks = urgentRoadworks; }
    public Validity getValidity() { return validity; }
    public Impact getImpact() { return impact; }
    public String getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getLinearLocationType() { return linearLocationType; }
    public void setLinearLocationType(String v) { this.linearLocationType = v; }
    public LocationPoint getFrom() { return from; }
    public void setFrom(LocationPoint from) { this.from = from; }
    public LocationPoint getTo() { return to; }
    public void setTo(LocationPoint to) { this.to = to; }
    public LocationPoint getPoint() { return point; }
    public void setPoint(LocationPoint point) { this.point = point; }
    public Set<String> getRoads() { return roads; }
    public Map<String, String> getOtherData() { return otherData; }
}
