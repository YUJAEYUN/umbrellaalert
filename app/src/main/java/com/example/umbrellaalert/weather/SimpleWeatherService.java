package com.example.umbrellaalert.weather;

import android.content.Context;
import android.util.Log;

import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.data.model.HourlyForecast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 단순한 날씨 서비스 - 모든 복잡한 로직을 제거하고 핵심 기능만 제공
 */
public class SimpleWeatherService {
    
    private static final String TAG = "SimpleWeatherService";
    private static SimpleWeatherService instance;
    private final ExecutorService executor;
    
    // 랜덤 날씨 데이터 - 3가지 날씨 상황 (리소스에 맞춤)
    private static final String[] WEATHER_CONDITIONS = {
        "맑음", "흐림", "비"
    };

    private static final float[] TEMPERATURE_RANGES = {
        8.0f, 15.0f, 22.0f, 28.0f  // 적당한 온도 범위
    };

    // 캐시 제거 - 앱을 열 때마다 새로운 랜덤 날씨
    private Weather currentWeather = null;

    private SimpleWeatherService() {
        executor = Executors.newSingleThreadExecutor();
    }
    
    public static synchronized SimpleWeatherService getInstance() {
        if (instance == null) {
            instance = new SimpleWeatherService();
        }
        return instance;
    }
    
    /**
     * 현재 날씨 가져오기 - 앱을 열 때마다 랜덤한 날씨 (즉시 반환)
     */
    public void getCurrentWeather(double latitude, double longitude, WeatherCallback callback) {
        Log.d(TAG, "🌤️ 랜덤 날씨 정보 생성: " + latitude + ", " + longitude);

        try {
            // API 호출 주석처리 - 랜덤 날씨 데이터 생성
            // Weather weather = fetchWeatherFromApi(latitude, longitude);

            // 랜덤한 날씨 데이터 즉시 생성 (executor 제거)
            Weather weather = createRandomWeather(latitude, longitude);
            currentWeather = weather;

            Log.d(TAG, "✅ 랜덤 날씨 데이터 생성: " + weather.getTemperature() + "°C, " + weather.getWeatherCondition());
            callback.onSuccess(weather);

        } catch (Exception e) {
            Log.e(TAG, "랜덤 날씨 데이터 생성 실패", e);
            // 에러 발생 시에도 랜덤 데이터 제공
            Weather randomWeather = createRandomWeather(latitude, longitude);
            callback.onSuccess(randomWeather);
        }
    }
    
    /**
     * 6시간 예보 가져오기 - 랜덤한 예보 (즉시 반환)
     */
    public void get6HourForecast(double latitude, double longitude, ForecastCallback callback) {
        Log.d(TAG, "🌤️ 랜덤 6시간 예보 생성: " + latitude + ", " + longitude);

        try {
            // 랜덤한 6시간 예보 즉시 생성 (executor 제거)
            List<HourlyForecast> forecasts = createRandomForecast();
            Log.d(TAG, "✅ 랜덤 6시간 예보 생성 완료: " + forecasts.size() + "개");
            callback.onSuccess(forecasts);
        } catch (Exception e) {
            Log.e(TAG, "랜덤 예보 데이터 생성 실패", e);
            callback.onError("예보 데이터를 가져올 수 없습니다");
        }
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
    
    // === 내부 메서드들 ===
    
    /**
     * 랜덤한 날씨 데이터 생성 - 앱을 열 때마다 다른 날씨
     */
    private Weather createRandomWeather(double latitude, double longitude) {
        // 랜덤한 날씨 상황 선택
        String condition = WEATHER_CONDITIONS[(int) (Math.random() * WEATHER_CONDITIONS.length)];

        // 랜덤한 온도 선택
        float temperature = TEMPERATURE_RANGES[(int) (Math.random() * TEMPERATURE_RANGES.length)];

        // 날씨에 따른 강수량 설정 (3가지 날씨)
        float precipitation = 0.0f;
        boolean needUmbrella = false;

        if (condition.contains("비")) {
            precipitation = (float) (Math.random() * 20 + 5); // 5-25mm
            needUmbrella = true;
        }

        // 랜덤한 습도 (30-90%)
        int humidity = (int) (Math.random() * 60 + 30);

        // 랜덤한 풍속 (0.5-8.0 m/s)
        float windSpeed = (float) (Math.random() * 7.5 + 0.5);

        Weather weather = new Weather(
            0,
            temperature,
            condition,
            precipitation,
            humidity,
            windSpeed,
            latitude + "," + longitude,
            System.currentTimeMillis(),
            needUmbrella
        );

        Log.d(TAG, "🎲 랜덤 날씨 생성: " + temperature + "°C, " + condition +
                   (needUmbrella ? " (우산 필요)" : " (우산 불필요)"));

        return weather;
    }
    
    /**
     * 랜덤한 6시간 예보 생성
     */
    private List<HourlyForecast> createRandomForecast() {
        List<HourlyForecast> forecasts = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.KOREA);

        // 현재 날씨를 기준으로 예보 생성
        float baseTemp = currentWeather != null ? currentWeather.getTemperature() : 20.0f;
        String baseCondition = currentWeather != null ? currentWeather.getWeatherCondition() : "맑음";

        for (int i = 1; i <= 6; i++) {
            calendar.add(Calendar.HOUR_OF_DAY, 1);

            HourlyForecast forecast = new HourlyForecast();
            forecast.setForecastTime(timeFormat.format(calendar.getTime()));

            // 기준 온도에서 ±5도 변화
            float tempChange = (float) (Math.random() * 10 - 5);
            forecast.setTemperature(baseTemp + tempChange);

            // 가끔 날씨 상황 변화 (3가지 날씨만)
            String condition = baseCondition;
            if (Math.random() < 0.3) { // 30% 확률로 날씨 변화
                condition = WEATHER_CONDITIONS[(int) (Math.random() * WEATHER_CONDITIONS.length)];
            }
            forecast.setWeatherCondition(condition);

            // 강수 확률 설정 (3가지 날씨)
            int precipProb = 10;
            float precipitation = 0.0f;
            boolean needUmbrella = false;

            if (condition.contains("비")) {
                precipProb = (int) (Math.random() * 40 + 60); // 60-100%
                precipitation = (float) (Math.random() * 15 + 2);
                needUmbrella = true;
            } else if (condition.contains("흐림")) {
                precipProb = (int) (Math.random() * 30 + 20); // 20-50%
            }

            forecast.setPrecipitationProbability(precipProb);
            forecast.setPrecipitation(precipitation);
            forecast.setHumidity((int) (Math.random() * 40 + 40)); // 40-80%
            forecast.setWindSpeed((float) (Math.random() * 5 + 1)); // 1-6 m/s
            forecast.setNeedUmbrella(needUmbrella);

            if (i == 1) {
                forecast.setCurrentHour(true);
            }

            forecasts.add(forecast);
        }

        return forecasts;
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
