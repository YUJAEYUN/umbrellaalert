package com.example.umbrellaalert.data.manager;

import android.content.Context;
import android.util.Log;

import com.example.umbrellaalert.data.model.Location;
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.data.model.HourlyForecast;
import com.example.umbrellaalert.weather.SimpleWeatherService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * 날씨 매니저 - OpenWeather API 사용
 * Hilt 의존성 주입을 통해 SimpleWeatherService 사용
 */
@Singleton
public class WeatherManager {

    private static final String TAG = "WeatherManager";
    private final Context context;
    private final SimpleWeatherService weatherService;

    @Inject
    public WeatherManager(@ApplicationContext Context context, SimpleWeatherService weatherService) {
        this.context = context.getApplicationContext();
        this.weatherService = weatherService;
    }



    /**
     * 현재 위치의 날씨 가져오기 - OpenWeather API 사용
     */
    public void getCurrentWeather(double latitude, double longitude, WeatherCallback callback) {
        Log.d(TAG, "🌤️ OpenWeather API로 날씨 데이터 요청 시작");

        weatherService.getCurrentWeather(latitude, longitude, new SimpleWeatherService.WeatherCallback() {
            @Override
            public void onSuccess(Weather weather) {
                Log.d(TAG, "✅ OpenWeather API 날씨 데이터 수신: " + weather.getTemperature() + "°C, " + weather.getWeatherCondition());
                callback.onSuccess(weather);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ OpenWeather API 날씨 데이터 요청 실패: " + error);
                callback.onError(error);
            }
        });
    }



    /**
     * 고양이 메시지 생성 - 단순화된 버전
     */
    public String getCatMessage(Weather weather) {
        return weatherService.getCatMessage(weather);
    }

    /**
     * 12시간 예보 가져오기 - OpenWeather API 사용
     */
    public void get12HourForecast(double latitude, double longitude, ForecastCallback callback) {
        Log.d(TAG, "🌤️ OpenWeather API로 12시간 예보 요청 시작");

        weatherService.get12HourForecast(latitude, longitude, new SimpleWeatherService.ForecastCallback() {
            @Override
            public void onSuccess(List<HourlyForecast> forecasts) {
                Log.d(TAG, "✅ OpenWeather API 12시간 예보 수신 완료: " + forecasts.size() + "개");
                callback.onSuccess(forecasts);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ OpenWeather API 예보 데이터 요청 실패: " + error);
                callback.onError(error);
            }
        });
    }

    /**
     * 현재 위치와 등록된 모든 위치에 대해 오늘 하루 비 예보 체크
     */
    public void checkTodayRainForAllLocations(double currentLat, double currentLng, List<Location> locations, WeatherCheckCallback callback) {
        Log.d(TAG, "🌧️ 현재 위치와 등록된 위치들의 오늘 하루 비 예보 체크 시작");

        // 체크할 위치들 수집 (현재 위치 + 활성화된 등록 위치들)
        List<LocationInfo> locationsToCheck = new ArrayList<>();

        // 1. 현재 위치 추가
        locationsToCheck.add(new LocationInfo("현재 위치", currentLat, currentLng));

        // 2. 활성화된 등록 위치들 추가
        if (locations != null) {
            for (Location location : locations) {
                if (location.isNotificationEnabled()) {
                    locationsToCheck.add(new LocationInfo(location.getName(), location.getLatitude(), location.getLongitude()));
                }
            }
        }

        if (locationsToCheck.isEmpty()) {
            Log.d(TAG, "체크할 위치가 없습니다");
            callback.onWeatherCheckCompleted(false);
            return;
        }

        Log.d(TAG, "총 " + locationsToCheck.size() + "개 위치의 오늘 하루 비 예보 체크");

        // 비동기로 각 위치의 오늘 하루 예보 체크
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicBoolean anyLocationHasRain = new AtomicBoolean(false);

        for (LocationInfo locationInfo : locationsToCheck) {
            checkTodayRainForLocation(locationInfo.latitude, locationInfo.longitude, locationInfo.name, new RainCheckCallback() {
                @Override
                public void onRainCheckCompleted(boolean hasRainToday) {
                    Log.d(TAG, "위치 '" + locationInfo.name + "' 오늘 비 예보: " + (hasRainToday ? "있음" : "없음"));

                    if (hasRainToday) {
                        anyLocationHasRain.set(true);
                    }

                    // 모든 위치 체크 완료 시 콜백 호출
                    if (completedCount.incrementAndGet() == locationsToCheck.size()) {
                        boolean finalResult = anyLocationHasRain.get();
                        Log.d(TAG, "🌧️ 전체 위치 비 예보 체크 완료: " + (finalResult ? "비 예상됨" : "비 없음"));
                        callback.onWeatherCheckCompleted(finalResult);
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "위치 '" + locationInfo.name + "' 비 예보 체크 실패: " + error);

                    // 에러가 발생해도 카운트 증가 (다른 위치들은 계속 체크)
                    if (completedCount.incrementAndGet() == locationsToCheck.size()) {
                        callback.onWeatherCheckCompleted(anyLocationHasRain.get());
                    }
                }
            });
        }
    }

    /**
     * 특정 위치의 오늘 하루 비 예보 체크
     */
    private void checkTodayRainForLocation(double latitude, double longitude, String locationName, RainCheckCallback callback) {
        // 12시간 예보를 가져와서 오늘 하루 비 여부 판단
        get12HourForecast(latitude, longitude, new ForecastCallback() {
            @Override
            public void onSuccess(List<HourlyForecast> forecasts) {
                boolean hasRainToday = false;

                // 오늘 날짜 계산
                long todayStart = getTodayStartTime();
                long todayEnd = todayStart + 24 * 60 * 60 * 1000; // 24시간 후

                for (HourlyForecast forecast : forecasts) {
                    long forecastTime = forecast.getTimestamp();

                    // 오늘 범위 내의 예보만 체크
                    if (forecastTime >= todayStart && forecastTime < todayEnd) {
                        if (isRainyWeather(forecast.getWeatherCondition()) || forecast.getPrecipitation() > 0.1f) {
                            hasRainToday = true;
                            Log.d(TAG, locationName + " - 비 예보 발견: " + forecast.getWeatherCondition() +
                                      ", 강수량: " + forecast.getPrecipitation() + "mm");
                            break;
                        }
                    }
                }

                callback.onRainCheckCompleted(hasRainToday);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * 오늘 시작 시간 (00:00) 계산
     */
    private long getTodayStartTime() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * 비가 오는 날씨인지 판단
     */
    private boolean isRainyWeather(String weatherCondition) {
        if (weatherCondition == null) return false;

        String condition = weatherCondition.toLowerCase();
        return condition.contains("rain") || condition.contains("비") ||
               condition.contains("drizzle") || condition.contains("이슬비") ||
               condition.contains("shower") || condition.contains("소나기") ||
               condition.contains("thunderstorm") || condition.contains("천둥") ||
               condition.contains("storm") || condition.contains("폭풍");
    }

    /**
     * 위치 정보를 담는 내부 클래스
     */
    private static class LocationInfo {
        final String name;
        final double latitude;
        final double longitude;

        LocationInfo(String name, double latitude, double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    /**
     * 비 예보 체크 콜백 인터페이스
     */
    public interface RainCheckCallback {
        void onRainCheckCompleted(boolean hasRainToday);
        void onError(String error);
    }

    // === 콜백 인터페이스 ===

    public interface WeatherCallback {
        void onSuccess(Weather weather);
        void onError(String error);
    }

    public interface ForecastCallback {
        void onSuccess(List<HourlyForecast> forecasts);
        void onError(String error);
    }

    public interface WeatherCheckCallback {
        void onWeatherCheckCompleted(boolean anyLocationNeedsUmbrella);
    }
}