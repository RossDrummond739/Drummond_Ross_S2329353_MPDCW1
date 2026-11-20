package org.me.gcu.drummond_ross_s2329353.data;

import org.me.gcu.drummond_ross_s2329353.model.FeedType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Downloads a DATEX II publication into a String, sending the client id and key as the
 * Authorization header (Base64 of "id:key"), as in the coursework starter code.
 * Always called from a background thread.
 */
public class FeedDownloader {
    public static final String BASE_URL = "https://datex2.trafficscotland.org/rest/2.3/publications/";
    private static final int TIMEOUT_MS = 20_000;

    private final ApiCredentials credentials;

    public FeedDownloader(ApiCredentials credentials) {
        this.credentials = credentials;
    }

    /** Remembers which publication name worked, so later refreshes go straight to it. */
    private final java.util.Map<FeedType, String> workingUrl = new java.util.concurrent.ConcurrentHashMap<>();

    public static String urlFor(String publicationName) {
        return BASE_URL + publicationName + "/Content.xml";
    }

    public String download(FeedType type) throws IOException {
        if (!credentials.hasTrafficScotlandKeys()) {
            throw new IOException("Traffic Scotland client ID/key not set in local.properties");
        }
        String known = workingUrl.get(type);
        if (known != null) return fetch(known, type);
        IOException last = null;
        for (String name : type.getPublicationNames()) {
            try {
                String xml = fetch(urlFor(name), type);
                workingUrl.put(type, urlFor(name));
                return xml;
            } catch (NotFound e) {
                last = e;           // try the next candidate name
            }
        }
        throw last != null ? last : new IOException("No publication url for " + type);
    }

    private static final class NotFound extends IOException {
        NotFound(String msg) { super(msg); }
    }

    private String fetch(String url, FeedType type) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestMethod("GET");
            String auth = credentials.getClientId() + ":" + credentials.getClientKey();
            conn.setRequestProperty("Authorization",
                    Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8)));
            conn.setRequestProperty("Accept", "application/xml");

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new NotFound("HTTP 404 for " + type.getLabel());
            }
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server returned HTTP " + code + " for " + type.getLabel());
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                char[] buf = new char[8192];
                int n;
                while ((n = in.read(buf)) != -1) sb.append(buf, 0, n);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }
}
