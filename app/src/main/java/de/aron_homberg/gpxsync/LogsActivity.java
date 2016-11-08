package de.aron_homberg.gpxsync;

import android.app.LauncherActivity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import de.aron_homberg.gpxsync.db.GpxTrackPool;
import de.aron_homberg.gpxsync.db.LogEntryPool;
import de.aron_homberg.gpxsync.entities.GpxTrack;
import de.aron_homberg.gpxsync.entities.LogEntry;


public class LogsActivity extends AppCompatActivity {

    final String TAG = "LogsActivity";
    protected LogEntryPool logEntryPool;
    protected int trackId;
    String MENU_ITEM_DELETE = "DELETE";
    String[] contextMenuItems = new String[] {
            MENU_ITEM_DELETE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);

        setLogEntryPool(new LogEntryPool(this));
        handleIntents();
        populateLogsGrid();
    }

    public LogEntryPool getLogEntryPool() {
        return logEntryPool;
    }

    public void setLogEntryPool(LogEntryPool logEntryPool) {
        this.logEntryPool = logEntryPool;
    }

    public void populateLogsGrid() {

        ArrayList<LogEntry> logEntriesList = (ArrayList<LogEntry>) getLogEntryPool().getByGpxTrack(trackId);
        List<String> logEntriesNamesList = new ArrayList<>();

        for (LogEntry l : logEntriesList) {
            logEntriesNamesList.add(l.getId() + ": " + l.getMessage());
        }

        String[] logEntriesNamesArray = new String[logEntriesList.size()];
        logEntriesNamesArray = logEntriesNamesList.toArray(logEntriesNamesArray);

        ArrayAdapter<String> logEntriesListAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                logEntriesNamesArray
        );

        ListView logsList = (ListView) findViewById(R.id.logsListView);
        logsList.setAdapter(logEntriesListAdapter);

        registerForContextMenu(logsList);
    }

    protected void handleIntents() {

        trackId = getIntent().getIntExtra("trackId", 0);

        if (getIntent().getBooleanExtra("BACK", false)) {

            long entryId = getIntent().getLongExtra("entryId", 0);

            Log.d(TAG, "Back from: Entry created: " + entryId);

        } else {

            Log.d(TAG, "Log entries of id: " + trackId);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_logs, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add) {

            startAddEntryActivity();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {

        if (v.getId()==R.id.logsListView) {

            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;

            Log.d(TAG, "Menu pos clicked: " + info.position);

            ListView logsList = (ListView) findViewById(R.id.logsListView);
            String logEntryName = (String) logsList.getAdapter().getItem(info.position);

            menu.setHeaderTitle(logEntryName);

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

        ListView logsList = (ListView) findViewById(R.id.logsListView);
        String logEntryName = (String) logsList.getAdapter().getItem(info.position);
        long logEntryId = Long.parseLong(logEntryName.split(":")[0]);

        Log.d(TAG, "Tapped: " + menuItemName + " of " + logEntryName);

        if (menuItemName.equals(MENU_ITEM_DELETE)) {

            getLogEntryPool().delete(logEntryId);

            populateLogsGrid();
        }

        return true;
    }

    protected void startAddEntryActivity() {

        Intent myIntent = new Intent(LogsActivity.this, AddLogEntryActivity.class);
        myIntent.putExtra("trackId", trackId);
        LogsActivity.this.startActivity(myIntent);
    }
}
