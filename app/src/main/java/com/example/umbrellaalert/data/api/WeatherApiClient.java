package com.example.umbrellaalert.data.api;

import android.net.Uri;
import android.util.Log;

import com.example.umbrellaalert.data.model.Weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WeatherApiClient {

    private static final String TAG = "WeatherApiClient";
    private static final String API_KEY = "YOUR_OPENWEATHERMAP_API_KEY"; // 실제 API 키로 변경 필요
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    private static final boolean USE_MOCK_DATA = true; // 개발 중에는 true로 설정
    private static WeatherApiClient instance;
    private ExecutorService executorService;

    // 싱글톤 패턴
    public static synchronized WeatherApiClient getInstance() {
        if (instance == null) {
            instance = new WeatherApiClient();
        }
        return instance;
    }

    private WeatherApiClient() {
        executorService = Executors.newCachedThreadPool();
    }

    // 현재 위치의 날씨 데이터 가져오기
    public Future<Weather> getCurrentWeather(double latitude, double longitude) {
        return executorService.submit(new Callable<Weather>() {
            @Override
            public Weather call() throws Exception {
                if (USE_MOCK_DATA) {
                    // 더미 데이터 반환
                    return createMockWeatherData(latitude, longitude);
                }
                try {
                    // API URL 구성
                    Uri.Builder builder = Uri.parse(BASE_URL).buildUpon();
                    builder.appendQueryParameter("lat", String.valueOf(latitude));
                    builder.appendQueryParameter("lon", String.valueOf(longitude));
                    builder.appendQueryParameter("appid", API_KEY);
                    builder.appendQueryParameter("units", "metric"); // 섭씨 온도

                    URL url = new URL(builder.toString());
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    // 응답 읽기
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // JSON 파싱
                    return parseWeatherResponse(response.toString(), latitude, longitude);

                } catch (IOException e) {
                    Log.e(TAG, "Error fetching weather data", e);
                    return createMockWeatherData(latitude, longitude);
                }
            }
            // 더미 날씨 데이터 생성
            private Weather createMockWeatherData(double latitude, double longitude) {
                // 현재 시간 기준으로 랜덤 날씨 생성
                boolean isRainy = Math.random() > 0.7; // 30% 확률로 비
                float temperature = (float)(15 + Math.random() * 15); // 15-30도 랜덤
                String condition = isRainy ? "Rain" : "Clear";
                float precipitation = isRainy ? (float)(Math.random() * 10) : 0;
                int humidity = (int)(50 + Math.random() * 40); // 50-90% 랜덤
                float windSpeed = (float)(Math.random() * 10);

                return new Weather(
                        0,
                        temperature,
                        condition,
                        precipitation,
                        humidity,
                        windSpeed,
                        latitude + "," + longitude,
                        System.currentTimeMillis(),
                        isRainy
                );
            }
        });
    }

    // JSON 응답 파싱
    private Weather parseWeatherResponse(String jsonResponse, double latitude, double longitude) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonResponse);

        // 기본 날씨 정보
        JSONObject main = jsonObject.getJSONObject("main");
        float temperature = (float) main.getDouble("temp");
        int humidity = main.getInt("humidity");

        // 날씨 상태
        JSONArray weatherArray = jsonObject.getJSONArray("weather");
        JSONObject weatherObject = weatherArray.getJSONObject(0);
        String weatherCondition = weatherObject.getString("main");

        // 바람 정보
        JSONObject wind = jsonObject.getJSONObject("wind");
        float windSpeed = (float) wind.getDouble("speed");

        // 강수량 (비가 오지 않으면 0)
        float precipitation = 0;
        if (jsonObject.has("rain")) {
            // 지난 1시간 동안의 강수량
            JSONObject rain = jsonObject.getJSONObject("rain");
            if (rain.has("1h")) {
                precipitation = (float) rain.getDouble("1h");
            }
        }

        // 우산 필요 여부 결정 (강수량, 날씨 상태 기반)
        boolean needUmbrella = determineUmbrellaNeeded(precipitation, weatherCondition);

        // 위치 문자열 생성
        String locationStr = latitude + "," + longitude;

        // 현재 시간
        long timestamp = System.currentTimeMillis();

        return new Weather(0, temperature, weatherCondition, precipitation, humidity,
                windSpeed, locationStr, timestamp, needUmbrella);
    }

    // 우산 필요 여부 결정
    private boolean determineUmbrellaNeeded(float precipitation, String weatherCondition) {
        // 비나 눈이 내리는 경우
        if (precipitation > 0 ||
                weatherCondition.equalsIgnoreCase("Rain") ||
                weatherCondition.equalsIgnoreCase("Drizzle") ||
                weatherCondition.equalsIgnoreCase("Snow")) {
            return true;
        }

        return false;
    }
}