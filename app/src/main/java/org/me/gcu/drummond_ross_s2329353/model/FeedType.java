package org.me.gcu.drummond_ross_s2329353.model;

/** The two Traffic Scotland DATEX II publications used by the app. */
public enum FeedType {
    UNPLANNED("Incident", "UnplannedEvents"),
    ROADWORKS("Roadworks", "CurrentRoadworks");   // confirmed against the live API

    private final String label;
    private final String[] publicationNames;

    FeedType(String label, String... publicationNames) {
        this.label = label;
        this.publicationNames = publicationNames;
    }

    public String getLabel() { return label; }

    /** Candidate publication names in the DATEX II REST url (…/publications/&lt;name&gt;/Content.xml). */
    public String[] getPublicationNames() { return publicationNames.clone(); }
}
