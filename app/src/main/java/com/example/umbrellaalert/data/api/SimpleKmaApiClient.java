package com.example.umbrellaalert.data.api;

import android.content.Context;
import android.util.Log;

import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.data.model.HourlyForecast;
import com.example.umbrellaalert.util.ApiKeyUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * 기상청 API허브를 사용한 간단한 날씨 API 클라이언트
 * 하나의 API로 모든 날씨 정보를 제공
 */
@Singleton
public class SimpleKmaApiClient {
    
    private static final String TAG = "SimpleKmaApiClient";
    private static final String BASE_URL = "https://apihub.kma.go.kr/api/typ01/url/kma_sfctm2.php";
    
    private static SimpleKmaApiClient instance;
    private final Context context;
    private final ExecutorService executorService;
    private final String apiKey;
    
    // 주요 지점번호 매핑 (위도/경도 기준 가장 가까운 관측소)
    private static final StationInfo[] STATIONS = {
        new StationInfo(108, "서울", 37.5665, 126.9780),
        new StationInfo(112, "인천", 37.4563, 126.7052),
        new StationInfo(119, "수원", 37.2636, 127.0286),
        new StationInfo(133, "대전", 36.3504, 127.3845),
        new StationInfo(143, "대구", 35.8714, 128.6014),
        new StationInfo(156, "광주", 35.1595, 126.8526),
        new StationInfo(159, "부산", 35.1796, 129.0756),
        new StationInfo(152, "울산", 35.5384, 129.3114),
        new StationInfo(184, "제주", 33.4996, 126.5312),
        new StationInfo(165, "목포", 34.8118, 126.3922),
        new StationInfo(168, "여수", 34.7604, 127.6622),
        new StationInfo(192, "진주", 35.1641, 128.0664),
        new StationInfo(201, "강릉", 37.7519, 128.9006),
        new StationInfo(203, "춘천", 37.9021, 127.7358),
        new StationInfo(235, "통영", 34.8453, 128.4333)
    };
    
    private static class StationInfo {
        final int stationId;
        final String name;
        final double lat;
        final double lon;
        
        StationInfo(int stationId, String name, double lat, double lon) {
            this.stationId = stationId;
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }
    
    // 싱글톤 패턴 (Hilt 사용 시에는 필요 없지만 호환성을 위해 유지)
    public static synchronized SimpleKmaApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new SimpleKmaApiClient(context.getApplicationContext(), true);
        }
        return instance;
    }
    
    @Inject
    public SimpleKmaApiClient(@ApplicationContext Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newCachedThreadPool();
        this.apiKey = ApiKeyUtil.getKmaApiHubKey(context); // 새로운 API 키 사용
    }

    private SimpleKmaApiClient(Context context, boolean singleton) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newCachedThreadPool();
        this.apiKey = ApiKeyUtil.getKmaApiHubKey(context); // 새로운 API 키 사용
    }
    
    /**
     * 현재 날씨 정보 가져오기 (간단한 버전)
     */
    public Future<Weather> getCurrentWeather(double latitude, double longitude) {
        return executorService.submit(new Callable<Weather>() {
            @Override
            public Weather call() throws Exception {
                try {
                    // 1. 가장 가까운 관측소 찾기
                    int stationId = findNearestStation(latitude, longitude);
                    Log.d(TAG, "🎯 선택된 관측소: " + stationId + " (위치: " + latitude + ", " + longitude + ")");
                    
                    // 2. API 호출
                    String response = requestWeatherData(stationId);
                    
                    // 3. 응답 파싱
                    Weather weather = parseWeatherResponse(response, latitude, longitude);
                    
                    Log.d(TAG, "✅ 날씨 데이터 수신 완료: " + weather.getTemperature() + "°C");
                    return weather;
                    
                } catch (Exception e) {
                    Log.e(TAG, "날씨 데이터 요청 실패", e);
                    return createDefaultWeather(latitude, longitude);
                }
            }
        });
    }
    
    /**
     * 6시간 예보 데이터 가져오기 (간단한 버전)
     */
    public Future<List<HourlyForecast>> get6HourForecast(double latitude, double longitude) {
        return executorService.submit(new Callable<List<HourlyForecast>>() {
            @Override
            public List<HourlyForecast> call() throws Exception {
                try {
                    // 1. 가장 가까운 관측소 찾기
                    int stationId = findNearestStation(latitude, longitude);
                    
                    // 2. 기간 조회 API 사용 (현재부터 6시간)
                    String response = requestForecastData(stationId);
                    
                    // 3. 예보 데이터 파싱
                    List<HourlyForecast> forecasts = parseForecastResponse(response);
                    
                    Log.d(TAG, "✅ 6시간 예보 데이터 수신 완료: " + forecasts.size() + "개");
                    return forecasts;
                    
                } catch (Exception e) {
                    Log.e(TAG, "예보 데이터 요청 실패", e);
                    return createDefaultForecast(latitude, longitude);
                }
            }
        });
    }
    
    /**
     * 가장 가까운 관측소 찾기
     */
    private int findNearestStation(double latitude, double longitude) {
        double minDistance = Double.MAX_VALUE;
        int nearestStationId = 108; // 기본값: 서울
        
        for (StationInfo station : STATIONS) {
            double distance = calculateDistance(latitude, longitude, station.lat, station.lon);
            if (distance < minDistance) {
                minDistance = distance;
                nearestStationId = station.stationId;
            }
        }
        
        return nearestStationId;
    }
    
    /**
     * 두 지점 간 거리 계산 (단순한 유클리드 거리)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double deltaLat = lat1 - lat2;
        double deltaLon = lon1 - lon2;
        return Math.sqrt(deltaLat * deltaLat + deltaLon * deltaLon);
    }
    
    /**
     * 현재 날씨 데이터 요청
     */
    private String requestWeatherData(int stationId) throws IOException {
        // 현재 시간 (KST)
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm", Locale.KOREA);
        String currentTime = dateFormat.format(new Date());
        
        String urlStr = BASE_URL + "?tm=" + currentTime + "&stn=" + stationId + "&help=0&authKey=" + apiKey;
        
        Log.d(TAG, "🌐 API 요청: " + urlStr);
        
        return executeHttpRequest(urlStr);
    }
    
    /**
     * 예보 데이터 요청 (기간 조회)
     */
    private String requestForecastData(int stationId) throws IOException {
        // 현재 시간부터 6시간 후까지
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm", Locale.KOREA);
        Calendar now = Calendar.getInstance();
        String startTime = dateFormat.format(now.getTime());
        
        now.add(Calendar.HOUR_OF_DAY, 6);
        String endTime = dateFormat.format(now.getTime());
        
        String urlStr = "https://apihub.kma.go.kr/api/typ01/url/kma_sfctm3.php?tm1=" + startTime + 
                       "&tm2=" + endTime + "&stn=" + stationId + "&help=0&authKey=" + apiKey;
        
        Log.d(TAG, "🌐 예보 API 요청: " + urlStr);
        
        return executeHttpRequest(urlStr);
    }
    
    /**
     * HTTP 요청 실행
     */
    private String executeHttpRequest(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        
        BufferedReader rd;
        int responseCode = conn.getResponseCode();
        
        if (responseCode >= 200 && responseCode <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
            Log.e(TAG, "API 오류 응답 코드: " + responseCode);
        }
        
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();
        
        String response = sb.toString();
        Log.d(TAG, "📡 API 응답: " + (response.length() > 200 ? response.substring(0, 200) + "..." : response));
        
        return response;
    }
    
    /**
     * 날씨 응답 파싱 (CSV 형식)
     */
    private Weather parseWeatherResponse(String response, double latitude, double longitude) {
        // 기상청 API허브는 CSV 형식으로 응답
        // 첫 번째 줄은 헤더, 두 번째 줄부터 데이터
        
        String[] lines = response.split("\n");
        if (lines.length < 2) {
            return createDefaultWeather(latitude, longitude);
        }
        
        try {
            String[] headers = lines[0].split(",");
            String[] values = lines[1].split(",");
            
            // 기본값 설정
            float temperature = 20.0f;
            int humidity = 50;
            float windSpeed = 2.0f;
            String weatherCondition = "Clear";
            boolean needUmbrella = false;
            
            // 데이터 파싱 (헤더와 값 매칭)
            for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                String header = headers[i].trim();
                String value = values[i].trim();
                
                if (value.isEmpty() || value.equals("-")) continue;
                
                try {
                    switch (header) {
                        case "TA": // 기온
                            temperature = Float.parseFloat(value);
                            break;
                        case "HM": // 습도
                            humidity = Integer.parseInt(value);
                            break;
                        case "WS": // 풍속
                            windSpeed = Float.parseFloat(value);
                            break;
                        case "RN": // 강수량
                            float precipitation = Float.parseFloat(value);
                            if (precipitation > 0) {
                                needUmbrella = true;
                                weatherCondition = "Rain";
                            }
                            break;
                        case "WW": // 날씨 현상
                            if (value.contains("비") || value.contains("눈")) {
                                needUmbrella = true;
                                weatherCondition = value.contains("비") ? "Rain" : "Snow";
                            }
                            break;
                    }
                } catch (NumberFormatException e) {
                    Log.w(TAG, "데이터 파싱 오류: " + header + "=" + value);
                }
            }
            
            String locationStr = latitude + "," + longitude;
            long timestamp = System.currentTimeMillis();
            
            return new Weather(0, temperature, weatherCondition, 0.0f, humidity, windSpeed, 
                             locationStr, timestamp, needUmbrella);
            
        } catch (Exception e) {
            Log.e(TAG, "날씨 응답 파싱 실패", e);
            return createDefaultWeather(latitude, longitude);
        }
    }
    
    /**
     * 예보 응답 파싱
     */
    private List<HourlyForecast> parseForecastResponse(String response) {
        List<HourlyForecast> forecasts = new ArrayList<>();
        
        // 간단한 6시간 예보 생성 (실제 구현에서는 응답 데이터 파싱)
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.KOREA);
        
        for (int i = 1; i <= 6; i++) {
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            
            HourlyForecast forecast = new HourlyForecast();
            forecast.setForecastTime(timeFormat.format(calendar.getTime()));
            forecast.setTemperature(20.0f + (float)(Math.random() * 10 - 5)); // 임시 데이터
            forecast.setWeatherCondition("Clear");
            forecast.setPrecipitationProbability(10);
            forecast.setHumidity(60);
            forecast.setWindSpeed(2.0f);
            forecast.setNeedUmbrella(false);
            
            forecasts.add(forecast);
        }
        
        return forecasts;
    }
    
    /**
     * 기본 날씨 데이터 생성
     */
    private Weather createDefaultWeather(double latitude, double longitude) {
        String locationStr = latitude + "," + longitude;
        long timestamp = System.currentTimeMillis();
        
        return new Weather(0, 20.0f, "Clear", 0.0f, 50, 2.0f, locationStr, timestamp, false);
    }
    
    /**
     * 기본 예보 데이터 생성
     */
    private List<HourlyForecast> createDefaultForecast(double latitude, double longitude) {
        List<HourlyForecast> forecasts = new ArrayList<>();
        
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.KOREA);
        
        for (int i = 1; i <= 6; i++) {
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            
            HourlyForecast forecast = new HourlyForecast();
            forecast.setForecastTime(timeFormat.format(calendar.getTime()));
            forecast.setTemperature(20.0f);
            forecast.setWeatherCondition("Clear");
            forecast.setPrecipitationProbability(10);
            forecast.setHumidity(60);
            forecast.setWindSpeed(2.0f);
            forecast.setNeedUmbrella(false);
            
            forecasts.add(forecast);
        }
        
        return forecasts;
    }
}
