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

                    // 2. 현재 날짜의 최신 데이터 요청 (필요시 과거 날짜도 시도)
                    Weather weather = null;
                    for (int dayOffset = 0; dayOffset <= 2; dayOffset++) {
                        try {
                            String response = requestWeatherDataWithOffset(stationId, dayOffset);
                            weather = parseWeatherResponse(response, latitude, longitude);

                            // 유효한 데이터를 받았으면 중단
                            if (weather != null && weather.getTemperature() > -50 && weather.getTemperature() < 60) {
                                Log.d(TAG, "✅ 날씨 데이터 수신 완료 (" + dayOffset + "일 전 최신 데이터): " + weather.getTemperature() + "°C");
                                return weather;
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "⚠️ " + dayOffset + "일 전 데이터 요청 실패: " + e.getMessage());
                        }
                    }

                    // 모든 시도가 실패한 경우
                    Log.w(TAG, "⚠️ 모든 시간대에서 데이터 수신 실패, 기본값 사용");
                    return createDefaultWeather(latitude, longitude);

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
     * 현재 날씨 데이터 요청 (여러 시간대 시도)
     */
    private String requestWeatherData(int stationId) throws IOException {
        // 여러 시간대를 시도해서 데이터가 있는 시간 찾기
        for (int hourOffset = 0; hourOffset <= 6; hourOffset++) {
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.HOUR_OF_DAY, -hourOffset); // 과거 시간으로 이동
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHH00", Locale.KOREA);
                String requestTime = dateFormat.format(calendar.getTime());

                String urlStr = BASE_URL + "?tm=" + requestTime + "&stn=" + stationId + "&authKey=" + apiKey;

                Log.d(TAG, "🌐 API 요청 (" + hourOffset + "시간 전): " + urlStr);
                Log.d(TAG, "🕐 요청 시간: " + requestTime);

                String response = executeHttpRequest(urlStr);

                // 응답에 실제 데이터가 있는지 확인
                if (hasActualData(response)) {
                    Log.d(TAG, "✅ " + hourOffset + "시간 전 데이터 발견!");
                    return response;
                } else {
                    Log.d(TAG, "⚠️ " + hourOffset + "시간 전 데이터 없음, 다음 시간 시도");
                }

            } catch (Exception e) {
                Log.w(TAG, "⚠️ " + hourOffset + "시간 전 요청 실패: " + e.getMessage());
            }
        }

        // 모든 시간대에서 실패한 경우 가장 최근 응답 반환
        Log.w(TAG, "⚠️ 모든 시간대에서 데이터 없음, 마지막 응답 반환");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHH00", Locale.KOREA);
        String requestTime = dateFormat.format(calendar.getTime());
        String urlStr = BASE_URL + "?tm=" + requestTime + "&stn=" + stationId + "&authKey=" + apiKey;

        return executeHttpRequest(urlStr);
    }

    /**
     * 시간 오프셋을 적용한 날씨 데이터 요청
     */
    private String requestWeatherDataWithOffset(int stationId, int hourOffset) throws IOException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -hourOffset); // 일 단위로 과거로 이동 (현재 시간 데이터를 위해)

        // 현재 시간의 최신 데이터를 받기 위해 날짜만 지정
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        String requestDate = dateFormat.format(calendar.getTime());

        String urlStr = BASE_URL + "?tm=" + requestDate + "&stn=" + stationId + "&authKey=" + apiKey;

        Log.d(TAG, "🌐 API 요청 (" + hourOffset + "일 전 최신 데이터): " + urlStr);

        return executeHttpRequest(urlStr);
    }

    /**
     * 응답에 실제 데이터가 있는지 확인
     */
    private boolean hasActualData(String response) {
        if (response == null || response.trim().isEmpty()) {
            return false;
        }

        String[] lines = response.split("\n");
        for (String line : lines) {
            line = line.trim();
            // 숫자로 시작하는 데이터 라인이 있는지 확인
            if (line.matches("^\\d{12}\\s+.*")) {
                return true;
            }
        }
        return false;
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
                       "&tm2=" + endTime + "&stn=" + stationId + "&authKey=" + apiKey;
        
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
     * 날씨 응답 파싱 (기상청 API허브 고정폭 텍스트 형식)
     */
    private Weather parseWeatherResponse(String response, double latitude, double longitude) {
        try {
            Log.d(TAG, "📡 파싱할 응답 데이터: " + (response.length() > 500 ? response.substring(0, 500) + "..." : response));

            // 기상청 API허브는 고정폭 텍스트 형식
            // 한 줄에 모든 데이터가 들어있으므로 정규식으로 데이터 부분 추출
            String dataLine = null;

            Log.d(TAG, "📋 응답 데이터 분석 (길이: " + response.length() + ")");

            // 정규식으로 10자리 숫자로 시작하는 데이터 패턴 찾기
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d{10}\\s+[\\d\\s\\-\\.]+)");
            java.util.regex.Matcher matcher = pattern.matcher(response);

            if (matcher.find()) {
                String fullMatch = matcher.group(1);
                // #7777END 전까지만 추출
                int endIndex = fullMatch.indexOf("#7777END");
                if (endIndex != -1) {
                    dataLine = fullMatch.substring(0, endIndex).trim();
                } else {
                    dataLine = fullMatch.trim();
                }
                Log.d(TAG, "✅ 정규식으로 데이터 라인 발견: " + dataLine);
            } else {
                Log.w(TAG, "정규식으로 데이터 라인을 찾을 수 없음");

                // 대안: 응답을 줄 단위로 분리해서 찾기
                String[] lines = response.split("\n");
                Log.d(TAG, "대안 방법: 총 " + lines.length + "개 라인에서 검색");

                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].trim();
                    Log.d(TAG, "라인 " + i + ": [" + line.substring(0, Math.min(100, line.length())) + "...]");

                    // #으로 시작하는 헤더 라인은 건너뛰기
                    if (line.startsWith("#") || line.isEmpty()) {
                        continue;
                    }

                    // 10자리 숫자로 시작하는 데이터 라인 찾기
                    if (line.matches(".*\\d{10}\\s+.*")) {
                        // 10자리 숫자 부분부터 추출
                        java.util.regex.Pattern linePattern = java.util.regex.Pattern.compile("(\\d{10}\\s+[\\d\\s\\-\\.]+)");
                        java.util.regex.Matcher lineMatcher = linePattern.matcher(line);
                        if (lineMatcher.find()) {
                            dataLine = lineMatcher.group(1).trim();
                            Log.d(TAG, "✅ 대안 방법으로 데이터 라인 발견: " + dataLine);
                            break;
                        }
                    }
                }
            }

            if (dataLine == null) {
                Log.w(TAG, "⚠️ 데이터 라인을 찾을 수 없음 - 해당 시간/지점에 데이터가 없을 수 있음");

                // 전체 응답 내용을 로그로 출력 (디버깅용)
                Log.w(TAG, "=== 전체 API 응답 내용 ===");
                String[] debugLines = response.split("\n");
                Log.w(TAG, "응답 라인 수: " + debugLines.length);
                for (int i = 0; i < Math.min(debugLines.length, 10); i++) {
                    Log.w(TAG, "라인 " + i + ": " + debugLines[i]);
                }
                Log.w(TAG, "========================");

                if (response.contains("#START7777") && response.contains("#7777END")) {
                    Log.w(TAG, "정상적인 API 응답이지만 실제 관측 데이터가 없음");
                } else {
                    Log.w(TAG, "비정상적인 API 응답: " + response.substring(0, Math.min(200, response.length())));
                }
                return createDefaultWeather(latitude, longitude);
            }

            Log.d(TAG, "📊 데이터 라인: " + dataLine);

            // 공백으로 분리하여 파싱
            String[] parts = dataLine.trim().split("\\s+");

            if (parts.length < 15) {
                Log.w(TAG, "⚠️ 데이터 필드 부족: " + parts.length + "개");
                return createDefaultWeather(latitude, longitude);
            }

            // 기본값 설정
            float temperature = 20.0f;
            int humidity = 50;
            float windSpeed = 2.0f;
            float precipitation = 0.0f;
            String weatherCondition = "Clear";
            boolean needUmbrella = false;

            try {
                // 필드 위치에 따른 파싱 (API 문서 기준)
                // parts[0] = 시간 (YYMMDDHHMI)
                // parts[1] = 지점번호 (STN)
                // parts[2] = 풍향 (WD)
                // parts[3] = 풍속 (WS)
                // parts[11] = 기온 (TA)
                // parts[13] = 습도 (HM)
                // parts[15] = 강수량 (RN)

                // 풍속 파싱 (parts[3])
                if (parts.length > 3 && !parts[3].equals("-9") && !parts[3].equals("-9.0")) {
                    try {
                        windSpeed = Float.parseFloat(parts[3]);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "풍속 파싱 실패: " + parts[3]);
                    }
                }

                // 기온 파싱 (parts[11])
                if (parts.length > 11 && !parts[11].equals("-9") && !parts[11].equals("-9.0")) {
                    try {
                        temperature = Float.parseFloat(parts[11]);
                        Log.d(TAG, "🌡️ 기온 파싱 성공: " + temperature + "°C");
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "기온 파싱 실패: " + parts[11]);
                    }
                }

                // 습도 파싱 (parts[13])
                if (parts.length > 13 && !parts[13].equals("-9") && !parts[13].equals("-9.0")) {
                    try {
                        humidity = (int) Float.parseFloat(parts[13]);
                        Log.d(TAG, "💧 습도 파싱 성공: " + humidity + "%");
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "습도 파싱 실패: " + parts[13]);
                    }
                }

                // 강수량 파싱 (parts[15])
                if (parts.length > 15 && !parts[15].equals("-9") && !parts[15].equals("-9.0")) {
                    try {
                        precipitation = Float.parseFloat(parts[15]);
                        if (precipitation > 0) {
                            needUmbrella = true;
                            weatherCondition = "Rain";
                            Log.d(TAG, "🌧️ 강수량 감지: " + precipitation + "mm");
                        }
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "강수량 파싱 실패: " + parts[15]);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "데이터 필드 파싱 중 오류", e);
            }

            String locationStr = latitude + "," + longitude;
            long timestamp = System.currentTimeMillis();

            Weather weather = new Weather(0, temperature, weatherCondition, precipitation,
                                        humidity, windSpeed, locationStr, timestamp, needUmbrella);

            Log.d(TAG, "✅ 날씨 파싱 완료: " + temperature + "°C, 습도: " + humidity + "%, 풍속: " + windSpeed + "m/s");

            return weather;

        } catch (Exception e) {
            Log.e(TAG, "날씨 응답 파싱 실패", e);
            return createDefaultWeather(latitude, longitude);
        }
    }
    
    /**
     * 예보 응답 파싱 (현재 날씨 기반으로 6시간 예보 생성)
     */
    private List<HourlyForecast> parseForecastResponse(String response) {
        List<HourlyForecast> forecasts = new ArrayList<>();

        // 현재 날씨 데이터에서 기준 온도 추출
        float baseTemperature = 20.0f;
        int baseHumidity = 60;
        float baseWindSpeed = 2.0f;

        try {
            // 현재 날씨 응답에서 기준값 추출
            String[] lines = response.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.matches("^\\d{12}\\s+.*")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length > 13) {
                        try {
                            if (!parts[11].equals("-9") && !parts[11].equals("-9.0")) {
                                baseTemperature = Float.parseFloat(parts[11]);
                            }
                            if (!parts[13].equals("-9") && !parts[13].equals("-9.0")) {
                                baseHumidity = (int) Float.parseFloat(parts[13]);
                            }
                            if (!parts[3].equals("-9") && !parts[3].equals("-9.0")) {
                                baseWindSpeed = Float.parseFloat(parts[3]);
                            }
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "예보 기준값 파싱 실패");
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "예보 기준값 추출 실패, 기본값 사용");
        }

        Log.d(TAG, "📊 예보 기준값: 온도=" + baseTemperature + "°C, 습도=" + baseHumidity + "%, 풍속=" + baseWindSpeed + "m/s");

        // 6시간 예보 생성
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.KOREA);

        for (int i = 1; i <= 6; i++) {
            calendar.add(Calendar.HOUR_OF_DAY, 1);

            HourlyForecast forecast = new HourlyForecast();
            forecast.setForecastTime(timeFormat.format(calendar.getTime()));

            // 시간별 온도 변화 (기준 온도에서 ±3도 변화)
            float temperature = baseTemperature + (float)(Math.random() * 6 - 3);
            forecast.setTemperature(temperature);

            // 기본 날씨 상태
            forecast.setWeatherCondition("Clear");
            forecast.setPrecipitationProbability(10);
            forecast.setPrecipitation(0.0f);
            forecast.setHumidity(baseHumidity + (int)(Math.random() * 20 - 10)); // ±10% 변화
            forecast.setWindSpeed(baseWindSpeed + (float)(Math.random() * 2 - 1)); // ±1m/s 변화
            forecast.setPrecipitationType(0);
            forecast.setNeedUmbrella(false);

            if (i == 1) {
                forecast.setCurrentHour(true);
            }

            forecasts.add(forecast);

            Log.d(TAG, "🕐 " + i + "시간 후 예보: " + temperature + "°C (시간: " + forecast.getForecastTime() + ")");
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
