package com.example.umbrellaalert.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.example.umbrellaalert.BuildConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * API 키를 안전하게 관리하기 위한 유틸리티 클래스
 */
public class ApiKeyUtil {

    private static final String TAG = "ApiKeyUtil";
    private static final String API_KEYS_FILE = "api_keys.properties";
    
    private static Properties properties;
    
    /**
     * API 키 프로퍼티 파일 로드
     * @param context 애플리케이션 컨텍스트
     */
    private static void loadProperties(Context context) {
        if (properties == null) {
            properties = new Properties();
            AssetManager assetManager = context.getAssets();
            
            try (InputStream inputStream = assetManager.open(API_KEYS_FILE)) {
                properties.load(inputStream);
            } catch (IOException e) {
                Log.e(TAG, "API 키 파일을 로드할 수 없습니다", e);
            }
        }
    }
    


    /**
     * 기상청 API허브 키 가져오기
     * @param context 애플리케이션 컨텍스트
     * @return 기상청 API허브 키
     */
    public static String getKmaApiHubKey(Context context) {
        loadProperties(context);
        String apiKey = properties.getProperty("kma_apihub_key");

        // 키가 없는 경우 기본값 반환 (개발용)
        if (apiKey == null || apiKey.isEmpty()) {
            Log.w(TAG, "기상청 API허브 키가 없습니다. 기본값을 사용합니다.");
            return "BVvR_jijSSSb0f44o8kkqQ"; // 올바른 API 키
        }

        return apiKey;
    }

    /**
     * OpenWeather API 키 가져오기
     * @param context 애플리케이션 컨텍스트
     * @return OpenWeather API 키
     */
    public static String getOpenWeatherApiKey(Context context) {
        // BuildConfig에서 먼저 시도
        String apiKey = BuildConfig.OPENWEATHER_API_KEY;
        if (apiKey != null && !apiKey.isEmpty() && !apiKey.equals("null")) {
            Log.d(TAG, "BuildConfig에서 OpenWeather API 키 로드 완료");
            return apiKey;
        }

        // BuildConfig에 없으면 properties 파일에서 시도
        loadProperties(context);
        apiKey = properties.getProperty("openweather_api_key");

        // 키가 없는 경우 기본값 반환 (개발용)
        if (apiKey == null || apiKey.isEmpty()) {
            Log.w(TAG, "OpenWeather API 키가 없습니다. 기본값을 사용합니다.");
            return "bef3d511dc00345ed56204adcf073d16"; // 제공받은 API 키
        }

        return apiKey;
    }

    /**
     * 버스 API 키 가져오기
     * @param context 애플리케이션 컨텍스트
     * @return 버스 API 키
     */
    public static String getBusApiKey(Context context) {
        // BuildConfig에서 먼저 시도
        String apiKey = BuildConfig.BUS_API_SERVICE_KEY;
        if (apiKey != null && !apiKey.isEmpty() && !apiKey.equals("null")) {
            Log.d(TAG, "BuildConfig에서 버스 API 키 로드 완료");
            return apiKey;
        }

        // BuildConfig에 없으면 properties 파일에서 시도
        loadProperties(context);
        apiKey = properties.getProperty("bus_api_key");

        // 키가 없는 경우 기본값 반환 (개발용)
        if (apiKey == null || apiKey.isEmpty()) {
            Log.w(TAG, "버스 API 키가 없습니다. 기본값을 사용합니다.");
            return "VJ9IZb8N%2BRRUt%2Bl%2FtdMwuR2gO2W%2FyER8etH1%2FlCcR3q0c4AvOiXSItNi9hcNAfyrQOMTVvOkE0wJwTxnXZ0PDA%3D%3D";
        }

        return apiKey;
    }

    /**
     * 네이버 지도 클라이언트 ID 가져오기
     * @param context 애플리케이션 컨텍스트
     * @return 네이버 지도 클라이언트 ID
     */
    public static String getNaverMapClientId(Context context) {
        // BuildConfig에서 먼저 시도
        String clientId = BuildConfig.NAVER_MAP_CLIENT_ID;
        if (clientId != null && !clientId.isEmpty() && !clientId.equals("null")) {
            Log.d(TAG, "BuildConfig에서 네이버 지도 클라이언트 ID 로드 완료");
            return clientId;
        }

        // BuildConfig에 없으면 properties 파일에서 시도
        loadProperties(context);
        clientId = properties.getProperty("naver_client_id");

        // 키가 없는 경우 기본값 반환 (개발용)
        if (clientId == null || clientId.isEmpty()) {
            Log.w(TAG, "네이버 지도 클라이언트 ID가 없습니다. 기본값을 사용합니다.");
            return "okua9z6cuf";
        }

        return clientId;
    }
}
