package org.me.gcu.drummond_ross_s2329353.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.me.gcu.drummond_ross_s2329353.model.FeedType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Keeps the most recent successfully downloaded XML of each feed in internal storage,
 * so the app still has data to show when there is no connection.
 */
public class FeedCache {
    private static final String PREFS = "feed_cache";
    private final File dir;
    private final SharedPreferences prefs;

    public FeedCache(Context context) {
        dir = new File(context.getFilesDir(), "feeds");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private File fileFor(FeedType type) { return new File(dir, type.name().toLowerCase() + ".xml"); }

    public void save(FeedType type, String xml, long timestamp) {
        try {
            File tmp = new File(dir, type.name() + ".tmp");
            Files.write(tmp.toPath(), xml.getBytes(StandardCharsets.UTF_8));
            if (!tmp.renameTo(fileFor(type))) throw new IOException("rename failed");
            prefs.edit().putLong(type.name(), timestamp).apply();
        } catch (IOException ignored) {
            // A failed cache write only means less offline data next time.
        }
    }

    /** @return cached XML or null */
    public String load(FeedType type) {
        File f = fileFor(type);
        if (!f.exists()) return null;
        try {
            return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public long timestamp(FeedType type) { return prefs.getLong(type.name(), 0L); }
}
