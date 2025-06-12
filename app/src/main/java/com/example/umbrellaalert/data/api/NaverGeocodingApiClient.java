package com.example.umbrellaalert.data.api;

import android.content.Context;
import android.util.Log;

import com.example.umbrellaalert.BuildConfig;
import com.example.umbrellaalert.data.model.SearchLocation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 네이버 클라우드 플랫폼 Geocoding API 클라이언트
 */
public class NaverGeocodingApiClient {

    private static final String TAG = "NaverGeocodingApi";
    
    // 네이버 클라우드 플랫폼 Geocoding API URL
    private static final String GEOCODING_URL = "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode";
    private static final String REVERSE_GEOCODING_URL = "https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc";
    
    private final Context context;
    private final String clientId;
    private final String clientSecret;
    private final ExecutorService executorService;

    public NaverGeocodingApiClient(Context context) {
        this.context = context.getApplicationContext();
        this.clientId = BuildConfig.NAVER_MAP_CLIENT_ID;
        this.clientSecret = BuildConfig.NAVER_MAP_CLIENT_SECRET;
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * 주소/장소명으로 좌표 검색 (비동기)
     */
    public Future<List<SearchLocation>> searchByQuery(String query) {
        return executorService.submit(() -> searchByQuerySync(query));
    }

    /**
     * 주소/장소명으로 좌표 검색 (동기)
     */
    public List<SearchLocation> searchByQuerySync(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String urlStr = GEOCODING_URL + "?query=" + encodedQuery;
            
            Log.d(TAG, "🌐 네이버 Geocoding API 요청: " + urlStr);
            
            String response = executeHttpRequest(urlStr);
            Log.d(TAG, "📡 API 응답: " + response);
            
            return parseGeocodingResponse(response);
            
        } catch (Exception e) {
            Log.e(TAG, "Geocoding API 요청 실패", e);
            return new ArrayList<>();
        }
    }

    /**
     * 좌표를 주소로 변환 (Reverse Geocoding) - 비동기
     */
    public Future<String> getAddressFromCoordinates(double latitude, double longitude) {
        return executorService.submit(() -> getAddressFromCoordinatesSync(latitude, longitude));
    }

    /**
     * 좌표를 주소로 변환 (Reverse Geocoding) - 동기
     */
    public String getAddressFromCoordinatesSync(double latitude, double longitude) {
        try {
            String urlStr = REVERSE_GEOCODING_URL + 
                           "?coords=" + longitude + "," + latitude + 
                           "&output=json&orders=roadaddr,addr";
            
            Log.d(TAG, "🌐 네이버 Reverse Geocoding API 요청: " + urlStr);
            
            String response = executeHttpRequest(urlStr);
            Log.d(TAG, "📡 Reverse Geocoding API 응답: " + response);
            
            return parseReverseGeocodingResponse(response);
            
        } catch (Exception e) {
            Log.e(TAG, "Reverse Geocoding API 요청 실패", e);
            return String.format("위치 (%.4f, %.4f)", latitude, longitude);
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
            
            // 네이버 클라우드 플랫폼 인증 헤더 추가
            connection.setRequestProperty("X-NCP-APIGW-API-KEY-ID", clientId);
            connection.setRequestProperty("X-NCP-APIGW-API-KEY", clientSecret);
            
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
     * Geocoding API 응답 파싱
     */
    private List<SearchLocation> parseGeocodingResponse(String response) {
        List<SearchLocation> results = new ArrayList<>();
        
        try {
            JSONObject json = new JSONObject(response);
            
            if (!json.has("addresses")) {
                Log.w(TAG, "응답에 addresses 필드가 없습니다");
                return results;
            }
            
            JSONArray addresses = json.getJSONArray("addresses");
            
            for (int i = 0; i < addresses.length() && i < 10; i++) { // 최대 10개
                JSONObject address = addresses.getJSONObject(i);
                
                String roadAddress = address.optString("roadAddress", "");
                String jibunAddress = address.optString("jibunAddress", "");
                String englishAddress = address.optString("englishAddress", "");
                
                // 도로명 주소 우선, 없으면 지번 주소 사용
                String displayAddress = !roadAddress.isEmpty() ? roadAddress : jibunAddress;
                
                if (!displayAddress.isEmpty()) {
                    double lat = address.getDouble("y");
                    double lng = address.getDouble("x");

                    // 장소명은 주소에서 추출하거나 간단한 이름 생성
                    String placeName = extractPlaceNameFromAddress(displayAddress);

                    // 만약 영어 주소가 있다면 더 구체적인 이름을 시도
                    if (!englishAddress.isEmpty() && englishAddress.contains(",")) {
                        String[] englishParts = englishAddress.split(",");
                        if (englishParts.length > 0) {
                            String englishPlaceName = englishParts[0].trim();
                            if (!englishPlaceName.isEmpty() && englishPlaceName.length() < placeName.length()) {
                                placeName = englishPlaceName;
                            }
                        }
                    }

                    SearchLocation location = new SearchLocation(
                        placeName,
                        displayAddress,
                        lat,
                        lng,
                        "검색결과"
                    );

                    results.add(location);
                    Log.d(TAG, "검색 결과 추가: " + placeName + " (" + lat + ", " + lng + ")");
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Geocoding 응답 파싱 실패", e);
        }
        
        return results;
    }

    /**
     * Reverse Geocoding API 응답 파싱
     */
    private String parseReverseGeocodingResponse(String response) {
        try {
            JSONObject json = new JSONObject(response);
            
            if (!json.has("results")) {
                Log.w(TAG, "Reverse Geocoding 응답에 results 필드가 없습니다");
                return "주소를 찾을 수 없습니다";
            }
            
            JSONArray results = json.getJSONArray("results");
            
            if (results.length() > 0) {
                JSONObject result = results.getJSONObject(0);
                JSONObject region = result.getJSONObject("region");
                
                // 시/도, 시/군/구, 읍/면/동 정보 추출
                String area1 = region.getJSONObject("area1").optString("name", "");
                String area2 = region.getJSONObject("area2").optString("name", "");
                String area3 = region.getJSONObject("area3").optString("name", "");
                
                StringBuilder address = new StringBuilder();
                if (!area1.isEmpty()) address.append(area1);
                if (!area2.isEmpty()) {
                    if (address.length() > 0) address.append(" ");
                    address.append(area2);
                }
                if (!area3.isEmpty()) {
                    if (address.length() > 0) address.append(" ");
                    address.append(area3);
                }
                
                return address.toString();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Reverse Geocoding 응답 파싱 실패", e);
        }
        
        return "주소를 찾을 수 없습니다";
    }

    /**
     * 주소에서 장소명 추출
     */
    private String extractPlaceNameFromAddress(String address) {
        if (address == null || address.isEmpty()) {
            return "검색된 장소";
        }

        // 주소에서 의미있는 장소명 추출
        String[] parts = address.split(" ");

        // 마지막 2-3개 부분을 조합하여 장소명 생성
        StringBuilder placeName = new StringBuilder();
        int startIndex = Math.max(0, parts.length - 3);

        for (int i = startIndex; i < parts.length; i++) {
            if (placeName.length() > 0) {
                placeName.append(" ");
            }
            placeName.append(parts[i]);
        }

        String result = placeName.toString();
        return result.isEmpty() ? "검색된 장소" : result;
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
