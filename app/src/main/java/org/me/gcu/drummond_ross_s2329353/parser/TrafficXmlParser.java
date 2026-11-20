package org.me.gcu.drummond_ross_s2329353.parser;

import org.me.gcu.drummond_ross_s2329353.model.FeedType;
import org.me.gcu.drummond_ross_s2329353.model.LocationPoint;
import org.me.gcu.drummond_ross_s2329353.model.ParseResult;
import org.me.gcu.drummond_ross_s2329353.model.Situation;
import org.me.gcu.drummond_ross_s2329353.util.DateUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Parses a Traffic Scotland DATEX II publication (already downloaded into a String)
 * with {@link XmlPullParser}. Each &lt;situationRecord&gt; becomes one {@link Situation}.
 *
 * <p>The parser keeps a stack of open element names so that generic tags such as
 * &lt;value&gt; can be interpreted by context (comment, source name, location names…).
 * Unknown elements are stored in {@link Situation#getOtherData()} rather than causing errors.</p>
 */
public class TrafficXmlParser {

    /** Creates the pull parser; swapped in unit tests for a plain-JVM implementation. */
    public interface PullParserProvider {
        XmlPullParser create() throws XmlPullParserException;
    }

    private final PullParserProvider provider;

    public TrafficXmlParser() {
        this(() -> XmlPullParserFactory.newInstance().newPullParser());
    }

    public TrafficXmlParser(PullParserProvider provider) {
        this.provider = provider;
    }

    // Per-parse state
    private Deque<String> path;
    private Situation current;
    private LocationPoint currentPoint;
    private String pendingDescriptor;
    private OffsetDateTime publicationTime;
    private String currentSituationId;

    public synchronized ParseResult parse(String xml, FeedType feedType) throws XmlPullParserException, IOException {
        List<Situation> situations = new ArrayList<>();
        path = new ArrayDeque<>();
        current = null;
        currentPoint = null;
        pendingDescriptor = null;
        publicationTime = null;
        currentSituationId = null;

        XmlPullParser xpp = provider.create();
        // Keep prefixes in names (e.g. "soapenv:Body") and read xsi:type as a plain attribute.
        xpp.setInput(new StringReader(xml));

        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                startTag(xpp, feedType);
            } else if (eventType == XmlPullParser.TEXT) {
                String text = xpp.getText();
                if (text != null && !text.trim().isEmpty() && !path.isEmpty()) {
                    handleText(path.peek(), text.trim());
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                String name = localName(xpp.getName());
                if ("situationRecord".equals(name) && current != null) {
                    current.finishParsing();
                    situations.add(current);
                    current = null;
                } else if (currentPoint != null && isPointElement(name)) {
                    assignPoint(name);
                } else if (currentPoint != null && isNameElement(name) && pendingDescriptor != null) {
                    // Descriptor without a type we recognise: keep it as an extra name
                    currentPoint.addOtherName(pendingDescriptor);
                    pendingDescriptor = null;
                }
                if (!path.isEmpty()) path.pop();
            }
            eventType = xpp.next();
        }
        return new ParseResult(publicationTime, situations);
    }

    private void startTag(XmlPullParser xpp, FeedType feedType) {
        String name = localName(xpp.getName());
        path.push(name);
        switch (name) {
            case "situation":
                currentSituationId = attribute(xpp, "id");
                break;
            case "situationRecord":
                current = new Situation();
                current.setFeedType(feedType);
                current.setSituationId(currentSituationId);
                current.setRecordId(attribute(xpp, "id"));
                current.setVersion(attribute(xpp, "version"));
                current.setRecordType(attribute(xpp, "type"));
                break;
            case "groupOfLocations":
                if (current != null) current.setLocationType(attribute(xpp, "type"));
                break;
            default:
                if (current != null && isPointElement(name) && inside("groupOfLocations")) {
                    currentPoint = new LocationPoint();
                    currentPoint.setPointType(attribute(xpp, "type"));
                } else if (isNameElement(name)) {
                    pendingDescriptor = null;
                }
        }
    }

    private void handleText(String tag, String text) {
        if (current == null) {
            if ("publicationTime".equals(tag) && parentIs("payloadPublication")) {
                publicationTime = DateUtils.parse(text);
            }
            return;
        }
        if (currentPoint != null) {
            handlePointText(tag, text);
            return;
        }
        switch (tag) {
            case "situationRecordCreationTime": current.setCreationTime(DateUtils.parse(text)); break;
            case "situationRecordVersionTime": current.setVersionTime(DateUtils.parse(text)); break;
            case "situationRecordCreationReference":
            case "situationRecordFirstSupplierVersionTime": break; // duplicates of id / version time
            case "probabilityOfOccurrence": current.setProbability(text); break;
            case "sourceIdentification": if (current.getSourceName() == null) current.setSourceName(text); break;
            case "sourceType": current.setSourceType(text); break;
            case "validityStatus": current.getValidity().setStatus(text); break;
            case "overallStartTime": current.getValidity().setStart(DateUtils.parse(text)); break;
            case "overallEndTime": current.getValidity().setEnd(DateUtils.parse(text)); break;
            case "numberOfOperationalLanes": current.getImpact().setOperationalLanes(toInt(text)); break;
            case "numberOfLanesRestricted": current.getImpact().setLanesRestricted(toInt(text)); break;
            case "delaysType": current.getImpact().setDelaysType(text); break;
            case "delayTimeValue": current.getImpact().setDelayTimeSeconds(toInt(text)); break;
            case "tpegDirection": current.setDirection(text); break;
            case "tpegLinearLocationType": current.setLinearLocationType(text); break;
            case "complianceOption": current.setComplianceOption(text); break;
            case "urgentRoadworks": current.setUrgentRoadworks(Boolean.parseBoolean(text)); break;
            case "value":
                if (inside("generalPublicComment")) {
                    String existing = current.getComment();
                    if (existing == null) current.setComment(text);
                    else if (!existing.contains(text)) current.setComment(existing + "\n" + text);
                } else if (inside("sourceName")) {
                    current.setSourceName(text);
                } else {
                    storeOther(text);
                }
                break;
            default:
                // e.g. roadMaintenanceType, abnormalTrafficType, roadOrCarriagewayOrLaneManagementType,
                // accidentType, vehicleObstructionType… directly inside the record
                if (tag.endsWith("Type") && parentIs("situationRecord") && current.getSubType() == null) {
                    current.setSubType(text);
                } else {
                    storeOther(text);
                }
        }
    }

    private void handlePointText(String tag, String text) {
        switch (tag) {
            case "latitude": currentPoint.setLatitude(toDouble(text)); break;
            case "longitude": currentPoint.setLongitude(toDouble(text)); break;
            case "value": pendingDescriptor = text; break;
            case "tpegJunctionPointDescriptorType":
                if ("junctionName".equals(text)) currentPoint.setJunctionName(pendingDescriptor);
                else currentPoint.addOtherName(pendingDescriptor);
                pendingDescriptor = null;
                break;
            case "tpegIlcPointDescriptorType":
                if ("tpegIlcName1".equals(text)) setRoadIfAbsent(pendingDescriptor);
                else if ("tpegIlcName2".equals(text)) currentPoint.setAreaName(pendingDescriptor);
                else currentPoint.addOtherName(pendingDescriptor);
                pendingDescriptor = null;
                break;
            case "tpegOtherPointDescriptorType":
                switch (text) {
                    case "linkName": setRoadIfAbsent(pendingDescriptor); break;
                    case "nonLinkedPointName":
                    case "intersectionName": currentPoint.setPlaceName(pendingDescriptor); break;
                    case "townName": currentPoint.setAreaName(pendingDescriptor); break;
                    default: currentPoint.addOtherName(pendingDescriptor);
                }
                pendingDescriptor = null;
                break;
            default:
                storeOther(text);
        }
    }

    private void setRoadIfAbsent(String road) {
        if (road != null && currentPoint.getRoadName() == null) currentPoint.setRoadName(road);
    }

    private void assignPoint(String name) {
        switch (name) {
            case "from": current.setFrom(currentPoint); break;
            case "to": current.setTo(currentPoint); break;
            default: current.setPoint(currentPoint);
        }
        currentPoint = null;
        pendingDescriptor = null;
    }

    /** Store a field we don't explicitly handle under a readable path, e.g. "Impact › Delays › Delay time value". */
    private void storeOther(String text) {
        List<String> parts = new ArrayList<>();
        Iterator<String> it = path.descendingIterator();
        boolean inRecord = false;
        while (it.hasNext()) {
            String p = it.next();
            if (inRecord && !"values".equals(p) && !"value".equals(p)) parts.add(DateUtils.humanise(p));
            if ("situationRecord".equals(p)) inRecord = true;
        }
        if (parts.isEmpty()) return;
        String key = String.join(" › ", parts);
        String existing = current.getOtherData().get(key);
        current.getOtherData().put(key, existing == null ? text : existing + ", " + text);
    }

    // ---- small helpers ----

    private static boolean isPointElement(String name) {
        return "from".equals(name) || "to".equals(name) || "locationForDisplay".equals(name)
                || "point".equals(name) || "tpegPointLocation".equals(name);
    }

    private static boolean isNameElement(String name) {
        return "name".equals(name) || "ilc".equals(name) || "otherName".equals(name);
    }

    private boolean inside(String element) { return path.contains(element); }

    private boolean parentIs(String element) {
        Iterator<String> it = path.iterator();
        if (!it.hasNext()) return false;
        it.next();                       // current element
        return it.hasNext() && element.equals(it.next());
    }

    /** Strip any namespace prefix: "soapenv:Body" -> "Body". */
    static String localName(String name) {
        if (name == null) return "";
        int i = name.indexOf(':');
        return i >= 0 ? name.substring(i + 1) : name;
    }

    /** Finds an attribute by local name (so "xsi:type" matches "type"). */
    static String attribute(XmlPullParser xpp, String localName) {
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            if (localName.equals(localName(xpp.getAttributeName(i)))) return xpp.getAttributeValue(i);
        }
        return null;
    }

    private static Integer toInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static Double toDouble(String s) {
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return null; }
    }
}
