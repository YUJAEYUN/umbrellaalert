package com.example.umbrellaalert.data.repository;

import android.content.Context;

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
        // 동기 방식으로 변환하기 위해 임시 저장소 사용
        final Weather[] result = {null};
        final boolean[] completed = {false};

        weatherManager.getCurrentWeather(latitude, longitude, new WeatherManager.WeatherCallback() {
            @Override
            public void onSuccess(Weather weather) {
                result[0] = weather;
                completed[0] = true;
                synchronized (result) {
                    result.notify();
                }
            }

            @Override
            public void onError(String error) {
                result[0] = createDefaultWeather(latitude, longitude);
                completed[0] = true;
                synchronized (result) {
                    result.notify();
                }
            }
        });

        // 결과를 기다림 (최대 10초)
        synchronized (result) {
            try {
                if (!completed[0]) {
                    result.wait(10000); // 10초 대기
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return result[0] != null ? result[0] : createDefaultWeather(latitude, longitude);
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
