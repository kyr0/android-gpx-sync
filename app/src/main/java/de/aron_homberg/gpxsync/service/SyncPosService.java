package de.aron_homberg.gpxsync.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import de.aron_homberg.gpxsync.App;
import de.aron_homberg.gpxsync.net.RemoteApiAdapter;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationParams;

public class SyncPosService extends Service {

    private Timer timer = new Timer();
    final String TAG = "SyncPosService";
    private Handler handler = new Handler();

    public SyncPosService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void onFailedLocationUpdate(String msg, int errorCode) {

        if (errorCode > -1) {
            msg = msg + "; HTTP code: " + errorCode;
        }

        final String finalMsg = msg;
        handler.post(() -> Toast.makeText(getApplicationContext(), finalMsg, Toast.LENGTH_LONG).show());
    }

    public void onSuccessLocationUpdate() {

        handler.post(() -> Toast.makeText(getApplicationContext(), "Updated GeoPos!", Toast.LENGTH_LONG).show());
    }

    @Override
    public void onCreate() {

        Log.d(TAG, "onStartCommand");

        int UPDATE_INTERVAL = 10 * 60 * 1000; // all 10 minutes
        App app = (App) getApplication();

        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {

                if (app.isAutoUpdatePosition()) {

                    Log.d(TAG, "GeoPosLocation!");

                    final SmartLocation.LocationControl lc = SmartLocation.with(SyncPosService.this).location();
                    final OnLocationUpdatedListener locListener = location -> {
                        lc.stop();
                        RemoteApiAdapter.callSyncCurrentPosApi(
                                location.getLatitude(),
                                location.getLongitude(),
                                SyncPosService.this
                        );
                    };
                    lc.start(locListener);
                    lc.config(LocationParams.NAVIGATION);
                }
            }

        }, 0, UPDATE_INTERVAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }
}
