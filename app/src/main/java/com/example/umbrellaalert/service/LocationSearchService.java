package com.example.umbrellaalert.service;

import android.content.Context;
import android.util.Log;

import com.example.umbrellaalert.data.api.NaverGeocodingApiClient;
import com.example.umbrellaalert.data.model.SearchLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * 장소 검색 서비스 (네이버 클라우드 플랫폼 Geocoding API 사용)
 */
public class LocationSearchService {

    private static final String TAG = "LocationSearchService";
    private static NaverGeocodingApiClient geocodingClient;

    /**
     * 네이버 Geocoding API 클라이언트 초기화
     */
    public static void initialize(Context context) {
        if (geocodingClient == null) {
            geocodingClient = new NaverGeocodingApiClient(context);
            Log.d(TAG, "네이버 Geocoding API 클라이언트 초기화 완료");
        }
    }


    /**
     * 장소 이름으로 검색 (네이버 Geocoding API 사용)
     */
    public static List<SearchLocation> searchByName(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(); // 빈 검색어일 때는 빈 리스트 반환
        }

        if (geocodingClient == null) {
            Log.w(TAG, "Geocoding 클라이언트가 초기화되지 않았습니다.");
            return new ArrayList<>();
        }

        try {
            // 네이버 Geocoding API로 검색
            List<SearchLocation> apiResults = geocodingClient.searchByQuerySync(query);

            Log.d(TAG, "네이버 API 검색 결과: " + apiResults.size() + "개");
            return apiResults;

        } catch (Exception e) {
            Log.e(TAG, "네이버 API 검색 실패", e);
            return new ArrayList<>();
        }
    }

    /**
     * 좌표를 주소로 변환 (네이버 Reverse Geocoding API 사용)
     */
    public static String getAddressFromCoordinates(double latitude, double longitude) {
        if (geocodingClient == null) {
            Log.w(TAG, "Geocoding 클라이언트가 초기화되지 않았습니다. 기본 주소를 반환합니다.");
            return getFallbackAddress(latitude, longitude);
        }

        try {
            String address = geocodingClient.getAddressFromCoordinatesSync(latitude, longitude);
            Log.d(TAG, "네이버 Reverse Geocoding 결과: " + address);
            return address;
        } catch (Exception e) {
            Log.e(TAG, "네이버 Reverse Geocoding 실패, 기본 주소 사용", e);
            return getFallbackAddress(latitude, longitude);
        }
    }

    /**
     * 네이버 API 실패 시 사용할 기본 주소 (좌표 범위 기반)
     */
    private static String getFallbackAddress(double latitude, double longitude) {
        // 세종시 범위 내인지 확인
        if (latitude >= 36.4 && latitude <= 36.7 && longitude >= 127.1 && longitude <= 127.4) {
            return "세종특별자치시";
        }
        // 대전시 범위 내인지 확인
        else if (latitude >= 36.2 && latitude <= 36.5 && longitude >= 127.2 && longitude <= 127.5) {
            return "대전광역시";
        } else {
            return String.format("위치 (%.4f, %.4f)", latitude, longitude);
        }
    }



    /**
     * 리소스 정리
     */
    public static void shutdown() {
        if (geocodingClient != null) {
            geocodingClient.shutdown();
            geocodingClient = null;
            Log.d(TAG, "LocationSearchService 리소스 정리 완료");
        }
    }
}
