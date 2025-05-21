package com.example.umbrellaalert.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

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
     * 기상청 API 키 가져오기
     * @param context 애플리케이션 컨텍스트
     * @return 기상청 API 키
     */
    public static String getKmaApiKey(Context context) {
        loadProperties(context);
        String apiKey = properties.getProperty("kma_api_key");
        
        // 키가 없는 경우 기본값 반환 (개발용)
        if (apiKey == null || apiKey.isEmpty()) {
            Log.w(TAG, "기상청 API 키가 없습니다. 기본값을 사용합니다.");
            return "SAMPLE_API_KEY";
        }
        
        return apiKey;
    }
    
    /**
     * 다른 API 키가 필요한 경우 여기에 메서드 추가
     */
}
