package com.example.umbrellaalert.data.api;

import android.content.Context;
import android.util.Log;

import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.data.model.HourlyForecast;
import com.example.umbrellaalert.util.ApiKeyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
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
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * OpenWeather API 클라이언트
 * Current Weather API와 5-day forecast API 사용
 */
@Singleton
public class OpenWeatherApiClient {
    
    private static final String TAG = "OpenWeatherApiClient";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5";
    private static final String CURRENT_WEATHER_URL = BASE_URL + "/weather";
    private static final String FORECAST_URL = BASE_URL + "/forecast";
    
    private final Context context;
    private final ExecutorService executorService;
    private final String apiKey;
    
    @Inject
    public OpenWeatherApiClient(@ApplicationContext Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newCachedThreadPool();
        this.apiKey = ApiKeyUtil.getOpenWeatherApiKey(context);
    }
    
    /**
     * 현재 날씨 정보 가져오기 (Future 버전)
     */
    public Future<Weather> getCurrentWeather(double latitude, double longitude) {
        return executorService.submit(() -> getCurrentWeatherSync(latitude, longitude));
    }
    
    /**
     * 현재 날씨 정보 가져오기 (동기 버전)
     */
    public Weather getCurrentWeatherSync(double latitude, double longitude) {
        try {
            String urlStr = CURRENT_WEATHER_URL + 
                           "?lat=" + latitude + 
                           "&lon=" + longitude + 
                           "&appid=" + apiKey + 
                           "&units=metric" + 
                           "&lang=kr";
            
            Log.d(TAG, "🌐 OpenWeather API 요청: " + urlStr);
            
            String response = executeHttpRequest(urlStr);
            Log.d(TAG, "📡 API 응답: " + response);
            
            return parseCurrentWeatherResponse(response, latitude, longitude);
            
        } catch (Exception e) {
            Log.e(TAG, "현재 날씨 데이터 요청 실패", e);
            return createDefaultWeather(latitude, longitude);
        }
    }
    
    /**
     * 12시간 예보 데이터 가져오기 (Future 버전)
     */
    public Future<List<HourlyForecast>> get12HourForecast(double latitude, double longitude) {
        return executorService.submit(() -> get12HourForecastSync(latitude, longitude));
    }
    
    /**
     * 12시간 예보 데이터 가져오기 (동기 버전)
     */
    public List<HourlyForecast> get12HourForecastSync(double latitude, double longitude) {
        try {
            String urlStr = FORECAST_URL + 
                           "?lat=" + latitude + 
                           "&lon=" + longitude + 
                           "&appid=" + apiKey + 
                           "&units=metric" + 
                           "&lang=kr";
            
            Log.d(TAG, "🌐 OpenWeather 예보 API 요청: " + urlStr);
            
            String response = executeHttpRequest(urlStr);
            Log.d(TAG, "📡 예보 API 응답: " + response);
            
            return parseForecastResponse(response);
            
        } catch (Exception e) {
            Log.e(TAG, "예보 데이터 요청 실패", e);
            return createDefaultForecast();
        }
    }
    
    /**
     * HTTP 요청 실행
     */
    private String executeHttpRequest(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "📊 HTTP 응답 코드: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
                );
                
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                return response.toString();
            } else {
                throw new IOException("HTTP 요청 실패: " + responseCode);
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * 현재 날씨 응답 파싱
     */
    private Weather parseCurrentWeatherResponse(String response, double latitude, double longitude) {
        try {
            JSONObject json = new JSONObject(response);
            
            // 온도 정보
            JSONObject main = json.getJSONObject("main");
            float temperature = (float) main.getDouble("temp");
            int humidity = main.getInt("humidity");
            
            // 날씨 상태
            JSONArray weatherArray = json.getJSONArray("weather");
            JSONObject weather = weatherArray.getJSONObject(0);
            String weatherMain = weather.getString("main");
            String weatherDescription = weather.getString("description");
            
            // 바람 정보
            float windSpeed = 0.0f;
            if (json.has("wind")) {
                JSONObject wind = json.getJSONObject("wind");
                windSpeed = (float) wind.getDouble("speed");
            }
            
            // 강수량 정보
            float precipitation = 0.0f;
            if (json.has("rain")) {
                JSONObject rain = json.getJSONObject("rain");
                if (rain.has("1h")) {
                    precipitation = (float) rain.getDouble("1h");
                }
            }
            
            // 날씨 상태를 한국어로 변환
            String koreanWeatherCondition = convertWeatherToKorean(weatherMain, weatherDescription);
            
            // 우산 필요 여부 판단
            boolean needUmbrella = isUmbrellaNeeded(weatherMain, precipitation);
            
            String locationStr = latitude + "," + longitude;
            long timestamp = System.currentTimeMillis();
            
            Weather weatherData = new Weather(0, temperature, koreanWeatherCondition, precipitation,
                                            humidity, windSpeed, locationStr, timestamp, needUmbrella);
            
            Log.d(TAG, "✅ 날씨 파싱 완료: " + temperature + "°C, " + koreanWeatherCondition + 
                      ", 습도: " + humidity + "%, 풍속: " + windSpeed + "m/s");
            
            return weatherData;
            
        } catch (Exception e) {
            Log.e(TAG, "날씨 응답 파싱 실패", e);
            return createDefaultWeather(latitude, longitude);
        }
    }
    
    /**
     * 예보 응답 파싱 (12시간 예보)
     */
    private List<HourlyForecast> parseForecastResponse(String response) {
        List<HourlyForecast> forecasts = new ArrayList<>();
        
        try {
            JSONObject json = new JSONObject(response);
            JSONArray list = json.getJSONArray("list");
            
            // 현재 시간부터 12시간 동안의 예보 (3시간 간격으로 4개)
            int count = Math.min(4, list.length());
            
            for (int i = 0; i < count; i++) {
                JSONObject item = list.getJSONObject(i);
                
                // 시간 정보
                String dtTxt = item.getString("dt_txt");
                
                // 온도 정보
                JSONObject main = item.getJSONObject("main");
                float temperature = (float) main.getDouble("temp");
                int humidity = main.getInt("humidity");
                
                // 날씨 상태
                JSONArray weatherArray = item.getJSONArray("weather");
                JSONObject weather = weatherArray.getJSONObject(0);
                String weatherMain = weather.getString("main");
                String weatherDescription = weather.getString("description");
                
                // 바람 정보
                float windSpeed = 0.0f;
                if (item.has("wind")) {
                    JSONObject wind = item.getJSONObject("wind");
                    windSpeed = (float) wind.getDouble("speed");
                }
                
                // 강수량 정보
                float precipitation = 0.0f;
                if (item.has("rain")) {
                    JSONObject rain = item.getJSONObject("rain");
                    if (rain.has("3h")) {
                        precipitation = (float) rain.getDouble("3h");
                    }
                }
                
                // 날씨 상태를 한국어로 변환
                String koreanWeatherCondition = convertWeatherToKorean(weatherMain, weatherDescription);
                
                // 우산 필요 여부 판단
                boolean needUmbrella = isUmbrellaNeeded(weatherMain, precipitation);
                
                // 시간 포맷 변환 (yyyy-MM-dd HH:mm:ss -> HHmm)
                String timeStr = formatForecastTime(dtTxt);

                HourlyForecast forecast = new HourlyForecast(
                    dtTxt.substring(0, 10).replace("-", ""), // yyyyMMdd
                    timeStr.replace(":", ""), // HHmm 형식
                    temperature,
                    precipitation,
                    needUmbrella ? 80 : 10, // 강수확률
                    humidity,
                    windSpeed,
                    needUmbrella ? 1 : 0, // 강수형태 (0:없음, 1:비)
                    koreanWeatherCondition,
                    needUmbrella
                );
                
                forecasts.add(forecast);
            }
            
            Log.d(TAG, "✅ 예보 파싱 완료: " + forecasts.size() + "개 항목");
            
        } catch (Exception e) {
            Log.e(TAG, "예보 응답 파싱 실패", e);
            return createDefaultForecast();
        }
        
        return forecasts;
    }

    /**
     * 날씨 상태를 한국어로 변환
     */
    private String convertWeatherToKorean(String weatherMain, String description) {
        switch (weatherMain.toLowerCase()) {
            case "clear":
                return "맑음";
            case "clouds":
                return "흐림";
            case "rain":
            case "drizzle":
                return "비";
            case "thunderstorm":
                return "비"; // 뇌우도 비로 분류
            case "snow":
                return "눈";
            case "mist":
            case "fog":
            case "haze":
                return "흐림"; // 안개류는 흐림으로 분류
            default:
                return "흐림"; // 기본값
        }
    }

    /**
     * 우산 필요 여부 판단
     */
    private boolean isUmbrellaNeeded(String weatherMain, float precipitation) {
        return weatherMain.equalsIgnoreCase("rain") ||
               weatherMain.equalsIgnoreCase("drizzle") ||
               weatherMain.equalsIgnoreCase("thunderstorm") ||
               precipitation > 0.1f;
    }

    /**
     * 예보 시간 포맷 변환 (yyyy-MM-dd HH:mm:ss -> HH:mm)
     */
    private String formatForecastTime(String dtTxt) {
        try {
            String[] parts = dtTxt.split(" ");
            if (parts.length >= 2) {
                String timePart = parts[1];
                return timePart.substring(0, 5); // HH:mm
            }
        } catch (Exception e) {
            Log.w(TAG, "시간 포맷 변환 실패: " + dtTxt);
        }
        return "00:00";
    }

    /**
     * 기본 날씨 데이터 생성 (API 실패 시)
     */
    private Weather createDefaultWeather(double latitude, double longitude) {
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

        String locationStr = latitude + "," + longitude;
        long timestamp = System.currentTimeMillis();

        Log.d(TAG, "🎲 기본 날씨 데이터 생성: " + temperature + "°C, " + condition);

        return new Weather(0, temperature, condition, precipitation,
                          (int) (Math.random() * 40 + 40), // 40-80% 습도
                          (float) (Math.random() * 5 + 1), // 1-6 m/s 풍속
                          locationStr, timestamp, needUmbrella);
    }

    /**
     * 기본 예보 데이터 생성 (API 실패 시)
     */
    private List<HourlyForecast> createDefaultForecast() {
        List<HourlyForecast> forecasts = new ArrayList<>();

        String[] conditions = {"맑음", "흐림", "비"};
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.KOREA);

        // 3시간 간격으로 4개의 예보 생성
        for (int i = 0; i < 4; i++) {
            calendar.add(Calendar.HOUR_OF_DAY, 3);

            String condition = conditions[(int) (Math.random() * conditions.length)];
            float temperature = (float) (Math.random() * 20 + 10); // 10-30도
            float precipitation = condition.contains("비") ? (float) (Math.random() * 10 + 1) : 0.0f;
            boolean needUmbrella = condition.contains("비");

            HourlyForecast forecast = new HourlyForecast(
                dateFormat.format(calendar.getTime()),
                timeFormat.format(calendar.getTime()).replace(":", ""), // HHmm 형식
                temperature,
                precipitation,
                needUmbrella ? 80 : 10, // 강수확률
                (int) (Math.random() * 40 + 40), // 40-80% 습도
                (float) (Math.random() * 5 + 1), // 1-6 m/s 풍속
                needUmbrella ? 1 : 0, // 강수형태
                condition,
                needUmbrella
            );

            forecasts.add(forecast);
        }

        Log.d(TAG, "🎲 기본 예보 데이터 생성: " + forecasts.size() + "개 항목");
        return forecasts;
    }

    /**
     * 리소스 정리
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
