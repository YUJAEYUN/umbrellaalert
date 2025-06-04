package com.example.umbrellaalert.data.repository;

import android.content.Context;
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

    private final Context context;
    private final WeatherManager weatherManager;
    private final WeatherDao weatherDao;

    @Inject
    public WeatherRepositoryImpl(Context context) {
        this.context = context.getApplicationContext();
        this.weatherManager = WeatherManager.getInstance(context);
        this.weatherDao = new WeatherDao(DatabaseHelper.getInstance(context));
    }

    /**
     * 현재 위치의 날씨 정보 가져오기
     * WeatherManager를 통해 기상청 API 호출
     */
    @Override
    public Weather getCurrentWeather(double latitude, double longitude) {
        String locationKey = String.format("%f,%f", latitude, longitude);

        // 1. 먼저 데이터베이스에서 최신 데이터 확인
        Weather cachedWeather = weatherDao.getLatestWeatherByLocation(locationKey);

        // 2. 캐시된 데이터가 있고 5분 이내 데이터면 바로 반환
        if (cachedWeather != null) {
            long currentTime = System.currentTimeMillis();
            long dataAge = currentTime - cachedWeather.getTimestamp();

            if (dataAge < 5 * 60 * 1000) { // 5분 이내
                Log.d("WeatherRepositoryImpl", "✅ 캐시된 데이터 사용: " + cachedWeather.getTemperature() + "°C");
                return cachedWeather;
            }
        }

        // 3. 백그라운드에서 새 데이터 요청 (비동기)
        weatherManager.getCurrentWeather(latitude, longitude, new WeatherManager.WeatherCallback() {
            @Override
            public void onSuccess(Weather weather) {
                Log.d("WeatherRepositoryImpl", "🔄 새 데이터 백그라운드 업데이트 완료: " + weather.getTemperature() + "°C");
            }

            @Override
            public void onError(String error) {
                Log.e("WeatherRepositoryImpl", "백그라운드 날씨 업데이트 실패: " + error);
            }
        });

        // 4. 캐시된 데이터가 있으면 반환, 없으면 기본값
        if (cachedWeather != null) {
            Log.d("WeatherRepositoryImpl", "📦 오래된 캐시 데이터 사용: " + cachedWeather.getTemperature() + "°C");
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
}
