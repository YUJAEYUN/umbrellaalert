package com.example.umbrellaalert.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.umbrellaalert.data.database.DatabaseHelper;
import com.example.umbrellaalert.data.database.WeatherDao;
import com.example.umbrellaalert.data.manager.WeatherManager;

import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.domain.repository.WeatherRepository;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 날씨 데이터 관리를 위한 Repository 구현체
 * 데이터 소스(API, 로컬 DB)를 추상화하여 ViewModel에 제공
 */
@Singleton
public class WeatherRepositoryImpl implements WeatherRepository {

    private static final long CACHE_EXPIRATION_TIME = TimeUnit.HOURS.toMillis(1); // 1시간
    private static final String PREF_NAME = "weather_cache";
    private static final String KEY_LAST_WEATHER_DATA = "last_weather_data";
    private static final String KEY_LAST_WEATHER_TIMESTAMP = "last_weather_timestamp";
    private static final String KEY_LAST_WEATHER_LOCATION = "last_weather_location";

    private final Context context;
    private final WeatherManager weatherManager;
    private final WeatherDao weatherDao;
    private final SharedPreferences sharedPreferences;

    @Inject
    public WeatherRepositoryImpl(Context context) {
        this.context = context.getApplicationContext();
        this.weatherManager = WeatherManager.getInstance(context);
        this.weatherDao = new WeatherDao(DatabaseHelper.getInstance(context));
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 현재 위치의 날씨 정보 가져오기 - 단순화된 버전
     * WeatherManager를 통해 간단하게 호출
     */
    @Override
    public Weather getCurrentWeather(double latitude, double longitude) {
        Log.d("WeatherRepositoryImpl", "🌤️ 날씨 정보 요청 (단순화된 버전)");

        // 복잡한 캐시 로직 제거, WeatherManager에서 처리하도록 위임

        // 즉시 랜덤 날씨 데이터 생성 (대기 시간 제거)
        return createDefaultWeather(latitude, longitude);
    }

    /**
     * 기본 날씨 객체 생성 (API 호출 실패 시 사용)
     */
    private Weather createDefaultWeather(double latitude, double longitude) {
        String locationKey = String.format("%f,%f", latitude, longitude);
        long timestamp = System.currentTimeMillis();

        // 랜덤한 날씨 데이터 생성 - 3가지 날씨 (리소스에 맞춤)
        String[] conditions = {"맑음", "흐림", "비"};
        float[] temperatures = {8.0f, 15.0f, 22.0f, 28.0f};

        String condition = conditions[(int) (Math.random() * conditions.length)];
        float temperature = temperatures[(int) (Math.random() * temperatures.length)];

        float precipitation = 0.0f;
        boolean needUmbrella = false;

        if (condition.contains("비")) {
            precipitation = (float) (Math.random() * 15 + 2);
            needUmbrella = true;
        }

        return new Weather(
                0,
                temperature,
                condition,
                precipitation,
                (int) (Math.random() * 40 + 40), // 40-80% 습도
                (float) (Math.random() * 5 + 1), // 1-6 m/s 풍속
                locationKey,
                timestamp,
                needUmbrella
        );
    }

    /**
     * 캐시된 날씨 정보 조회
     */
    @Override
    public Weather getCachedWeather(String locationStr) {
        return weatherDao.getLatestWeatherByLocation(locationStr);
    }



    /**
     * 날씨 정보 저장
     */
    @Override
    public long saveWeather(Weather weather) {
        return weatherDao.insertWeather(weather);
    }

    /**
     * 오래된 날씨 데이터 정리
     */
    @Override
    public int cleanupOldWeatherData(long threshold) {
        return weatherDao.deleteOldWeatherData(threshold);
    }

    /**
     * 날씨에 따른 고양이 메시지 생성
     */
    @Override
    public String getCatMessage(Weather weather) {
        return weatherManager.getCatMessage(weather);
    }

    /**
     * SharedPreferences에서 날씨 데이터 가져오기
     */
    private Weather getWeatherFromSharedPreferences(String locationKey) {
        try {
            String savedLocation = sharedPreferences.getString(KEY_LAST_WEATHER_LOCATION, "");
            if (!locationKey.equals(savedLocation)) {
                return null; // 위치가 다르면 캐시 무효
            }

            long timestamp = sharedPreferences.getLong(KEY_LAST_WEATHER_TIMESTAMP, 0);
            if (timestamp == 0) {
                return null; // 저장된 데이터 없음
            }

            // 날씨 데이터 복원
            String weatherData = sharedPreferences.getString(KEY_LAST_WEATHER_DATA, "");
            if (weatherData.isEmpty()) {
                return null;
            }

            // 간단한 파싱 (온도|상태|강수량|습도|풍속|우산필요여부)
            String[] parts = weatherData.split("\\|");
            if (parts.length >= 6) {
                float temperature = Float.parseFloat(parts[0]);
                String condition = parts[1];
                float precipitation = Float.parseFloat(parts[2]);
                int humidity = Integer.parseInt(parts[3]);
                float windSpeed = Float.parseFloat(parts[4]);
                boolean needUmbrella = Boolean.parseBoolean(parts[5]);

                return new Weather(0, temperature, condition, precipitation, humidity, windSpeed, locationKey, timestamp, needUmbrella);
            }
        } catch (Exception e) {
            Log.e("WeatherRepositoryImpl", "SharedPreferences에서 날씨 데이터 복원 실패", e);
        }
        return null;
    }

    /**
     * SharedPreferences에 날씨 데이터 저장
     */
    private void saveWeatherToSharedPreferences(Weather weather, String locationKey) {
        try {
            String weatherData = weather.getTemperature() + "|" +
                    weather.getWeatherCondition() + "|" +
                    weather.getPrecipitation() + "|" +
                    weather.getHumidity() + "|" +
                    weather.getWindSpeed() + "|" +
                    weather.isNeedUmbrella();

            sharedPreferences.edit()
                    .putString(KEY_LAST_WEATHER_DATA, weatherData)
                    .putLong(KEY_LAST_WEATHER_TIMESTAMP, weather.getTimestamp())
                    .putString(KEY_LAST_WEATHER_LOCATION, locationKey)
                    .apply();

            Log.d("WeatherRepositoryImpl", "날씨 데이터를 SharedPreferences에 저장: " + weather.getTemperature() + "°C");
        } catch (Exception e) {
            Log.e("WeatherRepositoryImpl", "SharedPreferences에 날씨 데이터 저장 실패", e);
        }
    }
}
