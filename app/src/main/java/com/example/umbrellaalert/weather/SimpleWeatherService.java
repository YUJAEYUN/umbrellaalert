package com.example.umbrellaalert.weather;

import android.content.Context;
import android.util.Log;

import com.example.umbrellaalert.data.api.OpenWeatherApiClient;
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.data.model.HourlyForecast;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * OpenWeather API를 사용하는 날씨 서비스
 */
@Singleton
public class SimpleWeatherService {

    private static final String TAG = "SimpleWeatherService";
    private final Context context;
    private final OpenWeatherApiClient apiClient;
    private final ExecutorService executor;

    // 현재 날씨 캐시
    private Weather currentWeather = null;

    @Inject
    public SimpleWeatherService(@ApplicationContext Context context, OpenWeatherApiClient apiClient) {
        this.context = context.getApplicationContext();
        this.apiClient = apiClient;
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * 현재 날씨 가져오기 - OpenWeather API 사용
     */
    public void getCurrentWeather(double latitude, double longitude, WeatherCallback callback) {
        Log.d(TAG, "🌤️ OpenWeather API로 날씨 정보 요청: " + latitude + ", " + longitude);

        executor.execute(() -> {
            try {
                Weather weather = apiClient.getCurrentWeatherSync(latitude, longitude);
                currentWeather = weather;

                Log.d(TAG, "✅ OpenWeather API 날씨 데이터 수신: " + weather.getTemperature() + "°C, " + weather.getWeatherCondition());
                callback.onSuccess(weather);

            } catch (Exception e) {
                Log.e(TAG, "OpenWeather API 날씨 데이터 요청 실패", e);
                callback.onError("날씨 정보를 가져올 수 없습니다: " + e.getMessage());
            }
        });
    }
    
    /**
     * 12시간 예보 가져오기 - OpenWeather API 사용
     */
    public void get12HourForecast(double latitude, double longitude, ForecastCallback callback) {
        Log.d(TAG, "🌤️ OpenWeather API로 12시간 예보 요청: " + latitude + ", " + longitude);

        executor.execute(() -> {
            try {
                List<HourlyForecast> forecasts = apiClient.get12HourForecastSync(latitude, longitude);
                Log.d(TAG, "✅ OpenWeather API 12시간 예보 수신 완료: " + forecasts.size() + "개");
                callback.onSuccess(forecasts);
            } catch (Exception e) {
                Log.e(TAG, "OpenWeather API 예보 데이터 요청 실패", e);
                callback.onError("예보 데이터를 가져올 수 없습니다: " + e.getMessage());
            }
        });
    }
    
    /**
     * 고양이 메시지 생성 - 날씨 상황별 다양한 메시지
     */
    public String getCatMessage(Weather weather) {
        if (weather == null) {
            return "날씨 정보를 가져올 수 없다냥...";
        }

        String condition = weather.getWeatherCondition();
        float temp = weather.getTemperature();

        // 3가지 날씨 상황별 메시지
        if (condition.contains("비")) {
            return "비가 온다냥! ☔ 우산 꼭 챙기고 발 조심하라냥!";
        } else if (condition.contains("흐림")) {
            return "하늘이 흐리다냥! ☁️ 비가 올 수도 있으니 우산 준비하라냥!";
        } else {
            // 맑은 날 - 온도별 메시지
            if (temp > 25) {
                return "따뜻하고 좋은 날씨다냥! ☀️ 산책하기 딱 좋아냥!";
            } else if (temp > 15) {
                return "적당히 시원한 날씨다냥! 😊 외출하기 좋은 날이냥!";
            } else {
                return "조금 쌀쌀하지만 맑은 날이다냥! 🧥 가벼운 옷 챙기라냥!";
            }
        }
    }

    /**
     * 현재 캐시된 날씨 정보 반환
     */
    public Weather getCurrentWeatherCache() {
        return currentWeather;
    }

    /**
     * 리소스 정리
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        if (apiClient != null) {
            apiClient.shutdown();
        }
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
}
