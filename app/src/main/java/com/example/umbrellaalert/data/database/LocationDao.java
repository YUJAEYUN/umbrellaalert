package com.example.umbrellaalert.data.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.umbrellaalert.data.model.Location;

import java.util.ArrayList;
import java.util.List;

public class LocationDao {

    private DatabaseHelper dbHelper;

    public LocationDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // 위치 추가
    public long insertLocation(Location location) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME, location.getName());
        values.put(DatabaseHelper.COLUMN_LATITUDE, location.getLatitude());
        values.put(DatabaseHelper.COLUMN_LONGITUDE, location.getLongitude());
        values.put(DatabaseHelper.COLUMN_FREQUENT, location.isFrequent() ? 1 : 0);
        values.put(DatabaseHelper.COLUMN_NOTIFICATION_ENABLED, location.isNotificationEnabled() ? 1 : 0);

        long id = db.insert(DatabaseHelper.TABLE_LOCATION, null, values);
        return id;
    }

    // 위치 업데이트
    public int updateLocation(Location location) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME, location.getName());
        values.put(DatabaseHelper.COLUMN_LATITUDE, location.getLatitude());
        values.put(DatabaseHelper.COLUMN_LONGITUDE, location.getLongitude());
        values.put(DatabaseHelper.COLUMN_FREQUENT, location.isFrequent() ? 1 : 0);
        values.put(DatabaseHelper.COLUMN_NOTIFICATION_ENABLED, location.isNotificationEnabled() ? 1 : 0);

        return db.update(DatabaseHelper.TABLE_LOCATION, values,
                DatabaseHelper.COLUMN_LOCATION_ID + " = ?",
                new String[] { String.valueOf(location.getId()) });
    }

    // 위치 삭제
    public int deleteLocation(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        return db.delete(DatabaseHelper.TABLE_LOCATION,
                DatabaseHelper.COLUMN_LOCATION_ID + " = ?",
                new String[] { String.valueOf(id) });
    }

    // 모든 위치 조회
    public List<Location> getAllLocations() {
        List<Location> locationList = new ArrayList<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_LOCATION, null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_LOCATION_ID));
                String name = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME));
                double latitude = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LATITUDE));
                double longitude = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LONGITUDE));
                boolean frequent = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_FREQUENT)) == 1;
                boolean notificationEnabled = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_NOTIFICATION_ENABLED)) == 1;

                Location location = new Location(id, name, latitude, longitude, frequent, notificationEnabled);
                locationList.add(location);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return locationList;
    }

    // 자주 가는 위치 조회
    public List<Location> getFrequentLocations() {
        List<Location> locationList = new ArrayList<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COLUMN_FREQUENT + " = ?";
        String[] selectionArgs = { "1" };

        Cursor cursor = db.query(DatabaseHelper.TABLE_LOCATION, null, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_LOCATION_ID));
                String name = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME));
                double latitude = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LATITUDE));
                double longitude = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LONGITUDE));
                boolean frequent = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_FREQUENT)) == 1;
                boolean notificationEnabled = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_NOTIFICATION_ENABLED)) == 1;

                Location location = new Location(id, name, latitude, longitude, frequent, notificationEnabled);
                locationList.add(location);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return locationList;
    }
}