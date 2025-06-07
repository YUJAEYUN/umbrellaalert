package com.example.umbrellaalert.data.database;

import android.content.Context;

/**
 * 앱 데이터베이스 관리 클래스
 * SQLite 데이터베이스와 DAO들을 관리하는 중앙 클래스
 */
public class AppDatabase {
    
    private static AppDatabase instance;
    private final DatabaseHelper databaseHelper;
    private final BusDao busDao;
    private final LocationDao locationDao;
    private final WeatherDao weatherDao;
    
    private AppDatabase(Context context) {
        databaseHelper = DatabaseHelper.getInstance(context);
        busDao = new BusDao(databaseHelper);
        locationDao = new LocationDao(databaseHelper);
        weatherDao = new WeatherDao(databaseHelper);
    }
    
    /**
     * 싱글톤 인스턴스 반환
     */
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new AppDatabase(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * 버스 DAO 반환
     */
    public BusDao busDao() {
        return busDao;
    }
    
    /**
     * 위치 DAO 반환
     */
    public LocationDao locationDao() {
        return locationDao;
    }
    
    /**
     * 날씨 DAO 반환
     */
    public WeatherDao weatherDao() {
        return weatherDao;
    }
    
    /**
     * 데이터베이스 헬퍼 반환
     */
    public DatabaseHelper getDatabaseHelper() {
        return databaseHelper;
    }
}
