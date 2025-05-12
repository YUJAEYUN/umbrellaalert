package com.example.umbrellaalert.data.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    // 데이터베이스 정보
    private static final String DATABASE_NAME = "umbrella_alert.db";
    private static final int DATABASE_VERSION = 1;

    // 날씨 테이블
    public static final String TABLE_WEATHER = "weather";
    public static final String COLUMN_WEATHER_ID = "id";
    public static final String COLUMN_TEMPERATURE = "temperature";
    public static final String COLUMN_WEATHER_CONDITION = "weather_condition";
    public static final String COLUMN_PRECIPITATION = "precipitation";
    public static final String COLUMN_HUMIDITY = "humidity";
    public static final String COLUMN_WIND_SPEED = "wind_speed";
    public static final String COLUMN_LOCATION = "location";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_NEED_UMBRELLA = "need_umbrella";

    // 위치 테이블
    public static final String TABLE_LOCATION = "location";
    public static final String COLUMN_LOCATION_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_FREQUENT = "frequent";
    public static final String COLUMN_NOTIFICATION_ENABLED = "notification_enabled";

    // 싱글톤 인스턴스
    private static DatabaseHelper instance;

    // 싱글톤 패턴
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 날씨 테이블 생성
        String CREATE_WEATHER_TABLE = "CREATE TABLE " + TABLE_WEATHER + "("
                + COLUMN_WEATHER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TEMPERATURE + " REAL,"
                + COLUMN_WEATHER_CONDITION + " TEXT,"
                + COLUMN_PRECIPITATION + " REAL,"
                + COLUMN_HUMIDITY + " INTEGER,"
                + COLUMN_WIND_SPEED + " REAL,"
                + COLUMN_LOCATION + " TEXT,"
                + COLUMN_TIMESTAMP + " INTEGER,"
                + COLUMN_NEED_UMBRELLA + " INTEGER"
                + ")";
        db.execSQL(CREATE_WEATHER_TABLE);

        // 위치 테이블 생성
        String CREATE_LOCATION_TABLE = "CREATE TABLE " + TABLE_LOCATION + "("
                + COLUMN_LOCATION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_LATITUDE + " REAL,"
                + COLUMN_LONGITUDE + " REAL,"
                + COLUMN_FREQUENT + " INTEGER,"
                + COLUMN_NOTIFICATION_ENABLED + " INTEGER"
                + ")";
        db.execSQL(CREATE_LOCATION_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 데이터베이스 업그레이드 시 테이블 삭제 후 재생성
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEATHER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATION);
        onCreate(db);
    }
}