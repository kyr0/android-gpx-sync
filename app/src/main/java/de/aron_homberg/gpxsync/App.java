package de.aron_homberg.gpxsync;

import android.app.Application;
import android.content.SharedPreferences;

public class App extends Application {

    final String PREFS = "GpxSyncApp";
    final String AUTO_UPDATE_PREF = "autoUpdatePosition";

    public boolean isAutoUpdatePosition() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        return prefs.getBoolean(AUTO_UPDATE_PREF, true);
    }

    public void setAutoUpdatePosition(boolean autoUpdatePosition) {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        prefs.edit().putBoolean(AUTO_UPDATE_PREF, autoUpdatePosition).apply();
    }
}
