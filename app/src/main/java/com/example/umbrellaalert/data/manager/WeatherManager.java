package com.example.umbrellaalert.data.manager;

import android.content.Context;
import android.util.Log;

import com.example.umbrellaalert.data.model.Location;
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.data.model.HourlyForecast;
import com.example.umbrellaalert.weather.SimpleWeatherService;

import java.util.List;

/**
 * 날씨 매니저 - 단순화된 버전
 * 복잡한 로직을 제거하고 SimpleWeatherService를 사용
 */
public class WeatherManager {

    private static final String TAG = "WeatherManager";
    private static WeatherManager instance;
    private final SimpleWeatherService weatherService;

    // 싱글톤 패턴
    public static synchronized WeatherManager getInstance(Context context) {
        if (instance == null) {
            instance = new WeatherManager();
        }
        return instance;
    }

    private WeatherManager() {
        weatherService = SimpleWeatherService.getInstance();
    }

    /**
     * 현재 위치의 날씨 가져오기 - 랜덤 데이터만 사용
     */
    public void getCurrentWeather(double latitude, double longitude, WeatherCallback callback) {
        Log.d(TAG, "🌤️ 랜덤 날씨 데이터 생성 시작");

        // 실제 API 호출 주석처리 - 랜덤 데이터만 사용
        // weatherService.getCurrentWeather(latitude, longitude, new SimpleWeatherService.WeatherCallback() {

        // 즉시 랜덤 날씨 데이터 생성
        try {
            Weather randomWeather = createRandomWeather(latitude, longitude);
            Log.d(TAG, "✅ 랜덤 날씨 데이터 생성: " + randomWeather.getTemperature() + "°C, " + randomWeather.getWeatherCondition());
            callback.onSuccess(randomWeather);
        } catch (Exception e) {
            Log.e(TAG, "❌ 랜덤 날씨 데이터 생성 실패", e);
            Weather defaultWeather = createRandomWeather(latitude, longitude);
            callback.onSuccess(defaultWeather);
        }
    }

    // 랜덤 날씨 데이터 생성
    private Weather createRandomWeather(double latitude, double longitude) {
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
                latitude + "," + longitude,
                System.currentTimeMillis(),
                needUmbrella
        );
    }

    /**
     * 고양이 메시지 생성 - 단순화된 버전
     */
    public String getCatMessage(Weather weather) {
        return weatherService.getCatMessage(weather);
    }

    /**
     * 6시간 예보 가져오기 - 단순화된 버전
     */
    public void get6HourForecast(double latitude, double longitude, ForecastCallback callback) {
        Log.d(TAG, "🌤️ 6시간 예보 데이터 요청 시작");

        weatherService.get6HourForecast(latitude, longitude, new SimpleWeatherService.ForecastCallback() {
            @Override
            public void onSuccess(List<HourlyForecast> forecasts) {
                Log.d(TAG, "✅ 6시간 예보 데이터 가져오기 성공: " + forecasts.size() + "개");
                callback.onSuccess(forecasts);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ 6시간 예보 데이터 가져오기 실패: " + error);
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