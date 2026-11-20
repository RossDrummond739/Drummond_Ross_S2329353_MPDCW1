package org.me.gcu.drummond_ross_s2329353.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.drummond_ross_s2329353.model.FeedType;
import org.me.gcu.drummond_ross_s2329353.model.ParseResult;
import org.me.gcu.drummond_ross_s2329353.model.Situation;
import org.me.gcu.drummond_ross_s2329353.parser.TrafficXmlParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Single source of truth for the traffic data (the Model layer of MVVM).
 *
 * <p>Threading: downloads and parsing run on a fixed thread pool (both feeds in parallel),
 * coordinated by a single-thread executor so only one refresh runs at a time. Results are
 * published to the UI through {@link LiveData#postValue}, so no runOnUiThread / AsyncTask is needed.
 * A {@link ScheduledExecutorService} triggers automatic refreshes at the configured interval.</p>
 */
public class TrafficRepository {
    private static final String TAG = "TrafficRepository";
    private static volatile TrafficRepository instance;

    private final Context appContext;
    private final ApiCredentials credentials;
    private final FeedDownloader downloader;
    private final FeedCache cache;

    private final ExecutorService workers = Executors.newFixedThreadPool(2);
    private final ExecutorService coordinator = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> autoRefreshTask;

    private final MutableLiveData<TrafficState> state = new MutableLiveData<>(TrafficState.initial());
    private final MutableLiveData<Boolean> online = new MutableLiveData<>(true);
    private final MutableLiveData<Long> nextAutoRefresh = new MutableLiveData<>(0L);
    private final AtomicBoolean refreshing = new AtomicBoolean(false);
    private volatile TrafficState latest = TrafficState.initial();
    private volatile long periodMs;

    public static TrafficRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (TrafficRepository.class) {
                if (instance == null) instance = new TrafficRepository(context.getApplicationContext());
            }
        }
        return instance;
    }

    private TrafficRepository(Context context) {
        appContext = context;
        credentials = ApiCredentials.load(context);
        downloader = new FeedDownloader(credentials);
        cache = new FeedCache(context);
        watchConnectivity();
    }

    public LiveData<TrafficState> getState() { return state; }
    public LiveData<Boolean> isOnline() { return online; }
    public LiveData<Long> getNextAutoRefresh() { return nextAutoRefresh; }
    public ApiCredentials getCredentials() { return credentials; }

    /** Starts a background refresh of both feeds; ignored if one is already running. */
    public void refresh() {
        if (!refreshing.compareAndSet(false, true)) return;
        try {
            publish(latest.withLoading(true));
            coordinator.execute(this::refreshBlocking);
        } catch (RuntimeException e) {
            refreshing.set(false);
            throw e;
        }
    }

    private void refreshBlocking() {
        try {
            Map<FeedType, Future<FeedLoad>> futures = new EnumMap<>(FeedType.class);
            for (FeedType type : FeedType.values()) {
                futures.put(type, workers.submit(() -> loadFeed(type)));
            }
            List<Situation> all = new ArrayList<>();
            Map<FeedType, FeedStatus> statuses = new EnumMap<>(FeedType.class);
            for (Map.Entry<FeedType, Future<FeedLoad>> e : futures.entrySet()) {
                FeedLoad load;
                try {
                    load = e.getValue().get(60, TimeUnit.SECONDS);
                } catch (ExecutionException | java.util.concurrent.TimeoutException ex) {
                    load = new FeedLoad(new ArrayList<>(), new FeedStatus(e.getKey(), DataSource.NONE, 0,
                            null, 0, ex.getMessage()));
                }
                all.addAll(load.situations);
                statuses.put(e.getKey(), load.status);
            }
            all.sort(LIST_ORDER);
            publish(new TrafficState(all, statuses, false, System.currentTimeMillis()));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            publish(latest.withLoading(false));
        } finally {
            refreshing.set(false);
        }
    }

    /** Download -> parse -> cache; falls back to the cache (offline) or the bundled sample. Runs on a worker thread. */
    private FeedLoad loadFeed(FeedType type) {
        TrafficXmlParser parser = new TrafficXmlParser();
        String error;
        try {
            String xml = downloader.download(type);
            ParseResult result = parser.parse(xml, type);
            long now = System.currentTimeMillis();
            cache.save(type, xml, now);
            return new FeedLoad(result.getSituations(), new FeedStatus(type, DataSource.LIVE, now,
                    result.getPublicationTime(), result.getSituations().size(), null));
        } catch (Exception e) {
            error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            Log.w(TAG, "Download of " + type + " failed: " + error);
        }

        String cached = cache.load(type);
        if (cached != null) {
            try {
                ParseResult r = parser.parse(cached, type);
                return new FeedLoad(r.getSituations(), new FeedStatus(type, DataSource.CACHE,
                        cache.timestamp(type), r.getPublicationTime(), r.getSituations().size(), error));
            } catch (Exception e) {
                Log.w(TAG, "Cached " + type + " unreadable", e);
            }
        }

        if (!credentials.hasTrafficScotlandKeys()) {
            String asset = type == FeedType.UNPLANNED ? "sample_unplanned.xml" : "sample_roadworks.xml";
            try (InputStream in = appContext.getAssets().open(asset)) {
                String xml = readAll(in);
                ParseResult r = parser.parse(xml, type);
                OffsetDateTime pub = r.getPublicationTime();
                return new FeedLoad(r.getSituations(), new FeedStatus(type, DataSource.SAMPLE,
                        pub == null ? 0 : pub.toInstant().toEpochMilli(), pub, r.getSituations().size(), error));
            } catch (Exception e) {
                Log.w(TAG, "Sample " + asset + " unreadable", e);
            }
        }
        return new FeedLoad(new ArrayList<>(), new FeedStatus(type, DataSource.NONE, 0, null, 0, error));
    }

    private static String readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        return out.toString(StandardCharsets.UTF_8.name());
    }

    /** Incidents first, then by delay severity (worst first), then most recently started. */
    private static final Comparator<Situation> LIST_ORDER = Comparator
            .comparing((Situation s) -> s.getFeedType().ordinal())
            .thenComparing(s -> -s.getDelaySeverity().ordinal())
            .thenComparing(s -> s.getValidity().getStart(),
                    Comparator.nullsLast(Comparator.<OffsetDateTime>reverseOrder()));

    private void publish(TrafficState s) {
        latest = s;
        state.postValue(s);
    }

    // ---------------- automatic updates ----------------

    /**
     * Refreshes now if the data is older than the interval, then every {@code minutes}.
     * Safe to call repeatedly (e.g. on every onStart, or when the interval setting changes).
     */
    public synchronized void startAutoRefresh(int minutes) {
        stopAutoRefresh();
        periodMs = TimeUnit.MINUTES.toMillis(Math.max(1, minutes));
        long last = latest.getLastAttempt();
        long delay = last == 0 ? 0 : Math.max(0, last + periodMs - System.currentTimeMillis());
        autoRefreshTask = scheduler.scheduleWithFixedDelay(() -> {
            // An exception escaping here would silently cancel all future runs, so catch everything.
            try {
                nextAutoRefresh.postValue(System.currentTimeMillis() + periodMs);
                refresh();
            } catch (RuntimeException e) {
                Log.e(TAG, "Automatic refresh failed", e);
            }
        }, delay, periodMs, TimeUnit.MILLISECONDS);
        nextAutoRefresh.postValue(System.currentTimeMillis() + delay);
    }

    public synchronized void stopAutoRefresh() {
        if (autoRefreshTask != null) {
            autoRefreshTask.cancel(false);
            autoRefreshTask = null;
        }
        nextAutoRefresh.postValue(0L);
    }

    // ---------------- connectivity ----------------

    private void watchConnectivity() {
        ConnectivityManager cm = appContext.getSystemService(ConnectivityManager.class);
        if (cm == null) return;
        Network active = cm.getActiveNetwork();
        NetworkCapabilities caps = active == null ? null : cm.getNetworkCapabilities(active);
        online.postValue(caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
        cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                online.postValue(true);
                // Connection is back: if what we show is not live, fetch fresh data straight away.
                if (latest.getLastAttempt() != 0 && !latest.allLive()) refresh();
            }

            @Override
            public void onLost(@NonNull Network network) {
                online.postValue(false);
            }
        });
    }

    private static final class FeedLoad {
        final List<Situation> situations;
        final FeedStatus status;

        FeedLoad(List<Situation> situations, FeedStatus status) {
            this.situations = situations;
            this.status = status;
        }
    }
}
