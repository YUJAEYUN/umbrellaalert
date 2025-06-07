package com.example.umbrellaalert.data.api;

import android.content.Context;
import android.util.Log;

import com.example.umbrellaalert.BuildConfig;
import com.example.umbrellaalert.data.model.BusArrival;
import com.example.umbrellaalert.data.model.BusStop;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 버스 API 클라이언트
 * 공공데이터포털의 버스 정보 API를 사용
 */
public class BusApiClient {
    
    private static final String TAG = "BusApiClient";
    
    // API 기본 URL - 공공데이터포털 문서 기준
    private static final String NEARBY_STOPS_URL = "http://apis.data.go.kr/1613000/BusSttnInfoInqireService/getCrdntPrxmtSttnList";
    private static final String ARRIVAL_INFO_URL = "http://apis.data.go.kr/1613000/ArvlInfoInqireService/getSttnAcctoArvlPrearngeInfoList";
    
    private final Context context;
    private final ExecutorService executorService;
    private final String serviceKey;
    private final Gson gson;

    public BusApiClient(Context context) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
        this.serviceKey = BuildConfig.BUS_API_SERVICE_KEY;
        this.gson = new Gson();
    }

    /**
     * 좌표 기반 근접 정류소 목록 조회
     */
    public Future<List<BusStop>> getNearbyBusStops(double latitude, double longitude) {
        return executorService.submit(new Callable<List<BusStop>>() {
            @Override
            public List<BusStop> call() throws Exception {
                try {
                    // 서비스 키가 이미 인코딩되어 있으므로 직접 사용
                    String urlStr = NEARBY_STOPS_URL +
                        "?serviceKey=" + serviceKey +
                        "&pageNo=1" +
                        "&numOfRows=50" +
                        "&_type=json" +
                        "&gpsLati=" + latitude +
                        "&gpsLong=" + longitude;
                    
                    Log.d(TAG, "🚌 근접 정류소 API 요청: " + urlStr);

                    String response = executeHttpRequest(urlStr);
                    Log.d(TAG, "🚌 API 응답 길이: " + response.length());
                    Log.d(TAG, "🚌 API 응답 시작: " + response.substring(0, Math.min(200, response.length())));

                    List<BusStop> result = parseNearbyStopsResponse(response);

                    Log.d(TAG, "근접 정류소 조회 완료: " + result.size() + "개");
                    return result;

                } catch (Exception e) {
                    Log.e(TAG, "근접 정류소 조회 실패", e);
                    return new ArrayList<>();
                }
            }
        });
    }

    /**
     * 정류소별 도착 예정 정보 조회
     */
    public Future<List<BusArrival>> getBusArrivalInfo(String nodeId, int cityCode) {
        return executorService.submit(new Callable<List<BusArrival>>() {
            @Override
            public List<BusArrival> call() throws Exception {
                try {
                    // 공공데이터포털 문서 기준 파라미터 사용
                    String urlStr = ARRIVAL_INFO_URL +
                        "?serviceKey=" + serviceKey +
                        "&pageNo=1" +
                        "&numOfRows=10" +
                        "&_type=json" +
                        "&cityCode=" + cityCode +
                        "&nodeId=" + nodeId;
                    
                    Log.d(TAG, "🚌 도착 정보 API 요청: " + urlStr);
                    
                    String response = executeHttpRequest(urlStr);
                    List<BusArrival> result = parseArrivalInfoResponse(response);

                    Log.d(TAG, "버스 도착 정보 조회 완료: " + result.size() + "개");
                    return result;

                } catch (Exception e) {
                    Log.e(TAG, "도착 정보 조회 실패", e);
                    return new ArrayList<>();
                }
            }
        });
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
            connection.setRequestProperty("Accept", "application/json");
            
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "HTTP 응답 코드: " + responseCode);
            
            BufferedReader reader;
            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            } else {
                // 오류 응답도 읽어서 확인
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "UTF-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String result = response.toString();
            Log.d(TAG, "API 응답 (코드: " + responseCode + "): " + result.substring(0, Math.min(result.length(), 500)) + "...");

            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP 오류 응답: " + result);
                throw new IOException("HTTP 오류: " + responseCode + ", 응답: " + result);
            }

            return result;
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 근접 정류소 응답 파싱
     */
    private List<BusStop> parseNearbyStopsResponse(String response) {
        List<BusStop> busStops = new ArrayList<>();

        try {
            // XML 오류 응답 체크
            if (response.contains("<OpenAPI_ServiceResponse>") || response.contains("SERVICE ERROR")) {
                Log.e(TAG, "API 오류 응답: " + response);
                return busStops;
            }

            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
            JsonObject responseObj = jsonObject.getAsJsonObject("response");
            JsonObject body = responseObj.getAsJsonObject("body");
            
            if (body.has("items")) {
                JsonObject items = body.getAsJsonObject("items");
                JsonArray itemArray = items.getAsJsonArray("item");
                
                if (itemArray != null) {
                    for (JsonElement element : itemArray) {
                        JsonObject item = element.getAsJsonObject();
                        BusStop busStop = parseBusStopFromJson(item);
                        if (busStop != null) {
                            busStops.add(busStop);
                        }
                    }
                }
            }
            
            Log.d(TAG, "✅ 근접 정류소 파싱 완료: " + busStops.size() + "개");
            
        } catch (Exception e) {
            Log.e(TAG, "근접 정류소 응답 파싱 실패", e);
        }
        
        return busStops;
    }

    /**
     * 도착 정보 응답 파싱
     */
    private List<BusArrival> parseArrivalInfoResponse(String response) {
        List<BusArrival> arrivals = new ArrayList<>();

        try {
            // XML 오류 응답 체크
            if (response.contains("<OpenAPI_ServiceResponse>") || response.contains("SERVICE ERROR")) {
                Log.e(TAG, "API 오류 응답: " + response);
                return arrivals;
            }

            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
            JsonObject responseObj = jsonObject.getAsJsonObject("response");
            JsonObject body = responseObj.getAsJsonObject("body");
            
            if (body.has("items")) {
                JsonObject items = body.getAsJsonObject("items");
                JsonArray itemArray = items.getAsJsonArray("item");
                
                if (itemArray != null) {
                    for (JsonElement element : itemArray) {
                        JsonObject item = element.getAsJsonObject();
                        BusArrival arrival = parseBusArrivalFromJson(item);
                        if (arrival != null) {
                            arrivals.add(arrival);
                        }
                    }
                }
            }
            
            Log.d(TAG, "✅ 도착 정보 파싱 완료: " + arrivals.size() + "개");
            
        } catch (Exception e) {
            Log.e(TAG, "도착 정보 응답 파싱 실패", e);
        }
        
        return arrivals;
    }

    /**
     * JSON에서 BusStop 객체 생성
     */
    private BusStop parseBusStopFromJson(JsonObject item) {
        try {
            BusStop busStop = new BusStop();
            
            if (item.has("nodeid")) busStop.setNodeId(item.get("nodeid").getAsString());
            if (item.has("nodenm")) busStop.setNodeName(item.get("nodenm").getAsString());
            if (item.has("gpslati")) busStop.setGpsLati(item.get("gpslati").getAsDouble());
            if (item.has("gpslong")) busStop.setGpsLong(item.get("gpslong").getAsDouble());
            if (item.has("citycode")) busStop.setCityCode(item.get("citycode").getAsInt());
            if (item.has("nodeno")) busStop.setNodeNo(item.get("nodeno").getAsString());
            if (item.has("routetype")) busStop.setRouteType(item.get("routetype").getAsString());
            
            return busStop;
        } catch (Exception e) {
            Log.e(TAG, "BusStop 파싱 실패", e);
            return null;
        }
    }

    /**
     * JSON에서 BusArrival 객체 생성
     */
    private BusArrival parseBusArrivalFromJson(JsonObject item) {
        try {
            BusArrival arrival = new BusArrival();
            
            if (item.has("nodeid")) arrival.setNodeId(item.get("nodeid").getAsString());
            if (item.has("routeid")) arrival.setRouteId(item.get("routeid").getAsString());
            if (item.has("routeno")) arrival.setRouteNo(item.get("routeno").getAsString());
            if (item.has("routetp")) arrival.setRouteType(item.get("routetp").getAsString());
            if (item.has("arrprevstationcnt")) arrival.setArrPrevStationCnt(item.get("arrprevstationcnt").getAsInt());
            if (item.has("arrtime")) {
                int arrTimeSeconds = item.get("arrtime").getAsInt();
                // 초 단위를 분 단위로 변환 (60초 = 1분)
                int arrTimeMinutes = Math.max(1, arrTimeSeconds / 60);
                arrival.setArrTime(arrTimeMinutes);
            }
            if (item.has("vehicletp")) arrival.setVehicleNo(item.get("vehicletp").getAsString());
            if (item.has("routetypenm")) arrival.setRouteTypeName(item.get("routetypenm").getAsString());
            
            return arrival;
        } catch (Exception e) {
            Log.e(TAG, "BusArrival 파싱 실패", e);
            return null;
        }
    }

    /**
     * 테스트용 정류장 생성 (API 실패 시 사용)
     */
    private List<BusStop> createTestBusStops(double latitude, double longitude) {
        List<BusStop> testStops = new ArrayList<>();

        // 세종시 주요 정류장들
        if (latitude > 36.4 && latitude < 36.6 && longitude > 127.2 && longitude < 127.3) {
            // 세종시청 정류장
            BusStop stop1 = new BusStop();
            stop1.setNodeId("SJB293064313");
            stop1.setNodeName("세종시청");
            stop1.setGpsLati(36.4800);
            stop1.setGpsLong(127.2890);
            stop1.setCityCode(12);
            stop1.setNodeNo("64313");
            testStops.add(stop1);

            // 정부세종청사 정류장
            BusStop stop2 = new BusStop();
            stop2.setNodeId("SJB293064314");
            stop2.setNodeName("정부세종청사");
            stop2.setGpsLati(36.4790);
            stop2.setGpsLong(127.2880);
            stop2.setCityCode(12);
            stop2.setNodeNo("64314");
            testStops.add(stop2);
        }

        // 대전 주요 정류장들
        else if (latitude > 36.3 && latitude < 36.4 && longitude > 127.3 && longitude < 127.5) {
            // 대전역 정류장
            BusStop stop1 = new BusStop();
            stop1.setNodeId("DJB8001793");
            stop1.setNodeName("대전역");
            stop1.setGpsLati(36.3515);
            stop1.setGpsLong(127.3845);
            stop1.setCityCode(25);
            stop1.setNodeNo("1793");
            testStops.add(stop1);
        }

        Log.d(TAG, "테스트용 정류장 생성: " + testStops.size() + "개");
        return testStops;
    }

    /**
     * 테스트용 버스 도착 정보 생성
     */
    private List<BusArrival> createTestBusArrivals(String nodeId) {
        List<BusArrival> testArrivals = new ArrayList<>();

        // 세종시 테스트 버스
        if (nodeId.startsWith("SJB")) {
            BusArrival arrival1 = new BusArrival();
            arrival1.setNodeId(nodeId);
            arrival1.setRouteId("SJR001");
            arrival1.setRouteNo("370");
            arrival1.setRouteTypeName("일반버스");
            arrival1.setArrTime(3);  // 3분 후
            arrival1.setArrPrevStationCnt(2);
            arrival1.setDirectionName("대전역");
            testArrivals.add(arrival1);

            BusArrival arrival2 = new BusArrival();
            arrival2.setNodeId(nodeId);
            arrival2.setRouteId("SJR002");
            arrival2.setRouteNo("990");
            arrival2.setRouteTypeName("급행버스");
            arrival2.setArrTime(8);  // 8분 후
            arrival2.setArrPrevStationCnt(5);
            arrival2.setDirectionName("조치원");
            testArrivals.add(arrival2);
        }

        // 대전 테스트 버스
        else if (nodeId.startsWith("DJB")) {
            BusArrival arrival1 = new BusArrival();
            arrival1.setNodeId(nodeId);
            arrival1.setRouteId("DJR001");
            arrival1.setRouteNo("102");
            arrival1.setRouteTypeName("일반버스");
            arrival1.setArrTime(2);  // 2분 후
            arrival1.setArrPrevStationCnt(1);
            arrival1.setDirectionName("유성온천");
            testArrivals.add(arrival1);
        }

        Log.d(TAG, "테스트용 버스 도착 정보 생성: " + testArrivals.size() + "개");
        return testArrivals;
    }
}
