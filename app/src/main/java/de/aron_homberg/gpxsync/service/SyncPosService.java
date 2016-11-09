package de.aron_homberg.gpxsync.service;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

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
        handler.post(new Runnable() {
            public void run() {

                Toast.makeText(getApplicationContext(), finalMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void onSuccessLocationUpdate() {

        handler.post(new Runnable() {
            public void run() {

                Toast.makeText(getApplicationContext(), "Updated GeoPos!", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onCreate() {

        Log.d(TAG, "onStartCommand");

        int UPDATE_INTERVAL = 10 * 60 * 1000; // all 10 minutes
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {

                Log.d(TAG, "GeoPosLocation!");

                final SmartLocation.LocationControl lc = SmartLocation.with(SyncPosService.this).location();
                final OnLocationUpdatedListener locListener = new OnLocationUpdatedListener() {

                    @Override
                    public void onLocationUpdated(Location location) {

                    lc.stop();

                    RemoteApiAdapter.callSyncCurrentPosApi(
                            location.getLatitude(),
                            location.getLongitude(),
                            SyncPosService.this
                    );
                    }
                };
                lc.start(locListener);
                lc.config(LocationParams.NAVIGATION);
            }

        }, 0, UPDATE_INTERVAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }
}
