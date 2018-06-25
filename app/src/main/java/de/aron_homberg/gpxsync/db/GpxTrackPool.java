package de.aron_homberg.gpxsync.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import de.aron_homberg.gpxsync.entities.GpxTrack;

public class GpxTrackPool extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "gpxSync";

    private static final String TABLE_TRACKS = "tracks";

    private static final String KEY_ID = "id";
    private static final String KEY_STORAGE_PATH = "storage_path";
    private static final String KEY_IS_SYNCHRONIZED = "is_synchronized";
    private static final String KEY_MARKUP = "markup";

    public GpxTrackPool(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_TRACKS_TABLE = "CREATE TABLE " + TABLE_TRACKS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," // 0
                + KEY_STORAGE_PATH + " TEXT," // 1
                + KEY_IS_SYNCHRONIZED + " INTEGER," // 2
                + KEY_MARKUP + " TEXT" // 3
            + ")";

        db.execSQL(CREATE_TRACKS_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRACKS);

        // Create tables again
        onCreate(db);
    }

    public void addGpxTrack(GpxTrack gpxTrack) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_STORAGE_PATH, gpxTrack.getStoragePath());
        values.put(KEY_IS_SYNCHRONIZED, (gpxTrack.isSynchronized() ? 1 : 0));
        values.put(KEY_MARKUP, gpxTrack.getMarkup());

        db.insert(TABLE_TRACKS, null, values);

        db.close();
    }

    public GpxTrack getGpxTrack(int id) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_TRACKS, new String[]{
                        KEY_ID, // 0
                        KEY_STORAGE_PATH, // 1
                        KEY_IS_SYNCHRONIZED, // 2
                        KEY_MARKUP // 3
                }, KEY_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
        }

        // casting fun
        int isSynchronizedInt = Integer.valueOf(cursor.getString(2));
        boolean isSynchronized = false;

        if (isSynchronizedInt == 1) {
            isSynchronized = true;
        }

        return new GpxTrack(
                Integer.parseInt(cursor.getString(0)),
                cursor.getString(1),
                isSynchronized,
                cursor.getString(3)
        );
    }

    public List<GpxTrack> get(String selectQuery) {

        List<GpxTrack> trackList = new ArrayList<GpxTrack>();

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {


            int isSynchronizedInt = Integer.valueOf(cursor.getString(2));
            boolean isSynchronized = false;

            if (isSynchronizedInt == 1) {
                isSynchronized = true;
            }

            do {
                GpxTrack track = new GpxTrack(
                        Integer.parseInt(cursor.getString(0)),
                        cursor.getString(1),
                        isSynchronized,
                        cursor.getString(3)
                );

                // Adding contact to list
                trackList.add(track);

            } while (cursor.moveToNext());
        }

        // return contact list
        return trackList;
    }

    public boolean hasTrack(String storagePath) {

        List<GpxTrack> tracks = get("SELECT * FROM " + TABLE_TRACKS + " WHERE " + KEY_STORAGE_PATH + "=\"" + storagePath + "\"");

        if (tracks.size() > 0) {
            return true;
        }
        return false;
    }

    public List<GpxTrack> getAllGpxTracks() {
        return get("SELECT * FROM " + TABLE_TRACKS);
    }

    public List<GpxTrack> getUnSyncedTracks() {
        return get("SELECT * FROM " + TABLE_TRACKS);
    }

    public List<GpxTrack> getSyncedTracks() {
        return get("SELECT * FROM " + TABLE_TRACKS);
    }

    public int updateGpxTrack(GpxTrack track) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_STORAGE_PATH, track.getStoragePath());
        values.put(KEY_IS_SYNCHRONIZED, track.isSynchronized() ? 0 : 1);
        values.put(KEY_MARKUP, track.getMarkup());

        // updating row
        return db.update(TABLE_TRACKS, values, KEY_ID + " = ?",
                new String[]{String.valueOf(track.getId())});
    }

    public int getGpxTrackCount() {
        String countQuery = "SELECT  * FROM " + TABLE_TRACKS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();

        return cursor.getCount();
    }

    public void deleteGpxTrack(GpxTrack track) {

        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRACKS, KEY_ID + " = ?",
                new String[] {
                        String.valueOf(track.getId())
                });
        db.close();
    }

    public void deleteGpxTrackById(long id) {

        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRACKS, KEY_ID + " = ?",
                new String[] {
                        String.valueOf(id)
                });
        db.close();
    }
}
