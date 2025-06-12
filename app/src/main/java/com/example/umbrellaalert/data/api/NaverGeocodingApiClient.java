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
    
    // 네이버 클라우드 플랫폼 API URL (올바른 도메인 사용)
    private static final String GEOCODING_URL = "https://maps.apigw.ntruss.com/map-geocode/v2/geocode";
    private static final String REVERSE_GEOCODING_URL = "https://maps.apigw.ntruss.com/map-reversegeocode/v2/gc";
    private static final String PLACES_URL = "https://maps.apigw.ntruss.com/map-place/v1/search";
    
    private final Context context;
    private final String clientId;
    private final String clientSecret;
    private final ExecutorService executorService;

    public NaverGeocodingApiClient(Context context) {
        this.context = context.getApplicationContext();
        this.clientId = BuildConfig.NAVER_MAP_CLIENT_ID;
        this.clientSecret = BuildConfig.NAVER_MAP_CLIENT_SECRET;
        this.executorService = Executors.newCachedThreadPool();

        // 디버깅용: 실제 API 키 값 확인
        Log.d(TAG, "🔧 BuildConfig에서 읽은 Client ID: '" + clientId + "'");
        Log.d(TAG, "🔧 BuildConfig에서 읽은 Client Secret: '" + clientSecret + "'");
        Log.d(TAG, "🔧 Client ID 길이: " + (clientId != null ? clientId.length() : "null"));
        Log.d(TAG, "🔧 Client Secret 길이: " + (clientSecret != null ? clientSecret.length() : "null"));
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
            // 검색어 전처리 및 개선
            String processedQuery = preprocessSearchQuery(query);
            String encodedQuery = URLEncoder.encode(processedQuery, "UTF-8");
            String urlStr = GEOCODING_URL + "?query=" + encodedQuery;

            Log.d(TAG, "🌐 네이버 Geocoding API 요청: " + urlStr);
            Log.d(TAG, "🔍 원본 검색어: '" + query + "' → 처리된 검색어: '" + processedQuery + "'");

            String response = executeHttpRequest(urlStr);
            Log.d(TAG, "📡 API 응답: " + response);

            List<SearchLocation> results = parseGeocodingResponse(response);

            // 결과가 없으면 POI 검색 시도
            if (results.isEmpty()) {
                Log.d(TAG, "🔄 Geocoding 검색 실패, POI 검색 시도");
                results = searchPOI(query);
            }

            // 여전히 결과가 없으면 원본 검색어로 재시도
            if (results.isEmpty() && !query.equals(processedQuery)) {
                Log.d(TAG, "🔄 POI 검색도 실패, 원본 검색어로 재시도");
                String originalEncodedQuery = URLEncoder.encode(query, "UTF-8");
                String originalUrlStr = GEOCODING_URL + "?query=" + originalEncodedQuery;

                Log.d(TAG, "🌐 재시도 요청: " + originalUrlStr);
                String retryResponse = executeHttpRequest(originalUrlStr);
                results = parseGeocodingResponse(retryResponse);
            }

            return results;

        } catch (Exception e) {
            Log.e(TAG, "Geocoding API 요청 실패", e);
            return new ArrayList<>();
        }
    }

    /**
     * POI(관심지점) 검색 (Places API 사용)
     */
    private List<SearchLocation> searchPOI(String query) {
        List<SearchLocation> results = new ArrayList<>();

        try {
            // 1. Places API로 검색 시도
            Log.d(TAG, "🏢 네이버 Places API로 검색: " + query);
            List<SearchLocation> placesResults = searchPlacesAPI(query);
            results.addAll(placesResults);

            // 2. Places API 결과가 없으면 로컬 데이터베이스 사용
            if (results.isEmpty()) {
                Log.d(TAG, "🔄 Places API 결과 없음, 로컬 데이터베이스 검색");
                List<SearchLocation> localResults = searchLocalPOI(query);
                results.addAll(localResults);
            }

            // 결과 로그 출력
            if (!results.isEmpty()) {
                Log.d(TAG, "📍 POI 검색 결과: " + results.size() + "개");
                for (SearchLocation location : results) {
                    Log.d(TAG, "  - " + location.getName() + " (" + location.getLatitude() + ", " + location.getLongitude() + ")");
                }
            } else {
                Log.d(TAG, "❌ POI 검색 결과 없음");
            }

        } catch (Exception e) {
            Log.e(TAG, "POI 검색 실패", e);
        }

        return results;
    }

    /**
     * Places API를 사용한 검색 (현재는 사용하지 않음)
     */
    private List<SearchLocation> searchPlacesAPI(String query) {
        // Places API가 활성화되지 않았으므로 빈 결과 반환
        Log.d(TAG, "🚫 Places API 사용 안함 - 로컬 데이터베이스만 사용");
        return new ArrayList<>();
    }

    /**
     * 로컬 POI 데이터베이스에서 검색
     */
    private List<SearchLocation> searchLocalPOI(String query) {
        List<SearchLocation> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        Log.d(TAG, "🔍 로컬 POI 검색 - 입력: '" + query + "' → 소문자: '" + lowerQuery + "'");

        // 한밭대학교 검색
        if (lowerQuery.contains("한밭대학교")) {
            results.add(new SearchLocation(
                "한밭대학교",
                "대전광역시 유성구 동서대로 125 (덕명동)",
                36.3504, 127.2998,
                "대학교"
            ));
            Log.d(TAG, "✅ 한밭대학교 검색 결과 추가됨");
        }

        if (lowerQuery.contains("충남대") || lowerQuery.contains("충남대학교")) {
            results.add(new SearchLocation(
                "충남대학교",
                "대전광역시 유성구 대학로 99",
                36.3668, 127.3448,
                "대학교"
            ));
        }

        if (lowerQuery.contains("카이스트") || lowerQuery.contains("KAIST")) {
            results.add(new SearchLocation(
                "KAIST",
                "대전광역시 유성구 대학로 291",
                36.3736, 127.3616,
                "대학교"
            ));
        }

        if (lowerQuery.contains("건국대") || lowerQuery.contains("건국대학교")) {
            results.add(new SearchLocation(
                "건국대학교",
                "서울특별시 광진구 능동로 120",
                37.5419, 127.0799,
                "대학교"
            ));
        }

        // 주요 시설
        if (lowerQuery.contains("세종시청") || lowerQuery.equals("시청")) {
            results.add(new SearchLocation(
                "세종특별자치시청",
                "세종특별자치시 한누리대로 2130",
                36.4800, 127.2890,
                "관공서"
            ));
        }

        if (lowerQuery.contains("대전시청")) {
            results.add(new SearchLocation(
                "대전광역시청",
                "대전광역시 서구 둔산로 100",
                36.3504, 127.3845,
                "관공서"
            ));
        }

        if (lowerQuery.contains("대전역")) {
            results.add(new SearchLocation(
                "대전역",
                "대전광역시 동구 중앙로 215",
                36.3315, 127.4345,
                "교통"
            ));
        }

        if (lowerQuery.contains("서대전역")) {
            results.add(new SearchLocation(
                "서대전역",
                "대전광역시 서구 계룡로 493",
                36.3515, 127.3789,
                "교통"
            ));
        }

        // 병원
        if (lowerQuery.contains("충남대병원") || lowerQuery.contains("충남대학교병원")) {
            results.add(new SearchLocation(
                "충남대학교병원",
                "대전광역시 중구 문화로 282",
                36.3175, 127.4225,
                "병원"
            ));
        }

        // 쇼핑몰
        if (lowerQuery.contains("갤러리아") || lowerQuery.contains("타임월드")) {
            results.add(new SearchLocation(
                "갤러리아 타임월드",
                "대전광역시 서구 대덕대로 211",
                36.3535, 127.3789,
                "쇼핑몰"
            ));
        }

        return results;
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
            // Reverse Geocoding으로 상세 정보 조회 (API 문서에 따른 올바른 파라미터 사용)
            String urlStr = REVERSE_GEOCODING_URL +
                           "?coords=" + longitude + "," + latitude +
                           "&output=json&orders=legalcode,admcode,addr,roadaddr";

            Log.d(TAG, "🌐 네이버 Reverse Geocoding API 요청: " + urlStr);

            String response = executeHttpRequest(urlStr);
            Log.d(TAG, "📡 Reverse Geocoding API 응답: " + response);

            return parseReverseGeocodingResponse(response);

        } catch (Exception e) {
            Log.e(TAG, "❌ Reverse Geocoding API 요청 실패 - 오류 타입: " + e.getClass().getSimpleName(), e);
            Log.e(TAG, "❌ 오류 메시지: " + e.getMessage());
            Log.e(TAG, "❌ 요청했던 좌표: (" + latitude + ", " + longitude + ")");
            if (e.getCause() != null) {
                Log.e(TAG, "❌ 근본 원인: " + e.getCause().getMessage());
            }
            return String.format("위치 (%.4f, %.4f)", latitude, longitude);
        }
    }



    /**
     * HTTP 요청 실행
     */
    private String executeHttpRequest(String urlStr) throws IOException {
        Log.d(TAG, "🌐 HTTP 요청 시작: " + urlStr);

        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // 네이버 클라우드 플랫폼 인증 헤더 추가 (올바른 헤더명 사용)
            connection.setRequestProperty("x-ncp-apigw-api-key-id", clientId);
            connection.setRequestProperty("x-ncp-apigw-api-key", clientSecret);
            connection.setRequestProperty("Accept", "application/json");

            Log.d(TAG, "🔑 API 키 설정 - Client ID: '" + clientId + "', Client Secret: '" +
                  (clientSecret != null && !clientSecret.isEmpty() ? clientSecret : "없음") + "'");
            Log.d(TAG, "🔗 연결 시도 중...");

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "📊 HTTP 응답 코드: " + responseCode);
            Log.d(TAG, "📋 응답 메시지: " + connection.getResponseMessage());

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

                String responseStr = response.toString();
                Log.d(TAG, "✅ 성공 응답 내용: " + responseStr);
                return responseStr;
            } else {
                // 오류 응답 내용도 읽어보기
                BufferedReader errorReader = null;
                try {
                    errorReader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream())
                    );
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    Log.e(TAG, "❌ 오류 응답 내용: " + errorResponse.toString());
                } catch (Exception e) {
                    Log.e(TAG, "오류 응답 읽기 실패", e);
                } finally {
                    if (errorReader != null) {
                        try {
                            errorReader.close();
                        } catch (IOException e) {
                            // 무시
                        }
                    }
                }
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
            Log.d(TAG, "🔍 Reverse Geocoding 응답 길이: " + (response != null ? response.length() : "null"));
            Log.d(TAG, "🔍 전체 Reverse Geocoding 응답: " + response);

            if (response == null || response.trim().isEmpty()) {
                Log.e(TAG, "❌ 응답이 비어있습니다");
                return "응답이 비어있습니다";
            }

            JSONObject json = new JSONObject(response);

            if (!json.has("results")) {
                Log.w(TAG, "Reverse Geocoding 응답에 results 필드가 없습니다");
                return "주소를 찾을 수 없습니다";
            }

            JSONArray results = json.getJSONArray("results");

            if (results.length() > 0) {
                // 모든 결과를 확인해서 가장 상세한 정보 찾기
                for (int i = 0; i < results.length(); i++) {
                    JSONObject result = results.getJSONObject(i);
                    Log.d(TAG, "🏢 결과 " + i + ": " + result.toString());

                    // 1. 도로명 주소 우선 시도 (건물명 포함)
                    if (result.has("land")) {
                        JSONObject land = result.getJSONObject("land");
                        String roadAddress = buildDetailedRoadAddress(land);
                        if (!roadAddress.isEmpty() && containsBuildingInfo(roadAddress)) {
                            Log.d(TAG, "🏠 건물 정보 포함 주소 사용: " + roadAddress);
                            return roadAddress;
                        }
                    }
                }

                // 건물 정보가 없으면 첫 번째 결과 사용
                JSONObject firstResult = results.getJSONObject(0);

                // 2. 도로명 주소 시도
                if (firstResult.has("land")) {
                    JSONObject land = firstResult.getJSONObject("land");
                    String roadAddress = buildDetailedRoadAddress(land);
                    if (!roadAddress.isEmpty()) {
                        Log.d(TAG, "🏠 도로명 주소 사용: " + roadAddress);
                        return roadAddress;
                    }
                }

                // 3. 지역 정보로 구성
                if (firstResult.has("region")) {
                    JSONObject region = firstResult.getJSONObject("region");
                    String regionAddress = buildRegionAddress(region);
                    if (!regionAddress.isEmpty()) {
                        Log.d(TAG, "📍 지역 주소 사용: " + regionAddress);
                        return regionAddress;
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Reverse Geocoding 응답 파싱 실패", e);
        }

        return "주소를 찾을 수 없습니다";
    }

    /**
     * 상세 도로명 주소 구성 (건물명, 상호명 포함)
     */
    private String buildDetailedRoadAddress(JSONObject land) {
        try {
            Log.d(TAG, "🏗️ Land 객체 분석: " + land.toString());

            StringBuilder address = new StringBuilder();

            // 시/도
            String area1 = land.optString("area1", "");
            if (!area1.isEmpty()) {
                address.append(area1);
            }

            // 시/군/구
            String area2 = land.optString("area2", "");
            if (!area2.isEmpty()) {
                if (address.length() > 0) address.append(" ");
                address.append(area2);
            }

            // 읍/면/동
            String area3 = land.optString("area3", "");
            if (!area3.isEmpty()) {
                if (address.length() > 0) address.append(" ");
                address.append(area3);
            }

            // 도로명
            String roadName = land.optString("name", "");
            if (!roadName.isEmpty()) {
                if (address.length() > 0) address.append(" ");
                address.append(roadName);
            }

            // 건물번호
            String number1 = land.optString("number1", "");
            String number2 = land.optString("number2", "");
            if (!number1.isEmpty()) {
                if (address.length() > 0) address.append(" ");
                address.append(number1);
                if (!number2.isEmpty()) {
                    address.append("-").append(number2);
                }
            }

            // 건물명이나 상호명 찾기 시도
            String buildingInfo = extractBuildingInfo(land);
            if (!buildingInfo.isEmpty()) {
                if (address.length() > 0) address.append(" ");
                address.append("(").append(buildingInfo).append(")");
            }

            String result = address.toString();
            Log.d(TAG, "🏠 구성된 상세 주소: " + result);
            return result;

        } catch (Exception e) {
            Log.e(TAG, "상세 도로명 주소 구성 실패", e);
            return "";
        }
    }

    /**
     * 건물명이나 상호명 정보 추출
     */
    private String extractBuildingInfo(JSONObject land) {
        try {
            Log.d(TAG, "🔍 건물 정보 추출 시도 - Land 객체: " + land.toString());

            // 1. 직접적인 건물 관련 필드들 확인
            String[] buildingFields = {
                "building", "buildingName", "name", "placeName",
                "poiName", "landmark", "facility", "shop", "title"
            };

            for (String field : buildingFields) {
                String value = land.optString(field, "");
                if (!value.isEmpty() && !isRoadName(value) && !isAreaName(value)) {
                    Log.d(TAG, "🏢 건물 정보 발견 (" + field + "): " + value);
                    return value;
                }
            }

            // 2. addition 필드들 확인 (네이버 API에서 추가 정보 제공)
            for (int i = 0; i <= 4; i++) {
                String additionKey = "addition" + i;
                if (land.has(additionKey)) {
                    JSONObject addition = land.optJSONObject(additionKey);
                    if (addition != null) {
                        String type = addition.optString("type", "");
                        String value = addition.optString("value", "");

                        Log.d(TAG, "🔍 " + additionKey + " - type: " + type + ", value: " + value);

                        // 건물명, 상호명 관련 타입들
                        if (!value.isEmpty() && (
                            type.contains("building") ||
                            type.contains("poi") ||
                            type.contains("landmark") ||
                            type.contains("facility") ||
                            type.equals("BUILDING_NAME") ||
                            type.equals("POI_NAME") ||
                            type.equals("PLACE_NAME")
                        )) {
                            Log.d(TAG, "🏢 Addition에서 건물명 발견: " + value);
                            return value;
                        }

                        // 타입이 명확하지 않지만 건물명 같은 값들
                        if (!value.isEmpty() && !isRoadName(value) && !isAreaName(value) &&
                            value.length() > 1 && !value.matches("\\d+(-\\d+)?")) {
                            Log.d(TAG, "🏢 Addition에서 가능한 건물명 발견: " + value);
                            return value;
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "건물 정보 추출 실패", e);
        }

        return "";
    }

    /**
     * 건물 정보가 포함되어 있는지 확인
     */
    private boolean containsBuildingInfo(String address) {
        return address.contains("(") && address.contains(")");
    }

    /**
     * 지역명인지 확인 (건물명과 구분하기 위해)
     */
    private boolean isAreaName(String name) {
        if (name == null || name.isEmpty()) return false;

        // 지역명 패턴 확인
        String[] areaSuffixes = {"시", "군", "구", "동", "리", "면", "읍", "가", "로", "길", "대로"};
        for (String suffix : areaSuffixes) {
            if (name.endsWith(suffix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 도로명인지 확인 (건물명과 구분하기 위해)
     */
    private boolean isRoadName(String name) {
        if (name == null || name.isEmpty()) return false;

        // 도로명 패턴 확인
        String[] roadSuffixes = {"로", "길", "대로", "가", "동", "리", "면", "읍"};
        for (String suffix : roadSuffixes) {
            if (name.endsWith(suffix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 지역 주소 구성 (도로명 주소가 없을 때 사용)
     */
    private String buildRegionAddress(JSONObject region) {
        try {
            StringBuilder address = new StringBuilder();

            // 시/도, 시/군/구, 읍/면/동 정보 추출
            String area1 = region.getJSONObject("area1").optString("name", "");
            String area2 = region.getJSONObject("area2").optString("name", "");
            String area3 = region.getJSONObject("area3").optString("name", "");
            String area4 = region.getJSONObject("area4").optString("name", "");

            if (!area1.isEmpty()) address.append(area1);
            if (!area2.isEmpty()) {
                if (address.length() > 0) address.append(" ");
                address.append(area2);
            }
            if (!area3.isEmpty()) {
                if (address.length() > 0) address.append(" ");
                address.append(area3);
            }
            if (!area4.isEmpty()) {
                if (address.length() > 0) address.append(" ");
                address.append(area4);
            }

            return address.toString();

        } catch (Exception e) {
            Log.e(TAG, "지역 주소 구성 실패", e);
            return "";
        }
    }

    /**
     * 검색어 전처리 (POI 검색 개선)
     */
    private String preprocessSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return query;
        }

        String processed = query.trim();

        // 대학교 검색 개선
        if (processed.contains("대학교") || processed.contains("대학") || processed.contains("한밭")) {
            // "한밭대학교" → "대전 한밭대학교", "한밭대" 등으로 확장
            if (processed.contains("한밭")) {
                Log.d(TAG, "🎯 한밭대학교 검색어 감지: " + processed);
                return "대전 한밭대학교";
            }
            if (processed.equals("충남대학교") || processed.equals("충남대")) {
                return "대전 충남대학교";
            }
            if (processed.equals("건국대학교") || processed.equals("건국대")) {
                return "서울 건국대학교";
            }
            // 다른 대학들도 추가 가능
        }

        // 병원 검색 개선
        if (processed.contains("병원")) {
            // 지역명이 없으면 추가
            if (!processed.contains("시") && !processed.contains("구") && !processed.contains("동")) {
                // 유명 병원들의 위치 정보 추가
                if (processed.contains("서울대병원")) {
                    return "서울 종로구 서울대학교병원";
                }
                if (processed.contains("삼성서울병원")) {
                    return "서울 강남구 삼성서울병원";
                }
            }
        }

        // 공공기관 검색 개선
        if (processed.contains("시청") || processed.contains("구청") || processed.contains("동사무소")) {
            // 지역명이 없으면 세종시청으로 기본 설정
            if (processed.equals("시청")) {
                return "세종특별자치시청";
            }
        }

        return processed;
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
