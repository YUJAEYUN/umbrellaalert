package com.example.umbrellaalert.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.umbrellaalert.data.model.Weather;

import java.util.concurrent.TimeUnit;

/**
 * 날씨 데이터 캐시 관리 유틸리티
 * 홈 화면에서 받아온 날씨 데이터를 위젯과 알림에서 재사용
 */
public class WeatherCacheManager {
    
    private static final String TAG = "WeatherCacheManager";
    private static final String PREF_NAME = "weather_cache";
    private static final String KEY_LAST_WEATHER_DATA = "last_weather_data";
    private static final String KEY_LAST_WEATHER_TIMESTAMP = "last_weather_timestamp";
    private static final String KEY_LAST_WEATHER_LOCATION = "last_weather_location";
    
    // 캐시 유효 시간: 30분
    private static final long CACHE_EXPIRATION_TIME = TimeUnit.MINUTES.toMillis(30);
    
    /**
     * 날씨 데이터를 캐시에 저장
     */
    public static void saveWeatherToCache(Context context, Weather weather) {
        if (weather == null) return;
        
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            
            String weatherData = weather.getTemperature() + "|" +
                    weather.getWeatherCondition() + "|" +
                    weather.getPrecipitation() + "|" +
                    weather.getHumidity() + "|" +
                    weather.getWindSpeed() + "|" +
                    weather.isNeedUmbrella();
            
            prefs.edit()
                    .putString(KEY_LAST_WEATHER_DATA, weatherData)
                    .putLong(KEY_LAST_WEATHER_TIMESTAMP, System.currentTimeMillis())
                    .putString(KEY_LAST_WEATHER_LOCATION, weather.getLocation())
                    .apply();
            
            Log.d(TAG, "✅ 날씨 데이터 캐시 저장: " + weather.getTemperature() + "°C, " + weather.getWeatherCondition());
            
        } catch (Exception e) {
            Log.e(TAG, "날씨 데이터 캐시 저장 실패", e);
        }
    }
    
    /**
     * 캐시에서 날씨 데이터 가져오기
     */
    public static Weather getWeatherFromCache(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            
            long timestamp = prefs.getLong(KEY_LAST_WEATHER_TIMESTAMP, 0);
            if (timestamp == 0) {
                Log.d(TAG, "캐시된 날씨 데이터 없음");
                return null;
            }
            
            // 캐시 만료 확인
            long currentTime = System.currentTimeMillis();
            if (currentTime - timestamp > CACHE_EXPIRATION_TIME) {
                Log.d(TAG, "캐시된 날씨 데이터 만료됨 (30분 경과)");
                return null;
            }
            
            String weatherData = prefs.getString(KEY_LAST_WEATHER_DATA, "");
            String location = prefs.getString(KEY_LAST_WEATHER_LOCATION, "");
            
            if (weatherData.isEmpty()) {
                Log.d(TAG, "캐시된 날씨 데이터가 비어있음");
                return null;
            }
            
            // 날씨 데이터 파싱 (온도|상태|강수량|습도|풍속|우산필요여부)
            String[] parts = weatherData.split("\\|");
            if (parts.length >= 6) {
                float temperature = Float.parseFloat(parts[0]);
                String condition = parts[1];
                float precipitation = Float.parseFloat(parts[2]);
                int humidity = Integer.parseInt(parts[3]);
                float windSpeed = Float.parseFloat(parts[4]);
                boolean needUmbrella = Boolean.parseBoolean(parts[5]);
                
                Weather weather = new Weather(0, temperature, condition, precipitation, 
                                            humidity, windSpeed, location, timestamp, needUmbrella);
                
                Log.d(TAG, "✅ 캐시에서 날씨 데이터 로드: " + temperature + "°C, " + condition);
                return weather;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "캐시에서 날씨 데이터 로드 실패", e);
        }
        
        return null;
    }
    
    /**
     * 캐시된 데이터가 유효한지 확인
     */
    public static boolean isCacheValid(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long timestamp = prefs.getLong(KEY_LAST_WEATHER_TIMESTAMP, 0);
        
        if (timestamp == 0) return false;
        
        long currentTime = System.currentTimeMillis();
        boolean isValid = (currentTime - timestamp) <= CACHE_EXPIRATION_TIME;
        
        Log.d(TAG, "캐시 유효성 확인: " + isValid + " (경과시간: " + (currentTime - timestamp) / 1000 / 60 + "분)");
        return isValid;
    }
    
    /**
     * 캐시 데이터 삭제
     */
    public static void clearCache(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_LAST_WEATHER_DATA)
                .remove(KEY_LAST_WEATHER_TIMESTAMP)
                .remove(KEY_LAST_WEATHER_LOCATION)
                .apply();
        
        Log.d(TAG, "날씨 캐시 데이터 삭제됨");
    }
    
    /**
     * 캐시 정보 로그 출력 (디버깅용)
     */
    public static void logCacheInfo(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long timestamp = prefs.getLong(KEY_LAST_WEATHER_TIMESTAMP, 0);
        String location = prefs.getString(KEY_LAST_WEATHER_LOCATION, "");
        String data = prefs.getString(KEY_LAST_WEATHER_DATA, "");
        
        if (timestamp > 0) {
            long ageMinutes = (System.currentTimeMillis() - timestamp) / 1000 / 60;
            Log.d(TAG, "📊 캐시 정보 - 위치: " + location + ", 데이터: " + data + ", 나이: " + ageMinutes + "분");
        } else {
            Log.d(TAG, "📊 캐시 정보 - 캐시된 데이터 없음");
        }
    }
}
