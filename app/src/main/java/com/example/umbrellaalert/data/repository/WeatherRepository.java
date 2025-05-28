package com.example.umbrellaalert.data.repository;

import android.content.Context;

import com.example.umbrellaalert.data.database.DatabaseHelper;
import com.example.umbrellaalert.data.database.WeatherDao;
import com.example.umbrellaalert.data.api.WeatherApiService;
import com.example.umbrellaalert.data.manager.WeatherManager;
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.util.CoordinateConverter;

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
     * API 타입 설정 (호환성을 위해 유지하지만 실제로는 사용하지 않음)
     */
    public void setApiType(Object apiType) {
        // WeatherManager는 항상 초단기예보를 사용
    }

    /**
     * 현재 위치의 날씨 정보 가져오기
     * WeatherManager를 통해 기상청 API 호출
     */
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
     * 예보 데이터 가져오기 (호환성을 위해 유지하지만 빈 리스트 반환)
     */
    public java.util.List<Object> getUltraSrtForecast(double latitude, double longitude) {
        return new java.util.ArrayList<>();
    }

    /**
     * 예보 데이터 가져오기 (호환성을 위해 유지하지만 빈 리스트 반환)
     */
    public java.util.List<Object> getVilageForecast(double latitude, double longitude) {
        return new java.util.ArrayList<>();
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
