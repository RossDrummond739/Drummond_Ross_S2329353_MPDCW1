package org.me.gcu.drummond_ross_s2329353.viewmodel;

import org.me.gcu.drummond_ross_s2329353.model.NearbyOrigin;
import org.me.gcu.drummond_ross_s2329353.model.Situation;

import java.util.List;

/** What the map should currently display. */
public class MapContent {
    public enum Mode { ALL, ROAD_SEARCH, NEARBY, SINGLE }

    private final Mode mode;
    private final String title;
    private final List<Situation> situations;
    private final NearbyOrigin origin;   // only for NEARBY
    private final int radiusKm;          // only for NEARBY

    public MapContent(Mode mode, String title, List<Situation> situations, NearbyOrigin origin, int radiusKm) {
        this.mode = mode;
        this.title = title;
        this.situations = situations;
        this.origin = origin;
        this.radiusKm = radiusKm;
    }

    public Mode getMode() { return mode; }
    public String getTitle() { return title; }
    public List<Situation> getSituations() { return situations; }
    public NearbyOrigin getOrigin() { return origin; }
    public int getRadiusKm() { return radiusKm; }
}
