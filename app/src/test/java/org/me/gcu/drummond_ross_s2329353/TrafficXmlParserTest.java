package org.me.gcu.drummond_ross_s2329353;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.kxml2.io.KXmlParser;
import org.me.gcu.drummond_ross_s2329353.model.DelaySeverity;
import org.me.gcu.drummond_ross_s2329353.model.FeedType;
import org.me.gcu.drummond_ross_s2329353.model.ParseResult;
import org.me.gcu.drummond_ross_s2329353.model.Situation;
import org.me.gcu.drummond_ross_s2329353.model.SituationCategory;
import org.me.gcu.drummond_ross_s2329353.parser.TrafficXmlParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Checks the parsed data matches the sample feeds supplied with the coursework. */
public class TrafficXmlParserTest {

    private TrafficXmlParser parser;

    @Before
    public void setUp() {
        parser = new TrafficXmlParser(KXmlParser::new);
    }

    private String load(String name) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            assertNotNull("missing test resource " + name, in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void parsesAllUnplannedEvents() throws Exception {
        ParseResult result = parser.parse(load("unplannedContentExample.xml"), FeedType.UNPLANNED);
        List<Situation> list = result.getSituations();
        assertEquals(4, list.size());
        assertNotNull(result.getPublicationTime());

        Situation first = list.get(0);
        assertEquals("I_20260810_248_S", first.getSituationId());
        assertEquals("I_20260810_248", first.getRecordId());
        assertEquals("RoadOrCarriagewayOrLaneManagement", first.getRecordType());
        assertEquals("roadClosed", first.getSubType());
        assertEquals(SituationCategory.ROAD_CLOSED, first.getCategory());
        assertEquals("The A74M(S) junction 19 (Ecclefechan) off slip is closed to allow for essential roadworks.",
                first.getComment());
        assertEquals(Integer.valueOf(0), first.getImpact().getOperationalLanes());
        assertEquals("southBound", first.getDirection());
        assertEquals("mandatory", first.getComplianceOption());
        assertEquals("TrafficScotland", first.getSourceName());

        // Location broken down into its parts
        assertEquals(55.065432, first.getTo().getLatitude(), 1e-9);
        assertEquals(-3.273135, first.getTo().getLongitude(), 1e-9);
        assertEquals("J19", first.getTo().getJunctionName());
        assertEquals("A74(M)", first.getTo().getRoadName());
        assertEquals("A74(M) J19 Eaglesfield", first.getTo().getPlaceName());
        assertEquals("J18", first.getFrom().getJunctionName());
        assertEquals("A74(M) J18 Ecclefechan", first.getFrom().getPlaceName());
        assertTrue(first.getRoads().contains("A74(M)"));
        assertTrue(first.isOnRoad("a74"));
        assertNotNull(first.getValidity().getStart());
    }

    @Test
    public void classifiesClosuresAndQueues() throws Exception {
        List<Situation> list = parser.parse(load("unplannedContentExample.xml"), FeedType.UNPLANNED).getSituations();
        int closed = 0, queues = 0;
        for (Situation s : list) {
            if (s.getCategory() == SituationCategory.ROAD_CLOSED) closed++;
            if (s.getCategory() == SituationCategory.QUEUING_TRAFFIC) {
                queues++;
                assertEquals("AbnormalTraffic", s.getRecordType());
                assertEquals("queuingTraffic", s.getSubType());
                assertEquals(Integer.valueOf(2), s.getImpact().getLanesRestricted());
                assertEquals("2 lanes restricted Eastbound", s.getComment());
                assertTrue(s.isOnRoad("M8"));
            }
        }
        assertEquals(3, closed);
        assertEquals(1, queues);
    }

    @Test
    public void parsesAllRoadworks() throws Exception {
        List<Situation> list = parser.parse(load("roadworksContentExample.xml"), FeedType.ROADWORKS).getSituations();
        assertEquals(405, list.size());
        int delays = 0, longDelays = 0, veryLong = 0;
        for (Situation s : list) {
            assertEquals(SituationCategory.ROADWORKS, s.getCategory());
            assertNotNull(s.getComment());
            assertTrue(s.hasCoordinates());
            DelaySeverity d = s.getDelaySeverity();
            if (d == DelaySeverity.DELAYS) delays++;
            if (d == DelaySeverity.LONG) longDelays++;
            if (d == DelaySeverity.VERY_LONG) veryLong++;
        }
        // Counts taken from the sample file
        assertEquals(130, delays);
        assertEquals(26, longDelays);
        assertEquals(1, veryLong);

        Situation first = list.get(0);
        assertEquals("D_159301", first.getRecordId());
        assertEquals("M8 WB Woodside - A804 Phoenix Rd - Closure", first.getComment());
        assertEquals("M8", first.getTo().getRoadName());
        assertEquals("M8 (Jct 18 Off Slip to Entry A82)", first.getTo().getPlaceName());
        assertEquals(Boolean.FALSE, first.getUrgentRoadworks());
        assertEquals("roadworks", first.getSubType());
        assertNotNull(first.getValidity().getEnd());
        assertTrue(first.isOnRoad("M8"));
        assertFalse(first.isOnRoad("M80"));
    }

    @Test
    public void unknownElementsAreKeptNotFatal() throws Exception {
        String xml = "<d2LogicalModel><payloadPublication><situation id='S1'>"
                + "<situationRecord xsi:type='Accident' id='R1' version='1'>"
                + "<impact><brandNewField>42</brandNewField></impact>"
                + "<generalPublicComment><comment><values><value lang='en'>Crash on the M9</value></values></comment></generalPublicComment>"
                + "<accidentType>collision</accidentType>"
                + "</situationRecord></situation></payloadPublication></d2LogicalModel>";
        List<Situation> list = parser.parse(xml, FeedType.UNPLANNED).getSituations();
        assertEquals(1, list.size());
        Situation s = list.get(0);
        assertEquals(SituationCategory.ACCIDENT, s.getCategory());
        assertEquals("collision", s.getSubType());
        assertEquals("42", s.getOtherData().get("Impact › Brand new field"));
        assertTrue(s.isOnRoad("M9"));
        assertNull(s.getCentre());
    }

    @Test
    public void emptyPublicationGivesEmptyList() throws Exception {
        String xml = "<d2LogicalModel><payloadPublication><publicationTime>2026-11-20T09:00:00Z</publicationTime>"
                + "</payloadPublication></d2LogicalModel>";
        ParseResult r = parser.parse(xml, FeedType.ROADWORKS);
        assertTrue(r.getSituations().isEmpty());
        assertNotNull(r.getPublicationTime());
    }
}
