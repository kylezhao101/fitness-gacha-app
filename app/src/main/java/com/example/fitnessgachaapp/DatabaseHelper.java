package com.example.fitnessgachaapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "fitnessTrackerDatabase";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    public static final String TABLE_TRACKING = "tracking";

    // Tracking Table Columns
    public static final String KEY_TRACKING_ID = "_id";
    public static final String KEY_TRACKING_DATE = "date";
    public static final String KEY_TRACKING_DISTANCE = "distance";
    public static final String KEY_TRACKING_CALORIES = "calories";
    public static final String KEY_TRACKING_DURATION = "duration";

    // SQL to create table
    private static final String CREATE_TABLE_TRACKING = "CREATE TABLE " +
            TABLE_TRACKING + "(" +
            KEY_TRACKING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            KEY_TRACKING_DATE + " TEXT," +
            KEY_TRACKING_DISTANCE + " REAL," +
            KEY_TRACKING_CALORIES + " REAL," +
            KEY_TRACKING_DURATION + " LONG" +
            ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Creating required tables
        db.execSQL(CREATE_TABLE_TRACKING);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRACKING);
        onCreate(db);
    }

    public void addTrackingRecord(TrackingRecord record) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_TRACKING_DATE, record.getDate());
        values.put(KEY_TRACKING_DISTANCE, record.getDistance());
        values.put(KEY_TRACKING_CALORIES, record.getCalories());
        values.put(KEY_TRACKING_DURATION, record.getDuration());

        // Inserting Row
        db.insert(TABLE_TRACKING, null, values);
        db.close(); // Closing database connection
    }

    public List<TrackingRecord> getAllTrackingRecords() {
        List<TrackingRecord> trackingRecords = new ArrayList<>();

        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_TRACKING;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // Looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String date = cursor.getString(cursor.getColumnIndex(KEY_TRACKING_DATE));
                @SuppressLint("Range") float distance = cursor.getFloat(cursor.getColumnIndex(KEY_TRACKING_DISTANCE));
                @SuppressLint("Range") float calories = cursor.getFloat(cursor.getColumnIndex(KEY_TRACKING_CALORIES));
                @SuppressLint("Range") long duration = cursor.getLong(cursor.getColumnIndex(KEY_TRACKING_DURATION));

                TrackingRecord record = new TrackingRecord(date, distance, calories, duration);
                trackingRecords.add(record);
            } while (cursor.moveToNext());
        }

        // Close cursor and database to free up resources
        cursor.close();
        db.close();

        // Return the list of records
        return trackingRecords;
    }

    public void clearTrackingHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRACKING, null, null);
        db.close();
    }

}