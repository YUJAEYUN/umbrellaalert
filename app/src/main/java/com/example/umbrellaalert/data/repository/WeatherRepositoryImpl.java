package com.example.umbrellaalert.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.umbrellaalert.data.database.DatabaseHelper;
import com.example.umbrellaalert.data.database.WeatherDao;
import com.example.umbrellaalert.data.manager.WeatherManager;
import com.example.umbrellaalert.data.model.KmaForecast;
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
     * 현재 위치의 날씨 정보 가져오기
     * WeatherManager를 통해 기상청 API 호출
     */
    @Override
    public Weather getCurrentWeather(double latitude, double longitude) {
        String locationKey = String.format("%f,%f", latitude, longitude);

        // 1. 먼저 SharedPreferences에서 최신 데이터 확인 (앱 재시작 시에도 유지)
        Weather sharedPrefWeather = getWeatherFromSharedPreferences(locationKey);
        if (sharedPrefWeather != null) {
            long currentTime = System.currentTimeMillis();
            long dataAge = currentTime - sharedPrefWeather.getTimestamp();

            if (dataAge < 30 * 60 * 1000) { // 30분 이내
                Log.d("WeatherRepositoryImpl", "✅ SharedPreferences 캐시 데이터 사용: " + sharedPrefWeather.getTemperature() + "°C (데이터 나이: " + (dataAge / 60000) + "분)");
                return sharedPrefWeather;
            }
        }

        // 2. 데이터베이스에서 최신 데이터 확인
        Weather cachedWeather = weatherDao.getLatestWeatherByLocation(locationKey);

        // 3. 캐시된 데이터가 있고 30분 이내 데이터면 바로 반환 (캐시 시간 연장)
        if (cachedWeather != null) {
            long currentTime = System.currentTimeMillis();
            long dataAge = currentTime - cachedWeather.getTimestamp();

            if (dataAge < 30 * 60 * 1000) { // 30분 이내로 연장
                Log.d("WeatherRepositoryImpl", "✅ DB 캐시된 데이터 사용: " + cachedWeather.getTemperature() + "°C (데이터 나이: " + (dataAge / 60000) + "분)");
                // SharedPreferences에도 저장
                saveWeatherToSharedPreferences(cachedWeather, locationKey);
                return cachedWeather;
            }
        }

        // 3. 캐시가 만료되었거나 없는 경우에만 새 데이터 요청 (동기적으로 처리)
        try {
            Log.d("WeatherRepositoryImpl", "🔄 새로운 날씨 데이터 요청 중...");

            // WeatherManager를 통해 동기적으로 새 데이터 가져오기
            final Weather[] newWeather = new Weather[1];
            final boolean[] requestCompleted = new boolean[1];
            final Object lock = new Object();

            weatherManager.getCurrentWeather(latitude, longitude, new WeatherManager.WeatherCallback() {
                @Override
                public void onSuccess(Weather weather) {
                    synchronized (lock) {
                        newWeather[0] = weather;
                        requestCompleted[0] = true;
                        lock.notify();
                    }
                    Log.d("WeatherRepositoryImpl", "✅ 새 데이터 수신 완료: " + weather.getTemperature() + "°C");
                }

                @Override
                public void onError(String error) {
                    synchronized (lock) {
                        requestCompleted[0] = true;
                        lock.notify();
                    }
                    Log.e("WeatherRepositoryImpl", "날씨 데이터 요청 실패: " + error);
                }
            });

            // 최대 10초 대기
            synchronized (lock) {
                if (!requestCompleted[0]) {
                    lock.wait(10000);
                }
            }

            // 새 데이터를 성공적으로 받았으면 SharedPreferences에 저장하고 반환
            if (newWeather[0] != null) {
                Log.d("WeatherRepositoryImpl", "🎉 새 데이터 반환: " + newWeather[0].getTemperature() + "°C");
                saveWeatherToSharedPreferences(newWeather[0], locationKey);
                return newWeather[0];
            }

        } catch (InterruptedException e) {
            Log.e("WeatherRepositoryImpl", "날씨 데이터 요청 중 인터럽트 발생", e);
        }

        // 4. 새 데이터 요청이 실패한 경우, 캐시된 데이터가 있으면 반환 (만료되었어도)
        if (cachedWeather != null) {
            Log.d("WeatherRepositoryImpl", "📦 만료된 캐시 데이터 사용: " + cachedWeather.getTemperature() + "°C");
            return cachedWeather;
        } else {
            Log.d("WeatherRepositoryImpl", "🔧 기본 데이터 생성");
            return createDefaultWeather(latitude, longitude);
        }
    }

    /**
     * 기본 날씨 객체 생성 (API 호출 실패 시 사용)
     */
    private Weather createDefaultWeather(double latitude, double longitude) {
        String locationKey = String.format("%f,%f", latitude, longitude);
        long timestamp = System.currentTimeMillis();

        return new Weather(
                0,
                20.0f,  // 기본 온도 20도
                "Clear", // 기본 날씨 상태
                0.0f,   // 강수량 없음
                50,     // 습도 50%
                2.0f,   // 풍속 2m/s
                locationKey,
                timestamp,
                false   // 우산 필요 없음
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
     * 날씨 예보 정보 조회
     */
    @Override
    public List<KmaForecast> getWeatherForecast(double latitude, double longitude) {
        // TODO: 예보 API 구현 필요
        return new java.util.ArrayList<>();
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
