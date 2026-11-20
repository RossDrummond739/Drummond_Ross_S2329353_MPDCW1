package org.me.gcu.drummond_ross_s2329353;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.me.gcu.drummond_ross_s2329353.model.DurationClass;
import org.me.gcu.drummond_ross_s2329353.model.GeoPoint;
import org.me.gcu.drummond_ross_s2329353.util.RoadNames;

import java.util.ArrayList;
import java.util.List;

public class RoadNamesAndDistanceTest {

    @Test
    public void extractsRoadsFromText() {
        List<String> roads = new ArrayList<>(RoadNames.extract("M8 WB Woodside - A804 Phoenix Rd and A74M(S) / B957"));
        assertEquals(List.of("M8", "A804", "A74(M)", "B957"), roads);
    }

    @Test
    public void ignoresNonRoadWords() {
        assertTrue(RoadNames.extract("Major works at Ayr 2026, MB rooms").isEmpty());
    }

    @Test
    public void matchesRoadsSensibly() {
        assertTrue(RoadNames.matches("M8", "m8"));
        assertFalse(RoadNames.matches("M80", "M8"));
        assertTrue(RoadNames.matches("A74(M)", "A74"));
        assertTrue(RoadNames.matches("A74(M)", "a74m"));
        assertFalse(RoadNames.matches("A7", ""));
    }

    @Test
    public void sortsMotorwaysFirstThenNumerically() {
        List<String> roads = new ArrayList<>(List.of("B957", "A9", "M74", "M8", "A90"));
        roads.sort((a, b) -> RoadNames.sortKey(a).compareTo(RoadNames.sortKey(b)));
        assertEquals(List.of("M8", "M74", "A9", "A90", "B957"), roads);
    }

    @Test
    public void haversineGlasgowToEdinburgh() {
        GeoPoint glasgow = new GeoPoint(55.8642, -4.2518);
        GeoPoint edinburgh = new GeoPoint(55.9533, -3.1883);
        assertEquals(67.0, glasgow.distanceKmTo(edinburgh), 1.5);
        assertEquals(0.0, glasgow.distanceKmTo(glasgow), 1e-9);
    }

    @Test
    public void durationClasses() {
        assertEquals(DurationClass.SHORT, DurationClass.fromHours(10));
        assertEquals(DurationClass.MEDIUM, DurationClass.fromHours(24 * 10));
        assertEquals(DurationClass.LONG, DurationClass.fromHours(24 * 60));
        assertEquals(DurationClass.UNKNOWN, DurationClass.fromHours(-1));
    }
}
