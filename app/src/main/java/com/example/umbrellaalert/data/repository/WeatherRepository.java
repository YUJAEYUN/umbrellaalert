package com.example.umbrellaalert.data.repository;

import android.content.Context;

import com.example.umbrellaalert.data.database.DatabaseHelper;
import com.example.umbrellaalert.data.database.WeatherDao;
import com.example.umbrellaalert.data.manager.WeatherManager;
import com.example.umbrellaalert.data.model.Weather;

import java.util.concurrent.TimeUnit;

/**
 * 날씨 데이터 관리를 위한 Repository 클래스
 * 데이터 소스(API, 로컬 DB)를 추상화하여 ViewModel에 제공
 */
public class WeatherRepository {

    private static final long CACHE_EXPIRATION_TIME = TimeUnit.HOURS.toMillis(1); // 1시간
    
    private final Context context;
    private final WeatherManager weatherManager;
    private final WeatherDao weatherDao;
    
    public WeatherRepository(Context context) {
        this.context = context.getApplicationContext();
        this.weatherManager = WeatherManager.getInstance(context);
        this.weatherDao = new WeatherDao(DatabaseHelper.getInstance(context));
    }
    
    /**
     * 현재 위치의 날씨 정보 가져오기
     * 캐시된 데이터가 있으면 사용하고, 없거나 만료되었으면 API에서 새로 가져옴
     */
    public Weather getCurrentWeather(double latitude, double longitude) {
        // 위치 문자열 생성 (위도,경도)
        String locationKey = String.format("%f,%f", latitude, longitude);
        
        // 캐시된 날씨 데이터 확인
        Weather cachedWeather = weatherDao.getLatestWeatherByLocation(locationKey);
        
        // 캐시가 유효한지 확인
        if (isCacheValid(cachedWeather)) {
            return cachedWeather;
        }
        
        // 캐시가 없거나 만료된 경우 API에서 새로 가져옴
        Weather freshWeather = weatherManager.getCurrentWeather(latitude, longitude);
        
        // 새 데이터 캐싱
        if (freshWeather != null) {
            weatherDao.insertWeather(freshWeather);
            
            // 오래된 데이터 정리
            cleanupOldData();
        }
        
        return freshWeather;
    }
    
    /**
     * 캐시된 날씨 데이터가 유효한지 확인
     */
    private boolean isCacheValid(Weather weather) {
        if (weather == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long cacheTime = weather.getTimestamp();
        
        return (currentTime - cacheTime) < CACHE_EXPIRATION_TIME;
    }
    
    /**
     * 오래된 날씨 데이터 정리 (24시간 이상)
     */
    private void cleanupOldData() {
        long threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        weatherDao.deleteOldWeatherData(threshold);
    }
    
    /**
     * 날씨에 따른 고양이 메시지 생성
     */
    public String getCatMessage(Weather weather) {
        return weatherManager.getCatMessage(weather);
    }
}
