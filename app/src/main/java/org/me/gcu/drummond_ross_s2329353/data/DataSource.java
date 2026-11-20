package org.me.gcu.drummond_ross_s2329353.data;

/** Where the data currently shown for a feed came from. */
public enum DataSource {
    LIVE,      // freshly downloaded
    CACHE,     // most recent download, used because the download failed (e.g. offline)
    SAMPLE,    // bundled example file (no API keys and nothing cached yet)
    NONE
}
