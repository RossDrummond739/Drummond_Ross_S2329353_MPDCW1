package org.me.gcu.drummond_ross_s2329353.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

/**
 * Traffic Scotland client id / key and the Maps key. They are read from manifest meta-data,
 * which the secrets-gradle-plugin fills in from local.properties, so they are never in git.
 */
public final class ApiCredentials {
    private static final String NOT_SET = "NOT_SET";

    private final String clientId;
    private final String clientKey;
    private final String mapsKey;

    private ApiCredentials(String clientId, String clientKey, String mapsKey) {
        this.clientId = clientId;
        this.clientKey = clientKey;
        this.mapsKey = mapsKey;
    }

    public static ApiCredentials load(Context context) {
        String id = "", key = "", maps = "";
        try {
            ApplicationInfo info = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle md = info.metaData;
            if (md != null) {
                String pkg = context.getPackageName();
                id = md.getString(pkg + ".TS_CLIENT_ID", "");
                key = md.getString(pkg + ".TS_CLIENT_KEY", "");
                maps = md.getString("com.google.android.geo.API_KEY", "");
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            // cannot happen for our own package
        }
        return new ApiCredentials(id, key, maps);
    }

    private static boolean isSet(String v) { return v != null && !v.trim().isEmpty() && !NOT_SET.equals(v); }

    public boolean hasTrafficScotlandKeys() { return isSet(clientId) && isSet(clientKey); }
    public boolean hasMapsKey() { return isSet(mapsKey); }
    public String getClientId() { return clientId; }
    public String getClientKey() { return clientKey; }
}
