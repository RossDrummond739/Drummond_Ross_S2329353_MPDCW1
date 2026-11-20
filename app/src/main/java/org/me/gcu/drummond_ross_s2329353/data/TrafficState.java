package org.me.gcu.drummond_ross_s2329353.data;

import org.me.gcu.drummond_ross_s2329353.model.FeedType;
import org.me.gcu.drummond_ross_s2329353.model.Situation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Immutable snapshot of everything the UI needs to know about the traffic data. */
public class TrafficState {
    private final List<Situation> situations;
    private final Map<FeedType, FeedStatus> feeds;
    private final boolean loading;
    private final long lastAttempt;

    public TrafficState(List<Situation> situations, Map<FeedType, FeedStatus> feeds, boolean loading, long lastAttempt) {
        this.situations = Collections.unmodifiableList(situations);
        EnumMap<FeedType, FeedStatus> copy = new EnumMap<>(FeedType.class);
        copy.putAll(feeds);   // EnumMap(Map) throws on an empty non-EnumMap
        this.feeds = Collections.unmodifiableMap(copy);
        this.loading = loading;
        this.lastAttempt = lastAttempt;
    }

    public static TrafficState initial() {
        return new TrafficState(Collections.emptyList(), new EnumMap<>(FeedType.class), true, 0);
    }

    public TrafficState withLoading(boolean isLoading) {
        return new TrafficState(situations, feeds, isLoading, lastAttempt);
    }

    public List<Situation> getSituations() { return situations; }
    public Map<FeedType, FeedStatus> getFeeds() { return feeds; }
    public boolean isLoading() { return loading; }
    public long getLastAttempt() { return lastAttempt; }

    public boolean anySource(DataSource s) {
        for (FeedStatus f : feeds.values()) if (f.getSource() == s) return true;
        return false;
    }

    public boolean allLive() {
        if (feeds.size() < FeedType.values().length) return false;
        for (FeedStatus f : feeds.values()) if (f.getSource() != DataSource.LIVE) return false;
        return true;
    }

    /** Oldest download time among the shown feeds (0 when unknown). */
    public long oldestDataTimestamp() {
        long oldest = Long.MAX_VALUE;
        for (FeedStatus f : feeds.values()) if (f.getDataTimestamp() > 0) oldest = Math.min(oldest, f.getDataTimestamp());
        return oldest == Long.MAX_VALUE ? 0 : oldest;
    }

    public int count(FeedType type) {
        FeedStatus f = feeds.get(type);
        return f == null ? 0 : f.getCount();
    }
}
