package org.me.gcu.drummond_ross_s2329353.model;

import java.time.OffsetDateTime;
import java.util.List;

/** Output of parsing one feed. */
public class ParseResult {
    private final OffsetDateTime publicationTime;
    private final List<Situation> situations;

    public ParseResult(OffsetDateTime publicationTime, List<Situation> situations) {
        this.publicationTime = publicationTime;
        this.situations = situations;
    }

    public OffsetDateTime getPublicationTime() { return publicationTime; }
    public List<Situation> getSituations() { return situations; }
}
