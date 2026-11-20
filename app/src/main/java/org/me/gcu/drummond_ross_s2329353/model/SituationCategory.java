package org.me.gcu.drummond_ross_s2329353.model;

/**
 * App-level classification of a situation record. Each category has its own icon and colour.
 * Derived from the DATEX II xsi:type of the situationRecord plus its sub-type / impact.
 */
public enum SituationCategory {
    ROADWORKS("Roadworks"),
    ROAD_CLOSED("Road closed"),
    LANE_RESTRICTION("Lane restriction"),
    QUEUING_TRAFFIC("Queuing traffic"),
    ACCIDENT("Accident"),
    OBSTRUCTION("Obstruction"),
    CONDITIONS("Road conditions"),
    EVENT("Event"),
    OTHER("Other");

    private final String label;

    SituationCategory(String label) { this.label = label; }

    public String getLabel() { return label; }

    /**
     * Works out the category from the parsed XML values.
     *
     * @param recordType     xsi:type of situationRecord, e.g. "MaintenanceWorks"
     * @param subType        e.g. roadOrCarriagewayOrLaneManagementType "roadClosed"
     * @param lanesRestricted numberOfLanesRestricted or null
     * @param operationalLanes numberOfOperationalLanes or null
     */
    public static SituationCategory classify(String recordType, String subType,
                                             Integer lanesRestricted, Integer operationalLanes) {
        String type = recordType == null ? "" : recordType.toLowerCase();
        String sub = subType == null ? "" : subType.toLowerCase();

        if (type.contains("maintenanceworks") || type.contains("constructionworks") || sub.contains("roadworks")) {
            return ROADWORKS;
        }
        if (type.contains("roadorcarriagewayorlanemanagement") || type.contains("networkmanagement")) {
            if (lanesRestricted != null && lanesRestricted > 0) return LANE_RESTRICTION;
            if (sub.contains("lane") || sub.contains("contraflow") || sub.contains("narrow")
                    || sub.contains("alternate")) return LANE_RESTRICTION;
            return ROAD_CLOSED;
        }
        if (type.contains("abnormaltraffic")) return QUEUING_TRAFFIC;
        if (type.contains("accident")) return ACCIDENT;
        if (type.contains("obstruction") || type.contains("equipmentorsystemfault")) return OBSTRUCTION;
        if (type.contains("conditions") || type.contains("weather")) return CONDITIONS;
        if (type.contains("publicevent") || type.contains("disturbance")) return EVENT;
        if (operationalLanes != null && operationalLanes == 0) return ROAD_CLOSED;
        return OTHER;
    }
}
