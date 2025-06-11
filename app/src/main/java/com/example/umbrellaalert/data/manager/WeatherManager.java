package com.example.umbrellaalert.data.manager;

import android.content.Context;
import android.util.Log;

import com.example.umbrellaalert.data.model.Location;
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.data.model.HourlyForecast;
import com.example.umbrellaalert.weather.SimpleWeatherService;

import java.util.List;

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
     * 우산 알림 서비스를 위한 모든 위치에 대한 날씨 체크
     */
    public void checkAllLocationsWeather(List<Location> locations, WeatherCheckCallback callback) {
        // TODO: 필요시 구현
        Log.d(TAG, "checkAllLocationsWeather - 구현 예정");
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