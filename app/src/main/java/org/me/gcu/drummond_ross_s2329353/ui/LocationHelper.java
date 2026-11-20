package org.me.gcu.drummond_ross_s2329353.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.me.gcu.drummond_ross_s2329353.model.GeoPoint;

/** Gets a single coarse location fix using the platform LocationManager (no extra libraries). */
public final class LocationHelper {
    public interface Callback { void onResult(GeoPoint point); }   // null when unavailable

    private static final long TIMEOUT_MS = 10_000;

    private LocationHelper() { }

    /** Caller must already hold ACCESS_COARSE_LOCATION. */
    @SuppressLint("MissingPermission")
    public static void getCoarseLocation(Context context, Callback callback) {
        LocationManager lm = context.getSystemService(LocationManager.class);
        if (lm == null) { callback.onResult(null); return; }

        Location best = null;
        for (String provider : lm.getProviders(true)) {
            Location l = lm.getLastKnownLocation(provider);
            if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
        }
        if (best != null && System.currentTimeMillis() - best.getTime() < 30 * 60_000L) {
            callback.onResult(new GeoPoint(best.getLatitude(), best.getLongitude()));
            return;
        }
        final Location fallback = best;
        String provider = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ? LocationManager.NETWORK_PROVIDER
                : lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ? LocationManager.GPS_PROVIDER : null;
        if (provider == null) {
            callback.onResult(fallback == null ? null : new GeoPoint(fallback.getLatitude(), fallback.getLongitude()));
            return;
        }
        Handler handler = new Handler(Looper.getMainLooper());
        final boolean[] done = {false};
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (done[0]) return;
                done[0] = true;
                lm.removeUpdates(this);
                callback.onResult(new GeoPoint(location.getLatitude(), location.getLongitude()));
            }
        };
        lm.requestLocationUpdates(provider, 0, 0, listener, Looper.getMainLooper());
        handler.postDelayed(() -> {
            if (done[0]) return;
            done[0] = true;
            lm.removeUpdates(listener);
            callback.onResult(fallback == null ? null : new GeoPoint(fallback.getLatitude(), fallback.getLongitude()));
        }, TIMEOUT_MS);
    }
}
