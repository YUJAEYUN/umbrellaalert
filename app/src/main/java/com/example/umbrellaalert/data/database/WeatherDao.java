package com.example.umbrellaalert.data.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.umbrellaalert.data.model.Weather;

import java.util.ArrayList;
import java.util.List;

public class WeatherDao {

    private DatabaseHelper dbHelper;

    public WeatherDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // 날씨 데이터 추가
    public long insertWeather(Weather weather) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_TEMPERATURE, weather.getTemperature());
        values.put(DatabaseHelper.COLUMN_WEATHER_CONDITION, weather.getWeatherCondition());
        values.put(DatabaseHelper.COLUMN_PRECIPITATION, weather.getPrecipitation());
        values.put(DatabaseHelper.COLUMN_HUMIDITY, weather.getHumidity());
        values.put(DatabaseHelper.COLUMN_WIND_SPEED, weather.getWindSpeed());
        values.put(DatabaseHelper.COLUMN_LOCATION, weather.getLocation());
        values.put(DatabaseHelper.COLUMN_TIMESTAMP, weather.getTimestamp());
        values.put(DatabaseHelper.COLUMN_NEED_UMBRELLA, weather.isNeedUmbrella() ? 1 : 0);

        long id = db.insert(DatabaseHelper.TABLE_WEATHER, null, values);
        return id;
    }

    // 위치를 통한 가장 최근 날씨 데이터 조회
    public Weather getLatestWeatherByLocation(String location) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT * FROM " + DatabaseHelper.TABLE_WEATHER +
                " WHERE " + DatabaseHelper.COLUMN_LOCATION + " = ?" +
                " ORDER BY " + DatabaseHelper.COLUMN_TIMESTAMP + " DESC LIMIT 1";

        Cursor cursor = db.rawQuery(query, new String[] { location });

        Weather weather = null;
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_WEATHER_ID));
            float temperature = cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.COLUMN_TEMPERATURE));
            String weatherCondition = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_WEATHER_CONDITION));
            float precipitation = cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.COLUMN_PRECIPITATION));
            int humidity = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_HUMIDITY));
            float windSpeed = cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.COLUMN_WIND_SPEED));
            String locationName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LOCATION));
            long timestamp = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIMESTAMP));
            boolean needUmbrella = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_NEED_UMBRELLA)) == 1;

            weather = new Weather(id, temperature, weatherCondition, precipitation, humidity,
                    windSpeed, locationName, timestamp, needUmbrella);
        }

        cursor.close();
        return weather;
    }

    // 오래된 날씨 데이터 삭제 (24시간 이상)
    public int deleteOldWeatherData(long timeThreshold) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        return db.delete(DatabaseHelper.TABLE_WEATHER,
                DatabaseHelper.COLUMN_TIMESTAMP + " < ?",
                new String[] { String.valueOf(timeThreshold) });
    }

    // 모든 날씨 데이터 조회
    public List<Weather> getAllWeatherData() {
        List<Weather> weatherList = new ArrayList<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_WEATHER, null, null, null, null, null,
                DatabaseHelper.COLUMN_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_WEATHER_ID));
                float temperature = cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.COLUMN_TEMPERATURE));
                String weatherCondition = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_WEATHER_CONDITION));
                float precipitation = cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.COLUMN_PRECIPITATION));
                int humidity = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_HUMIDITY));
                float windSpeed = cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.COLUMN_WIND_SPEED));
                String location = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LOCATION));
                long timestamp = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIMESTAMP));
                boolean needUmbrella = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_NEED_UMBRELLA)) == 1;

                Weather weather = new Weather(id, temperature, weatherCondition, precipitation, humidity,
                        windSpeed, location, timestamp, needUmbrella);
                weatherList.add(weather);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return weatherList;
    }
}