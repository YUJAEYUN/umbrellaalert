package com.example.umbrellaalert.data.api;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.umbrellaalert.BuildConfig;
import com.example.umbrellaalert.data.model.WeatherApiResponse;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class WeatherApiService {
    private static final String TAG = "WeatherApiService";
    private static final String BASE_URL = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst";
    
    private RequestQueue requestQueue;
    private Gson gson;
    
    public interface WeatherApiCallback {
        void onSuccess(WeatherApiResponse response);
        void onError(String error);
    }
    
    public WeatherApiService(Context context) {
        requestQueue = Volley.newRequestQueue(context);
        gson = new Gson();
    }
    
    /**
     * 초단기예보 조회
     * @param nx 예보지점 X 좌표
     * @param ny 예보지점 Y 좌표
     * @param callback 결과 콜백
     */
    public void getUltraShortTermForecast(int nx, int ny, WeatherApiCallback callback) {
        // 현재 시간 기준으로 baseDate, baseTime 설정
        Calendar calendar = Calendar.getInstance();
        
        // 초단기예보는 매시간 30분에 생성되므로, 30분 이전이면 이전 시간 데이터를 가져와야 함
        if (calendar.get(Calendar.MINUTE) < 30) {
            calendar.add(Calendar.HOUR_OF_DAY, -1);
        }
        
        String baseDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.getTime());
        String baseTime = String.format(Locale.getDefault(), "%02d30", calendar.get(Calendar.HOUR_OF_DAY));
        
        getUltraShortTermForecast(baseDate, baseTime, nx, ny, callback);
    }
    
    /**
     * 초단기실황 조회
     * @param nx 예보지점 X 좌표
     * @param ny 예보지점 Y 좌표
     * @param callback 결과 콜백
     */
    public void getUltraShortTermObservation(int nx, int ny, WeatherApiCallback callback) {
        // 현재 시간 기준으로 baseDate, baseTime 설정
        Calendar calendar = Calendar.getInstance();
        
        // 초단기실황은 매시간 정시 10분 후에 생성되므로, 10분 이전이면 이전 시간 데이터를 가져와야 함
        if (calendar.get(Calendar.MINUTE) < 10) {
            calendar.add(Calendar.HOUR_OF_DAY, -1);
        }
        
        String baseDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.getTime());
        String baseTime = String.format(Locale.getDefault(), "%02d00", calendar.get(Calendar.HOUR_OF_DAY));
        
        getUltraShortTermObservation(baseDate, baseTime, nx, ny, callback);
    }
    
    /**
     * 초단기예보 조회 (상세 파라미터)
     * @param baseDate 발표일자 (YYYYMMDD)
     * @param baseTime 발표시각 (HHMM)
     * @param nx 예보지점 X 좌표
     * @param ny 예보지점 Y 좌표
     * @param callback 결과 콜백
     */
    public void getUltraShortTermForecast(String baseDate, String baseTime, int nx, int ny, WeatherApiCallback callback) {
        String url = buildForecastUrl(baseDate, baseTime, nx, ny);
        
        Log.d(TAG, "Forecast Request URL: " + url);
        
        StringRequest request = new StringRequest(Request.Method.GET, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "Forecast API Response: " + response);
                    try {
                        WeatherApiResponse weatherResponse = gson.fromJson(response, WeatherApiResponse.class);
                        callback.onSuccess(weatherResponse);
                    } catch (Exception e) {
                        Log.e(TAG, "JSON parsing error", e);
                        callback.onError("응답 데이터 파싱 오류: " + e.getMessage());
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "API Error", error);
                    String errorMessage = "네트워크 오류";
                    if (error.networkResponse != null) {
                        errorMessage += " (코드: " + error.networkResponse.statusCode + ")";
                    }
                    callback.onError(errorMessage);
                }
            });
        
        requestQueue.add(request);
    }
    
    /**
     * 초단기실황 조회 (상세 파라미터)
     * @param baseDate 발표일자 (YYYYMMDD)
     * @param baseTime 발표시각 (HHMM)
     * @param nx 예보지점 X 좌표
     * @param ny 예보지점 Y 좌표
     * @param callback 결과 콜백
     */
    public void getUltraShortTermObservation(String baseDate, String baseTime, int nx, int ny, WeatherApiCallback callback) {
        String url = buildObservationUrl(baseDate, baseTime, nx, ny);
        
        Log.d(TAG, "Observation Request URL: " + url);
        
        StringRequest request = new StringRequest(Request.Method.GET, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "Observation API Response: " + response);
                    try {
                        WeatherApiResponse weatherResponse = gson.fromJson(response, WeatherApiResponse.class);
                        callback.onSuccess(weatherResponse);
                    } catch (Exception e) {
                        Log.e(TAG, "JSON parsing error", e);
                        callback.onError("응답 데이터 파싱 오류: " + e.getMessage());
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "API Error", error);
                    String errorMessage = "네트워크 오류";
                    if (error.networkResponse != null) {
                        errorMessage += " (코드: " + error.networkResponse.statusCode + ")";
                    }
                    callback.onError(errorMessage);
                }
            });
        
        requestQueue.add(request);
    }
    
    /**
     * 초단기예보 API URL 생성
     */
    private String buildForecastUrl(String baseDate, String baseTime, int nx, int ny) {
        return BASE_URL + 
            "?serviceKey=" + BuildConfig.WEATHER_API_SERVICE_KEY +
            "&pageNo=1" +
            "&numOfRows=1000" +
            "&dataType=JSON" +
            "&base_date=" + baseDate +
            "&base_time=" + baseTime +
            "&nx=" + nx +
            "&ny=" + ny;
    }
    
    /**
     * 초단기실황 API URL 생성
     */
    private String buildObservationUrl(String baseDate, String baseTime, int nx, int ny) {
        return "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst" + 
            "?serviceKey=" + BuildConfig.WEATHER_API_SERVICE_KEY +
            "&pageNo=1" +
            "&numOfRows=1000" +
            "&dataType=JSON" +
            "&base_date=" + baseDate +
            "&base_time=" + baseTime +
            "&nx=" + nx +
            "&ny=" + ny;
    }
    
    /**
     * 요청 큐 정리
     */
    public void cancelAllRequests() {
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }
}