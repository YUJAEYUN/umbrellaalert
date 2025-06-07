package com.example.umbrellaalert.util;

import android.content.Context;
import android.util.Log;

import com.example.umbrellaalert.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 네이버 클라우드 플랫폼 Directions API를 사용한 도보 시간 계산 유틸리티
 */
public class WalkingTimeCalculator {
    
    private static final String TAG = "WalkingTimeCalculator";
    private static final String DIRECTIONS_API_URL = "https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving";
    
    private final Context context;
    private final OkHttpClient httpClient;
    private final ExecutorService executorService;
    private final String clientId;
    private final String clientSecret;
    
    public WalkingTimeCalculator(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient.Builder().build();
        this.executorService = Executors.newCachedThreadPool();
        
        // API 키 설정 (local.properties에서 가져오기)
        this.clientId = BuildConfig.NAVER_MAP_CLIENT_ID;
        this.clientSecret = getClientSecret(); // local.properties에서 가져와야 함
    }
    
    /**
     * 두 지점 간의 도보 시간 계산 (비동기)
     * @param startLat 출발지 위도
     * @param startLng 출발지 경도
     * @param endLat 도착지 위도
     * @param endLng 도착지 경도
     * @return 도보 시간 (분 단위)
     */
    public Future<Integer> calculateWalkingTime(double startLat, double startLng, double endLat, double endLng) {
        return executorService.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                try {
                    // 직선 거리 기반 도보 시간 계산 (네이버 API 대신 간단한 계산 사용)
                    double distance = calculateDistance(startLat, startLng, endLat, endLng);
                    
                    // 평균 도보 속도: 4km/h (분당 약 67m)
                    // 실제 도로를 따라 걸을 때는 직선거리의 약 1.3배 정도
                    double walkingDistance = distance * 1.3;
                    int walkingTimeMinutes = (int) Math.ceil(walkingDistance / 67.0);
                    
                    // 최소 1분, 최대 60분으로 제한
                    walkingTimeMinutes = Math.max(1, Math.min(60, walkingTimeMinutes));
                    
                    Log.d(TAG, String.format("도보 시간 계산: %.0fm -> %d분", walkingDistance, walkingTimeMinutes));
                    return walkingTimeMinutes;
                    
                } catch (Exception e) {
                    Log.e(TAG, "도보 시간 계산 실패", e);
                    // 기본값: 5분
                    return 5;
                }
            }
        });
    }
    
    /**
     * 두 지점 간의 직선 거리 계산 (Haversine 공식)
     * @param lat1 지점1 위도
     * @param lng1 지점1 경도
     * @param lat2 지점2 위도
     * @param lng2 지점2 경도
     * @return 거리 (미터)
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371000; // 지구 반지름 (미터)
        
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLatRad = Math.toRadians(lat2 - lat1);
        double deltaLngRad = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLngRad / 2) * Math.sin(deltaLngRad / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    /**
     * 네이버 클라우드 플랫폼 Directions API 호출 (향후 사용)
     * 현재는 간단한 계산을 사용하지만, 더 정확한 경로가 필요할 때 사용
     */
    private String callDirectionsAPI(double startLat, double startLng, double endLat, double endLng) throws IOException {
        String url = String.format(
            "%s?start=%f,%f&goal=%f,%f&option=trafast",
            DIRECTIONS_API_URL, startLng, startLat, endLng, endLat
        );
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-NCP-APIGW-API-KEY-ID", clientId)
                .addHeader("X-NCP-APIGW-API-KEY", clientSecret)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API 호출 실패: " + response.code());
            }
            
            return response.body().string();
        }
    }
    
    /**
     * Directions API 응답 파싱 (향후 사용)
     */
    private int parseDirectionsResponse(String jsonResponse) throws JSONException {
        JSONObject json = new JSONObject(jsonResponse);
        JSONObject route = json.getJSONObject("route");
        JSONArray trafast = route.getJSONArray("trafast");
        
        if (trafast.length() > 0) {
            JSONObject summary = trafast.getJSONObject(0).getJSONObject("summary");
            int duration = summary.getInt("duration"); // 밀리초
            return duration / (1000 * 60); // 분으로 변환
        }
        
        return 5; // 기본값
    }
    
    /**
     * Client Secret 가져오기 (BuildConfig에서)
     */
    private String getClientSecret() {
        return BuildConfig.NAVER_MAP_CLIENT_SECRET;
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
