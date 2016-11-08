package de.aron_homberg.gpxsync.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import de.aron_homberg.gpxsync.entities.LogEntry;


public class LogEntryPool extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 5;

    // Database Name
    private static final String DATABASE_NAME = "logEntry";

    private static final String TABLE_ENTRIES = "entries";

    private static final String KEY_ID = "id";
    private static final String KEY_GPX_TRACK_ID = "gpxTrackId";
    private static final String KEY_NR = "nr";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_MESSAGE_EN = "message_en";
    private static final String KEY_PICTURE = "picture";
    private static final String KEY_ORIGIN = "origin";
    private static final String KEY_TIME = "time";
    private static final String KEY_TYPE = "type";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LNG = "lng";

    public LogEntryPool(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_TRACKS_TABLE = "CREATE TABLE " + TABLE_ENTRIES + "("
                + KEY_ID + " INTEGER PRIMARY KEY," // 0
                + KEY_GPX_TRACK_ID + " INTEGER," // 1
                + KEY_NR + " INTEGER," // 2
                + KEY_MESSAGE + " TEXT," // 3
                + KEY_PICTURE + " BLOB," // 4
                + KEY_ORIGIN + " TEXT," // 5
                + KEY_TIME + " TEXT," // 6
                + KEY_TYPE + " TEXT," // 7
                + KEY_LAT + " DOUBLE," // 8
                + KEY_LNG + " DOUBLE," // 9
                + KEY_MESSAGE_EN + " TEXT" // 10
                + ")";

        db.execSQL(CREATE_TRACKS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ENTRIES);

        // Create tables again
        onCreate(db);
    }

    public long add(LogEntry logEntry) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_GPX_TRACK_ID, logEntry.getGpxTrackId());
        values.put(KEY_NR, logEntry.getNr());
        values.put(KEY_MESSAGE, logEntry.getMessage());
        values.put(KEY_PICTURE, logEntry.getPicture());
        values.put(KEY_ORIGIN, logEntry.getOrigin());
        values.put(KEY_TIME, logEntry.getTime());
        values.put(KEY_TYPE, logEntry.getType());
        values.put(KEY_LAT, logEntry.getLat());
        values.put(KEY_LNG, logEntry.getLng());
        values.put(KEY_MESSAGE_EN, logEntry.getMessage_en());

        long id = db.insert(TABLE_ENTRIES, null, values);

        db.close();

        return id;
    }

    public List<LogEntry> get(String selectQuery) {

        List<LogEntry> logEntriesList = new ArrayList<LogEntry>();

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {

            do {
                LogEntry logEntry = new LogEntry();

                logEntry.setId(Long.parseLong(cursor.getString(0)));
                logEntry.setGpxTrackId(Long.parseLong(cursor.getString(1)));
                logEntry.setNr(Integer.parseInt(cursor.getString(2)));
                logEntry.setMessage(cursor.getString(3));
                logEntry.setPicture(cursor.getBlob(4));
                logEntry.setOrigin(cursor.getString(5));
                logEntry.setTime(cursor.getString(6));
                logEntry.setType(cursor.getString(7));
                logEntry.setLat(cursor.getDouble(8));
                logEntry.setLng(cursor.getDouble(9));
                logEntry.setMessage_en(cursor.getString(10));

                logEntriesList.add(logEntry);

            } while (cursor.moveToNext());
        }
        return logEntriesList;
    }

    public List<LogEntry> getAll() {
        return get("SELECT * FROM " + TABLE_ENTRIES);
    }

    public List<LogEntry> getByGpxTrack(long gpxTrackId) {
        return get("SELECT * FROM " + TABLE_ENTRIES + " WHERE " + KEY_GPX_TRACK_ID + "=" + gpxTrackId);
    }

    public void delete(long id) {

        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ENTRIES, KEY_ID + " = ?",
                new String[] {
                        String.valueOf(id)
                });
        db.close();
    }
}
