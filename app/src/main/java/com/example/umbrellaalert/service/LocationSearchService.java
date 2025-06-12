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
        Log.d(TAG, "🔍 장소 검색 시작 - 검색어: '" + query + "'");

        if (query == null || query.trim().isEmpty()) {
            Log.w(TAG, "❌ 빈 검색어로 인해 검색 중단");
            return new ArrayList<>(); // 빈 검색어일 때는 빈 리스트 반환
        }

        if (geocodingClient == null) {
            Log.w(TAG, "❌ Geocoding 클라이언트가 초기화되지 않았습니다.");
            return new ArrayList<>();
        }

        try {
            Log.d(TAG, "🌐 네이버 Geocoding API 클라이언트로 검색 시작");

            // 네이버 Geocoding API로 검색
            List<SearchLocation> apiResults = geocodingClient.searchByQuerySync(query);

            Log.d(TAG, "✅ 네이버 API 검색 완료 - 결과: " + apiResults.size() + "개");

            // 결과 상세 로그
            if (!apiResults.isEmpty()) {
                for (int i = 0; i < apiResults.size(); i++) {
                    SearchLocation location = apiResults.get(i);
                    Log.d(TAG, "  결과 " + (i+1) + ": " + location.getName() + " - " + location.getAddress());
                }
            } else {
                Log.w(TAG, "⚠️ 검색 결과가 없습니다");
            }

            return apiResults;

        } catch (Exception e) {
            Log.e(TAG, "❌ 네이버 API 검색 실패", e);
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
        // 세종시 범위 내인지 확인 (더 정확한 범위)
        if (latitude >= 36.45 && latitude <= 36.65 && longitude >= 127.20 && longitude <= 127.35) {
            return "세종특별자치시 (대략적 위치)";
        }
        // 대전시 범위 내인지 확인 (더 정확한 범위)
        else if (latitude >= 36.25 && latitude <= 36.45 && longitude >= 127.30 && longitude <= 127.50) {
            return "대전광역시 (대략적 위치)";
        }
        // 충청남도 범위 확인
        else if (latitude >= 36.0 && latitude <= 37.0 && longitude >= 126.3 && longitude <= 127.8) {
            return "충청남도 (대략적 위치)";
        }
        // 충청북도 범위 확인
        else if (latitude >= 36.0 && latitude <= 37.2 && longitude >= 127.3 && longitude <= 128.5) {
            return "충청북도 (대략적 위치)";
        }
        // 서울 범위 확인
        else if (latitude >= 37.4 && latitude <= 37.7 && longitude >= 126.7 && longitude <= 127.3) {
            return "서울특별시 (대략적 위치)";
        }
        // 경기도 범위 확인
        else if (latitude >= 37.0 && latitude <= 38.0 && longitude >= 126.5 && longitude <= 127.8) {
            return "경기도 (대략적 위치)";
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
