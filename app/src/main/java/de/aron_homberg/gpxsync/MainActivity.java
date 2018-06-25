package de.aron_homberg.gpxsync;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.aron_homberg.gpxsync.db.GpxTrackPool;
import de.aron_homberg.gpxsync.db.LogEntryPool;
import de.aron_homberg.gpxsync.entities.GpxTrack;
import de.aron_homberg.gpxsync.entities.LogEntry;
import de.aron_homberg.gpxsync.net.RemoteApiAdapter;
import de.aron_homberg.gpxsync.service.SyncPosService;
import de.aron_homberg.gpxsync.util.Helper;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;

public class MainActivity extends AppCompatActivity implements OnRequestPermissionsResultCallback {

    protected GpxTrackPool dbh;

    private static final String[] PERMISSIONS = {

        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
        Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static final int PERMISSION_REQUEST_CODE = 1;

    final String TAG = "MainActivity";


    String MENU_ITEM_DELETE = "DELETE";
    String[] contextMenuItems = new String[] {
            MENU_ITEM_DELETE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    protected void onPermissionsGranted() {

        // start the sync service
        startService(new Intent(this, SyncPosService.class));

        setContentView(R.layout.activity_main);
        setDbh(new GpxTrackPool(this));
        handleIntents();
        populateGPXGrid();
        handleSyncButtonTap();
        handleSyncPosButtonTap();
        handleListItemTap();
        handleAutoUpdatePositionSwitch();
    }

    private void handleAutoUpdatePositionSwitch() {

        final App app = (App) getApplication();
        final Switch autoUpdatePositionSwitch = findViewById(R.id.autoUpdatePos);

        autoUpdatePositionSwitch.setChecked(app.isAutoUpdatePosition());

        autoUpdatePositionSwitch.setOnCheckedChangeListener((compoundButton, checked) -> app.setAutoUpdatePosition(checked));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        switch (requestCode) {

            case PERMISSION_REQUEST_CODE: {

                this.getPackageManager();
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permissions granted.
                    onPermissionsGranted();


                } else {
                    onPermissionsDenied(); // not granted
                }
            }
            return;
        }
        onPermissionsDenied(); // not all permissions granted
    }

    protected void onPermissionsDenied() {
        promptAndExt();
    }

    private void promptAndExt() {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setTitle("Permission is necessary!");
        alertDialog.setMessage("Please restart the app and grant the permissions as requested.");

        alertDialog.setPositiveButton("OK", (dialog, which) -> finish());
    }

    protected void triggerLocationUpdates() {

        OnLocationUpdatedListener locListener = location -> RemoteApiAdapter.callSyncCurrentPosApi(
                location.getLatitude(),
                location.getLongitude(),
                MainActivity.this
        );
        SmartLocation.with(MainActivity.this).location().start(locListener);
    }

    public void onFailedLocationUpdate(String msg, int errorCode) {

        if (errorCode > -1) {
            msg = msg + "; HTTP code: " + errorCode;
        }

        final String finalMsg = msg;

        runOnUiThread(() -> Toast.makeText(getApplicationContext(), finalMsg, Toast.LENGTH_LONG).show());
    }

    public void onSuccessLocationUpdate() {


        runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Updated GeoPos!", Toast.LENGTH_LONG).show());
    }

    protected void handleSyncPosButtonTap() {

        final Button syncPosButton = findViewById(R.id.updatePosButton);

        syncPosButton.setOnClickListener(v -> {

            DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:

                        runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Sync. pos...", Toast.LENGTH_LONG).show());

                        triggerLocationUpdates();

                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();

        });
    }


    public GpxTrackPool getDbh() {
        return dbh;
    }

    public void setDbh(GpxTrackPool dbh) {
        this.dbh = dbh;
    }

    public void toast(final String msg) {

        runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
    }

    protected void handleSyncButtonTap() {

        final Button syncButton = findViewById(R.id.syncButton);

        syncButton.setOnClickListener(v -> {

            DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        doSync();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();

        });
    }

    protected void doSync() {

        //toast("Syncing...");

        RemoteApiAdapter.callSyncTracksApi(
                (ArrayList<GpxTrack>) getDbh().getUnSyncedTracks(),
                MainActivity.this
        );
    }

    public void populateGPXGrid() {

        ArrayList<GpxTrack> trackList = (ArrayList<GpxTrack>) getDbh().getUnSyncedTracks();
        List<String> trackNamesList = new ArrayList<>();

        for (GpxTrack t : trackList) {
            trackNamesList.add(t.getId() + ": " + t.getName());
        }

        String[] trackNamesArray = new String[trackList.size()];
        trackNamesArray = trackNamesList.toArray(trackNamesArray);

        ArrayAdapter<String> gpxListAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                trackNamesArray
        );

        ListView gpxList = findViewById(R.id.gpxListView);
        gpxList.setAdapter(gpxListAdapter);

        registerForContextMenu(gpxList);
    }

    protected void handleIntents() {

        Intent receivedIntent = getIntent();
        String receivedAction = receivedIntent.getAction();
        String receivedMimeType = receivedIntent.getType();

        receivedIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // GPX track send via SEND intent
        if (Intent.ACTION_SEND.equals(receivedAction) &&
            receivedMimeType != null) {

            persistTrack();
        }
    }

    protected String getGpxMarkup(Uri gpxUri) {

        String gpxMarkup;
        ParcelFileDescriptor mInputPFD = null;

        try {
            /*
             * Get the content resolver instance for this context, and use it
             * to get a ParcelFileDescriptor for the file.
             */
            mInputPFD = getContentResolver().openFileDescriptor(gpxUri, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "Shared GPX file not found. Permission denied?");
        }

        gpxMarkup = Helper.getFileContents(mInputPFD);

        return gpxMarkup;
    }

    protected void persistTrack() {

        toast("Storing track...");

        Intent shareGpxIntent = getIntent();

        // get OSMAnd+ GPX stream URI
        Uri gpxUri = (Uri) Objects.requireNonNull(shareGpxIntent.getExtras()).get("android.intent.extra.STREAM");
        String gpxUrl = Objects.requireNonNull(gpxUri).getPath();

        if (gpxUrl != null && !getDbh().hasTrack(gpxUrl)) {

            toast("SHARED: " + gpxUrl);

            getDbh().addGpxTrack(
                    new GpxTrack(
                            gpxUrl, // URL
                            false, // not synchronized
                            getGpxMarkup(gpxUri)
                    )
            );
            populateGPXGrid();

        } else {
            toast("OMITTED: Track is already existing!");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_about) {

            toast("(c) 2016 Aron Homberg");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void handleListItemTap() {

        ListView listV = findViewById(R.id.gpxListView);

        listV.setOnItemClickListener((parent, view, position, id) -> {

            String trackName = (String) parent.getItemAtPosition(position);
            int trackId = Integer.parseInt(trackName.split(":")[0]);

            Intent myIntent = new Intent(MainActivity.this, LogsActivity.class);
            myIntent.putExtra("trackId", trackId);
            MainActivity.this.startActivity(myIntent);
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {

        if (v.getId()==R.id.gpxListView) {

            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;

            Log.d(TAG, "Menu pos clicked: " + info.position);

            ListView gpxList = findViewById(R.id.gpxListView);
            String gpxTrackName = (String) gpxList.getAdapter().getItem(info.position);

            menu.setHeaderTitle(gpxTrackName);

            for (int i = 0; i<contextMenuItems.length; i++) {
                menu.add(Menu.NONE, i, i, contextMenuItems[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

        int menuItemIndex = item.getItemId();
        String menuItemName = contextMenuItems[menuItemIndex];

        ListView gpxList = findViewById(R.id.gpxListView);
        String gpxTrackName = (String) gpxList.getAdapter().getItem(info.position);

        long gpxTrackId = Long.parseLong(gpxTrackName.split(":")[0]);

        Log.d(TAG, "Tapped: " + menuItemName + " of " + gpxTrackName);

        if (menuItemName.equals(MENU_ITEM_DELETE)) {

            // delete track
            getDbh().deleteGpxTrackById(gpxTrackId);

            LogEntryPool lePool = new LogEntryPool(MainActivity.this);

            // remove corresponding log entries
            ArrayList<LogEntry> logEntries = (ArrayList<LogEntry>) lePool.getByGpxTrack(gpxTrackId);

            for (LogEntry l : logEntries) {
                lePool.delete(l.getId());
            }

            // refresh list
            populateGPXGrid();
        }

        return true;
    }
}
