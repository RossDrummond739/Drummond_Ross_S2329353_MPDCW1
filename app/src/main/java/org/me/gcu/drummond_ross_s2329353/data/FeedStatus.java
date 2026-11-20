package org.me.gcu.drummond_ross_s2329353.data;

import org.me.gcu.drummond_ross_s2329353.model.FeedType;

import java.time.OffsetDateTime;

/** Result of the last refresh of one feed. */
public class FeedStatus {
    private final FeedType type;
    private final DataSource source;
    private final long dataTimestamp;           // when the shown data was downloaded
    private final OffsetDateTime publicationTime;
    private final int count;
    private final String error;                 // null when the download worked

    public FeedStatus(FeedType type, DataSource source, long dataTimestamp,
                      OffsetDateTime publicationTime, int count, String error) {
        this.type = type;
        this.source = source;
        this.dataTimestamp = dataTimestamp;
        this.publicationTime = publicationTime;
        this.count = count;
        this.error = error;
    }

    public FeedType getType() { return type; }
    public DataSource getSource() { return source; }
    public long getDataTimestamp() { return dataTimestamp; }
    public OffsetDateTime getPublicationTime() { return publicationTime; }
    public int getCount() { return count; }
    public String getError() { return error; }
}
