package com.example.umbrellaalert.data.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    // 데이터베이스 정보
    private static final String DATABASE_NAME = "umbrella_alert.db";
    private static final int DATABASE_VERSION = 3;

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

    // 등록된 버스 테이블
    public static final String TABLE_REGISTERED_BUS = "registered_bus";
    public static final String COLUMN_BUS_ID = "id";
    public static final String COLUMN_NODE_ID = "node_id";
    public static final String COLUMN_NODE_NAME = "node_name";
    public static final String COLUMN_ROUTE_ID = "route_id";
    public static final String COLUMN_ROUTE_NO = "route_no";
    public static final String COLUMN_ROUTE_TYPE = "route_type";
    public static final String COLUMN_DIRECTION_NAME = "direction_name";
    public static final String COLUMN_CITY_CODE = "city_code";
    public static final String COLUMN_BUS_LATITUDE = "latitude";
    public static final String COLUMN_BUS_LONGITUDE = "longitude";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_IS_ACTIVE = "is_active";
    public static final String COLUMN_ALIAS = "alias";

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

        // 등록된 버스 테이블 생성
        String CREATE_REGISTERED_BUS_TABLE = "CREATE TABLE " + TABLE_REGISTERED_BUS + "("
                + COLUMN_BUS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NODE_ID + " TEXT NOT NULL,"
                + COLUMN_NODE_NAME + " TEXT,"
                + COLUMN_ROUTE_ID + " TEXT,"
                + COLUMN_ROUTE_NO + " TEXT,"
                + COLUMN_ROUTE_TYPE + " TEXT,"
                + COLUMN_DIRECTION_NAME + " TEXT,"
                + COLUMN_CITY_CODE + " INTEGER,"
                + COLUMN_BUS_LATITUDE + " REAL DEFAULT 0.0,"
                + COLUMN_BUS_LONGITUDE + " REAL DEFAULT 0.0,"
                + COLUMN_CREATED_AT + " INTEGER,"
                + COLUMN_IS_ACTIVE + " INTEGER DEFAULT 1,"
                + COLUMN_ALIAS + " TEXT"
                + ")";
        db.execSQL(CREATE_REGISTERED_BUS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // 버전 2: 등록된 버스 테이블 추가
            String CREATE_REGISTERED_BUS_TABLE = "CREATE TABLE " + TABLE_REGISTERED_BUS + "("
                    + COLUMN_BUS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_NODE_ID + " TEXT NOT NULL,"
                    + COLUMN_NODE_NAME + " TEXT,"
                    + COLUMN_ROUTE_ID + " TEXT,"
                    + COLUMN_ROUTE_NO + " TEXT,"
                    + COLUMN_ROUTE_TYPE + " TEXT,"
                    + COLUMN_DIRECTION_NAME + " TEXT,"
                    + COLUMN_CITY_CODE + " INTEGER,"
                    + COLUMN_CREATED_AT + " INTEGER,"
                    + COLUMN_IS_ACTIVE + " INTEGER DEFAULT 1,"
                    + COLUMN_ALIAS + " TEXT"
                    + ")";
            db.execSQL(CREATE_REGISTERED_BUS_TABLE);
        }

        if (oldVersion < 3) {
            // 버전 3: 등록된 버스 테이블에 위치 정보 컬럼 추가
            db.execSQL("ALTER TABLE " + TABLE_REGISTERED_BUS + " ADD COLUMN " + COLUMN_BUS_LATITUDE + " REAL DEFAULT 0.0");
            db.execSQL("ALTER TABLE " + TABLE_REGISTERED_BUS + " ADD COLUMN " + COLUMN_BUS_LONGITUDE + " REAL DEFAULT 0.0");
        }
    }
}