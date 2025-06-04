package com.example.umbrellaalert.data.api;

import android.content.Context;
import android.util.Log;

import com.example.umbrellaalert.data.model.KmaForecast;
import com.example.umbrellaalert.data.model.KmaWeather;
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.util.ApiKeyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * 기상청 API 클라이언트
 */
public class KmaApiClient {

    private static final String TAG = "KmaApiClient";

    // API 상수 (공공데이터포털 기상청 API)
    private static final String BASE_URL = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0";
    private static final String ULTRA_SRT_NCST_URL = BASE_URL + "/getUltraSrtNcst"; // 초단기실황
    private static final String ULTRA_SRT_FCST_URL = BASE_URL + "/getUltraSrtFcst"; // 초단기예보
    private static final String VILAGE_FCST_URL = BASE_URL + "/getVilageFcst"; // 단기예보

    // API 키 (ApiKeyUtil에서 로드)

    // 싱글톤 인스턴스
    private static KmaApiClient instance;
    private final ExecutorService executorService;
    private final Context context;
    private String apiKey;



    // 싱글톤 패턴
    public static synchronized KmaApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new KmaApiClient(context.getApplicationContext());
        }
        return instance;
    }

    private KmaApiClient(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newCachedThreadPool();
        this.apiKey = ApiKeyUtil.getKmaApiKey(context);
    }

    /**
     * 초단기실황 조회 (현재 날씨) - 주변 격자 탐색 포함
     * @param nx X 좌표 (기상청 격자 좌표)
     * @param ny Y 좌표 (기상청 격자 좌표)
     * @return 날씨 정보
     */
    public Future<KmaWeather> getUltraSrtNcst(int nx, int ny) {
        return executorService.submit(new Callable<KmaWeather>() {
            @Override
            public KmaWeather call() throws Exception {
                // 주변 격자 좌표들을 시도해보기
                int[][] nearbyGrids = getNearbyGrids(nx, ny);

                for (int[] grid : nearbyGrids) {
                    try {
                        int currentNx = grid[0];
                        int currentNy = grid[1];

                        // 현재 시간 기준 가장 최근 발표 시각 계산
                        String baseDate = getCurrentDate();
                        String baseTime = getBaseTimeForUltraSrtNcst();

                        Log.d(TAG, "초단기실황 조회 시도 - 기준일자: " + baseDate + ", 기준시각: " + baseTime);
                        Log.d(TAG, "초단기실황 조회 시도 - 좌표: nx=" + currentNx + ", ny=" + currentNy);

                        // API 요청 URL 생성
                        String urlStr = buildApiUrl(ULTRA_SRT_NCST_URL, baseDate, baseTime, currentNx, currentNy);

                        // API 호출
                        String response = requestApi(urlStr);

                        // 응답 파싱 (XML 형식)
                        KmaWeather weather = parseXmlResponse(response);

                        // 날씨 데이터가 있으면 바로 반환 (유효성 검사 제거)
                        if (weather != null) {
                            Log.d(TAG, "✅ 날씨 데이터 발견 - 좌표: nx=" + currentNx + ", ny=" + currentNy +
                                     ", 온도: " + weather.getTemperature() + "°C");
                            return weather;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "격자 좌표 (" + grid[0] + ", " + grid[1] + ") 조회 실패: " + e.getMessage());
                        continue; // 다음 격자 시도
                    }
                }

                Log.e(TAG, "모든 주변 격자에서 유효한 데이터를 찾지 못함");
                // 빈 날씨 정보 반환
                return new KmaWeather();
            }
        });
    }

    /**
     * 초단기예보 조회 (6시간 예보) - 주변 격자 탐색 포함
     * @param nx X 좌표 (기상청 격자 좌표)
     * @param ny Y 좌표 (기상청 격자 좌표)
     * @return 예보 정보 리스트
     */
    public Future<List<KmaForecast>> getUltraSrtFcst(int nx, int ny) {
        return executorService.submit(new Callable<List<KmaForecast>>() {
            @Override
            public List<KmaForecast> call() throws Exception {
                // 주변 격자 좌표들을 시도해보기
                int[][] nearbyGrids = getNearbyGrids(nx, ny);

                for (int[] grid : nearbyGrids) {
                    try {
                        int currentNx = grid[0];
                        int currentNy = grid[1];

                        // 현재 시간 기준 가장 최근 발표 시각 계산
                        String baseDate = getCurrentDate();
                        String baseTime = getBaseTimeForUltraSrtFcst();

                        Log.d(TAG, "초단기예보 조회 시도 - 기준일자: " + baseDate + ", 기준시각: " + baseTime);
                        Log.d(TAG, "초단기예보 조회 시도 - 좌표: nx=" + currentNx + ", ny=" + currentNy);

                        // API 요청 URL 생성
                        String urlStr = buildApiUrl(ULTRA_SRT_FCST_URL, baseDate, baseTime, currentNx, currentNy);

                        // API 호출
                        String response = requestApi(urlStr);

                        // 응답 파싱 (XML 형식)
                        List<KmaForecast> forecasts = parseXmlForecastResponse(response);

                        // 예보 데이터가 있으면 바로 반환 (유효성 검사 제거)
                        if (forecasts != null && !forecasts.isEmpty()) {
                            Log.d(TAG, "✅ 예보 데이터 발견 - 좌표: nx=" + currentNx + ", ny=" + currentNy +
                                     ", 예보 개수: " + forecasts.size());
                            return forecasts;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "예보 격자 좌표 (" + grid[0] + ", " + grid[1] + ") 조회 실패: " + e.getMessage());
                        continue; // 다음 격자 시도
                    }
                }

                Log.e(TAG, "모든 주변 격자에서 유효한 예보 데이터를 찾지 못함");
                return new ArrayList<>();
            }
        });
    }

    /**
     * 단기예보 조회 (3일 예보)
     * @param nx X 좌표 (기상청 격자 좌표)
     * @param ny Y 좌표 (기상청 격자 좌표)
     * @return 예보 정보 리스트
     */
    public Future<List<KmaForecast>> getVilageFcst(int nx, int ny) {
        return executorService.submit(new Callable<List<KmaForecast>>() {
            @Override
            public List<KmaForecast> call() throws Exception {
                try {
                    // 현재 시간 기준 가장 최근 발표 시각 계산
                    String baseDate = getCurrentDate();
                    String baseTime = getBaseTimeForVilageFcst();

                    Log.w(TAG, "🔍 단기예보 조회 - 기준일자: " + baseDate + ", 기준시각: " + baseTime);
                    Log.w(TAG, "🔍 단기예보 조회 - 좌표: nx=" + nx + ", ny=" + ny);

                    // 현재 시간 정보도 로그 출력
                    Calendar now = Calendar.getInstance();
                    Log.w(TAG, "🕐 현재 시간: " + now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE));

                    // API 요청 URL 생성
                    String urlStr = buildApiUrl(VILAGE_FCST_URL, baseDate, baseTime, nx, ny);

                    // API 호출
                    String response = requestApi(urlStr);

                    // 응답 파싱 (XML 형식)
                    List<KmaForecast> forecasts = parseXmlForecastResponse(response);
                    return forecasts;
                } catch (Exception e) {
                    Log.e(TAG, "단기예보 조회 실패", e);
                    return new ArrayList<>();
                }
            }
        });
    }

    /**
     * 위도/경도를 기상청 격자 좌표로 변환
     * @param lat 위도
     * @param lon 경도
     * @return 기상청 격자 좌표 [nx, ny]
     */
    public int[] convertToGridCoord(double lat, double lon) {
        try {
            // 좌표 범위 검증
            if (lat < 20 || lat > 45 || lon < 120 || lon > 140) {
                Log.e(TAG, "위도/경도 범위 오류: lat=" + lat + ", lon=" + lon);
                // 서울 좌표 기본값 반환 (서울 중구 기준)
                return new int[] {60, 127};
            }

            // LCC 투영 방식 적용 (기상청 좌표계)
            double RE = 6371.00877; // 지구 반경(km)
            double GRID = 5.0; // 격자 간격(km)
            double SLAT1 = 30.0; // 표준위도 1
            double SLAT2 = 60.0; // 표준위도 2
            double OLON = 126.0; // 기준점 경도
            double OLAT = 38.0; // 기준점 위도
            double XO = 43; // 기준점 X좌표
            double YO = 136; // 기준점 Y좌표

            double DEGRAD = Math.PI / 180.0;
            double re = RE / GRID;
            double slat1 = SLAT1 * DEGRAD;
            double slat2 = SLAT2 * DEGRAD;
            double olon = OLON * DEGRAD;
            double olat = OLAT * DEGRAD;

            double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
            sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
            double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
            sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
            double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
            ro = re * sf / Math.pow(ro, sn);

            double ra = Math.tan(Math.PI * 0.25 + lat * DEGRAD * 0.5);
            ra = re * sf / Math.pow(ra, sn);
            double theta = lon * DEGRAD - olon;
            if (theta > Math.PI) theta -= 2.0 * Math.PI;
            if (theta < -Math.PI) theta += 2.0 * Math.PI;
            theta *= sn;

            int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
            int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

            // 좌표 범위 검증
            if (nx < 0 || nx > 149 || ny < 0 || ny > 253) {
                Log.e(TAG, "변환된 격자 좌표 범위 오류: nx=" + nx + ", ny=" + ny);
                // 서울 좌표 기본값 반환 (서울 중구 기준)
                return new int[] {60, 127};
            }

            Log.d(TAG, "위도/경도 변환 결과: lat=" + lat + ", lon=" + lon + " -> nx=" + nx + ", ny=" + ny);
            return new int[] {nx, ny};
        } catch (Exception e) {
            Log.e(TAG, "좌표 변환 오류", e);
            // 서울 좌표 기본값 반환 (서울 중구 기준)
            return new int[] {60, 127};
        }
    }

    // API URL 생성 (공공데이터포털 형식)
    private String buildApiUrl(String baseUrl, String baseDate, String baseTime, int nx, int ny) throws IOException {
        StringBuilder urlBuilder = new StringBuilder(baseUrl);

        // 첫 번째 파라미터는 ?로 시작
        urlBuilder.append("?serviceKey=").append(apiKey);

        // 공통 파라미터 (데이터 양 최소화)
        urlBuilder.append("&pageNo=1");
        urlBuilder.append("&numOfRows=100");  // 1000 -> 100으로 줄임
        urlBuilder.append("&dataType=XML");

        // 기준 날짜와 시간
        urlBuilder.append("&base_date=").append(baseDate);
        urlBuilder.append("&base_time=").append(baseTime);

        // 격자 좌표
        urlBuilder.append("&nx=").append(nx);
        urlBuilder.append("&ny=").append(ny);

        String url = urlBuilder.toString();
        Log.d(TAG, "API 요청 URL: " + url);
        return url;
    }

    /**
     * 주변 격자 좌표들을 생성 (최소한의 범위로 제한)
     * @param centerNx 중심 X 좌표
     * @param centerNy 중심 Y 좌표
     * @return 주변 격자 좌표 배열 (우선순위 순서)
     */
    private int[][] getNearbyGrids(int centerNx, int centerNy) {
        // 속도 최적화: 최대 5개 격자만 시도 (중심 + 인접 4방향)
        int[][] grids = {
            {centerNx, centerNy},           // 중심점 (1순위)
            {centerNx, centerNy + 1},       // 북쪽 (2순위)
            {centerNx, centerNy - 1},       // 남쪽 (3순위)
            {centerNx + 1, centerNy},       // 동쪽 (4순위)
            {centerNx - 1, centerNy},       // 서쪽 (5순위)
        };

        // 유효한 격자 좌표만 필터링
        List<int[]> validGrids = new ArrayList<>();
        for (int[] grid : grids) {
            if (isValidGrid(grid[0], grid[1])) {
                validGrids.add(grid);
            }
        }

        return validGrids.toArray(new int[validGrids.size()][]);
    }

    /**
     * 격자 좌표가 유효한 범위인지 확인
     */
    private boolean isValidGrid(int nx, int ny) {
        return nx >= 1 && nx <= 149 && ny >= 1 && ny <= 253;
    }



    // API 요청 실행
    private String requestApi(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");
        conn.setRequestProperty("Accept", "application/json");

        // 연결 타임아웃 설정 (속도 최적화)
        conn.setConnectTimeout(3000);  // 10초 -> 3초
        conn.setReadTimeout(5000);     // 10초 -> 5초

        BufferedReader rd;
        int responseCode = conn.getResponseCode();
        Log.d(TAG, "API 응답 코드: " + responseCode);

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
        // 응답 내용 로그 출력 (처음 500자만)
        String logResponse = response.length() > 500 ? response.substring(0, 500) + "..." : response;
        Log.d(TAG, "API 응답 내용: " + logResponse);

        return response;
    }

    // 현재 날짜 가져오기 (yyyyMMdd 형식)
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        return sdf.format(new Date());
    }

    // 초단기실황 기준 시간 계산 (매시간 40분 이후 호출 가능)
    private String getBaseTimeForUltraSrtNcst() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        // 현재 시각이 40분 이전이면 한 시간 전 발표 자료 사용
        if (minute < 40) {
            hour = (hour == 0) ? 23 : hour - 1;
        }

        return String.format(Locale.KOREA, "%02d00", hour);
    }

    // 초단기예보 기준 시간 계산 (매시간 45분 이후 호출 가능)
    private String getBaseTimeForUltraSrtFcst() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        // 현재 시각이 45분 이전이면 한 시간 전 발표 자료 사용
        if (minute < 45) {
            hour = (hour == 0) ? 23 : hour - 1;
        }

        return String.format(Locale.KOREA, "%02d30", hour);
    }

    // 단기예보 기준 시간 계산 (하루 8번 발표: 02, 05, 08, 11, 14, 17, 20, 23시)
    private String getBaseTimeForVilageFcst() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        // 발표 시각에 따라 기준 시간 결정
        if (hour < 2) {
            // 전날 23시 발표
            cal.add(Calendar.DATE, -1);
            return "2300";
        } else if (hour < 5) {
            return "0200";
        } else if (hour < 8) {
            return "0500";
        } else if (hour < 11) {
            return "0800";
        } else if (hour < 14) {
            return "1100";
        } else if (hour < 17) {
            return "1400";
        } else if (hour < 20) {
            return "1700";
        } else if (hour < 23) {
            return "2000";
        } else {
            return "2300";
        }
    }

    // 초단기실황 응답 파싱 (공공데이터포털 형식)
    private KmaWeather parseUltraSrtNcstResponse(String response) throws JSONException {
        KmaWeather weather = new KmaWeather();

        try {
            // JSON 응답 파싱 (공공데이터포털 형식)
            JSONObject jsonObject = new JSONObject(response);

            // 응답 코드 확인
            JSONObject responseObj = jsonObject.getJSONObject("response");
            JSONObject header = responseObj.getJSONObject("header");
            String resultCode = header.getString("resultCode");

            if (!"00".equals(resultCode)) {
                Log.e(TAG, "API 오류 응답: " + resultCode + " - " + header.getString("resultMsg"));
                return weather;
            }

            // 현재 날짜와 시간 설정
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.KOREA);
            Date now = new Date();
            weather.setBaseDate(dateFormat.format(now));
            weather.setBaseTime(timeFormat.format(now));

            // 데이터 파싱
            JSONObject body = responseObj.getJSONObject("body");
            JSONObject items = body.getJSONObject("items");
            JSONArray itemArray = items.getJSONArray("item");

            // 각 항목별 값 설정
            for (int i = 0; i < itemArray.length(); i++) {
                JSONObject item = itemArray.getJSONObject(i);
                String category = item.getString("category");
                String obsrValue = item.getString("obsrValue");

                processWeatherCategory(weather, category, obsrValue);
            }

        } catch (Exception e) {
            Log.e(TAG, "응답 파싱 실패", e);
        }

        // 우산 필요 여부 결정
        String weatherCondition = weather.getWeatherCondition();
        boolean hasRainOrSnow = weatherCondition != null &&
                (weatherCondition.contains("Rain") || weatherCondition.contains("Snow"));

        weather.setNeedUmbrella(weather.getPrecipitation() > 0 || hasRainOrSnow);

        return weather;
    }

    // XML 응답 파싱
    private KmaWeather parseXmlResponse(String xmlResponse) {
        KmaWeather weather = new KmaWeather();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xmlResponse));
            Document doc = builder.parse(is);

            // 응답 코드 확인
            NodeList resultCodeList = doc.getElementsByTagName("resultCode");
            if (resultCodeList.getLength() > 0) {
                String resultCode = resultCodeList.item(0).getTextContent();
                Log.d(TAG, "API 응답 코드: " + resultCode);

                // 응답이 성공이 아니면 빈 객체 반환
                if (!"00".equals(resultCode)) {
                    Log.e(TAG, "API 오류 응답: " + resultCode);
                    return weather;
                }
            }

            // 아이템 목록 가져오기
            NodeList itemList = doc.getElementsByTagName("item");

            // 발표 일시 설정
            if (itemList.getLength() > 0) {
                Element firstItem = (Element) itemList.item(0);
                NodeList baseDateList = firstItem.getElementsByTagName("baseDate");
                NodeList baseTimeList = firstItem.getElementsByTagName("baseTime");

                if (baseDateList.getLength() > 0) {
                    weather.setBaseDate(baseDateList.item(0).getTextContent());
                }

                if (baseTimeList.getLength() > 0) {
                    weather.setBaseTime(baseTimeList.item(0).getTextContent());
                }
            }

            // 각 항목별 값 설정
            for (int i = 0; i < itemList.getLength(); i++) {
                Element item = (Element) itemList.item(i);
                NodeList categoryList = item.getElementsByTagName("category");
                NodeList valueList = item.getElementsByTagName("obsrValue");

                if (categoryList.getLength() > 0 && valueList.getLength() > 0) {
                    String category = categoryList.item(0).getTextContent();
                    String value = valueList.item(0).getTextContent();

                    Log.d(TAG, "현재 날씨 카테고리 파싱: " + category + " = " + value);
                    processWeatherCategory(weather, category, value);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            Log.e(TAG, "XML 파싱 실패", e);
        }

        return weather;
    }

    // 날씨 카테고리 처리
    private void processWeatherCategory(KmaWeather weather, String category, String value) {
        try {
            switch (category) {
                case "T1H": // 기온
                    float temperature = Float.parseFloat(value);
                    weather.setTemperature(temperature);
                    Log.d(TAG, "온도 설정: " + temperature + "°C");
                    break;
                case "RN1": // 1시간 강수량
                    weather.setPrecipitation(Float.parseFloat(value));
                    break;
                case "REH": // 습도
                    weather.setHumidity(Integer.parseInt(value));
                    break;
                case "WSD": // 풍속
                    weather.setWindSpeed(Float.parseFloat(value));
                    break;
                case "PTY": // 강수형태 (0:없음, 1:비, 2:비/눈, 3:눈, 4:소나기)
                    int precipitationType = Integer.parseInt(value);
                    weather.setPrecipitationType(precipitationType);
                    Log.d(TAG, "강수형태 설정: " + precipitationType + " (시간: " + weather.getBaseTime() + ")");
                    // 강수 형태에 따라 날씨 상태 설정
                    if (precipitationType == 0) {
                        weather.setWeatherCondition("Clear");
                        Log.d(TAG, "날씨 상태 설정: Clear (강수형태: " + precipitationType + ")");
                    } else if (precipitationType == 1 || precipitationType == 4) {
                        weather.setWeatherCondition("Rain");
                        Log.d(TAG, "날씨 상태 설정: Rain (강수형태: " + precipitationType + ")");
                    } else if (precipitationType == 2) {
                        weather.setWeatherCondition("Rain/Snow");
                        Log.d(TAG, "날씨 상태 설정: Rain/Snow (강수형태: " + precipitationType + ")");
                    } else if (precipitationType == 3) {
                        weather.setWeatherCondition("Snow");
                        Log.d(TAG, "날씨 상태 설정: Snow (강수형태: " + precipitationType + ")");
                    }
                    break;
                case "SKY": // 하늘상태 (1:맑음, 3:구름많음, 4:흐림)
                    int skyCondition = Integer.parseInt(value);
                    Log.d(TAG, "하늘상태 설정: " + skyCondition + " (시간: " + weather.getBaseTime() + ")");
                    // 강수가 없는 경우에만 하늘 상태로 날씨 상태 설정
                    if (weather.getPrecipitationType() == 0) {
                        if (skyCondition == 1) {
                            weather.setWeatherCondition("Clear");
                            Log.d(TAG, "날씨 상태 설정: Clear (하늘상태: " + skyCondition + ")");
                        } else if (skyCondition == 3) {
                            weather.setWeatherCondition("Partly Cloudy");
                            Log.d(TAG, "날씨 상태 설정: Partly Cloudy (하늘상태: " + skyCondition + ")");
                        } else if (skyCondition == 4) {
                            weather.setWeatherCondition("Clouds");
                            Log.d(TAG, "날씨 상태 설정: Clouds (하늘상태: " + skyCondition + ")");
                        }
                    } else {
                        Log.d(TAG, "강수가 있어서 하늘상태 무시 (강수형태: " + weather.getPrecipitationType() + ")");
                    }
                    break;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "날씨 데이터 변환 실패: " + category + "=" + value, e);
        }
    }



    // 초단기예보 응답 파싱 (기상청 API 허브 형식)
    private List<KmaForecast> parseUltraSrtFcstResponse(String response) throws JSONException {
        List<KmaForecast> forecasts = new ArrayList<>();

        try {
            // JSON 응답 파싱 (공공데이터포털 형식)
            JSONObject jsonObject = new JSONObject(response);

            // 응답 코드 확인
            JSONObject responseObj = jsonObject.getJSONObject("response");
            JSONObject header = responseObj.getJSONObject("header");
            String resultCode = header.getString("resultCode");

            if (!"00".equals(resultCode)) {
                Log.e(TAG, "API 오류 응답: " + resultCode + " - " + header.getString("resultMsg"));
                return forecasts;
            }

            // 데이터 파싱
            JSONObject body = responseObj.getJSONObject("body");
            JSONObject items = body.getJSONObject("items");
            JSONArray itemArray = items.getJSONArray("item");

            // 시간별로 예보 데이터 그룹화
            String currentFcstDate = "";
            String currentFcstTime = "";
            KmaForecast currentForecast = null;

            for (int i = 0; i < itemArray.length(); i++) {
                JSONObject item = itemArray.getJSONObject(i);
                String fcstDate = item.getString("fcstDate");
                String fcstTime = item.getString("fcstTime");
                String category = item.getString("category");
                String fcstValue = item.getString("fcstValue");

                // 새로운 시간대 예보 시작
                if (!fcstDate.equals(currentFcstDate) || !fcstTime.equals(currentFcstTime)) {
                    currentFcstDate = fcstDate;
                    currentFcstTime = fcstTime;
                    currentForecast = new KmaForecast();
                    currentForecast.setForecastDate(fcstDate);
                    currentForecast.setForecastTime(fcstTime);
                    forecasts.add(currentForecast);
                }

                processForecastCategory(currentForecast, category, fcstValue);
            }

        } catch (Exception e) {
            Log.e(TAG, "예보 응답 파싱 실패", e);
        }

        return forecasts;
    }

    // XML 예보 응답 파싱
    private List<KmaForecast> parseXmlForecastResponse(String xmlResponse) {
        List<KmaForecast> forecasts = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xmlResponse));
            Document doc = builder.parse(is);

            // 응답 코드 확인
            NodeList resultCodeList = doc.getElementsByTagName("resultCode");
            if (resultCodeList.getLength() > 0) {
                String resultCode = resultCodeList.item(0).getTextContent();
                Log.d(TAG, "API 응답 코드: " + resultCode);

                // 응답이 성공이 아니면 빈 리스트 반환
                if (!"00".equals(resultCode)) {
                    Log.e(TAG, "API 오류 응답: " + resultCode);
                    return forecasts;
                }
            }

            // 아이템 목록 가져오기
            NodeList itemList = doc.getElementsByTagName("item");

            // 시간별로 예보 데이터 그룹화
            String currentFcstDate = "";
            String currentFcstTime = "";
            KmaForecast currentForecast = null;

            for (int i = 0; i < itemList.getLength(); i++) {
                Element item = (Element) itemList.item(i);

                NodeList fcstDateList = item.getElementsByTagName("fcstDate");
                NodeList fcstTimeList = item.getElementsByTagName("fcstTime");
                NodeList categoryList = item.getElementsByTagName("category");
                NodeList valueList = item.getElementsByTagName("fcstValue");

                if (fcstDateList.getLength() > 0 && fcstTimeList.getLength() > 0 &&
                    categoryList.getLength() > 0 && valueList.getLength() > 0) {

                    String fcstDate = fcstDateList.item(0).getTextContent();
                    String fcstTime = fcstTimeList.item(0).getTextContent();
                    String category = categoryList.item(0).getTextContent();
                    String value = valueList.item(0).getTextContent();

                    // 모든 카테고리 로그 출력
                    Log.d(TAG, "📋 API 응답 카테고리: " + category + " = " + value + " (시간: " + fcstTime + ")");

                    // 온도 카테고리 특별 로그
                    if ("TMP".equals(category) || "T1H".equals(category)) {
                        Log.w(TAG, "🌡️ 온도 카테고리 발견! " + category + " = " + value + "°C (시간: " + fcstTime + ")");
                    }

                    // 새로운 시간대 예보 시작
                    if (!fcstDate.equals(currentFcstDate) || !fcstTime.equals(currentFcstTime)) {
                        currentFcstDate = fcstDate;
                        currentFcstTime = fcstTime;
                        currentForecast = new KmaForecast();
                        currentForecast.setForecastDate(fcstDate);
                        currentForecast.setForecastTime(fcstTime);
                        forecasts.add(currentForecast);
                        Log.d(TAG, "🕐 새로운 시간대 예보 생성: " + fcstDate + " " + fcstTime);
                    }

                    processForecastCategory(currentForecast, category, value);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            Log.e(TAG, "XML 예보 파싱 실패", e);
        }

        return forecasts;
    }

    // 단기예보 응답 파싱 (공공데이터포털 형식)
    private List<KmaForecast> parseVilageFcstResponse(String response) throws JSONException {
        // 단기예보도 초단기예보와 동일한 형식으로 파싱
        return parseUltraSrtFcstResponse(response);
    }

    // 예보 카테고리 처리
    private void processForecastCategory(KmaForecast forecast, String category, String value) {
        try {
            switch (category) {
                case "T1H": // 기온 (초단기예보)
                case "TMP": // 기온 (단기예보)
                    float temperature = Float.parseFloat(value);
                    forecast.setTemperature(temperature);
                    Log.d(TAG, "예보 온도 설정: " + temperature + "°C (시간: " + forecast.getForecastTime() + ", 카테고리: " + category + ")");
                    break;
                case "RN1": // 1시간 강수량
                    // 강수량이 "강수없음"인 경우 0으로 처리
                    if (value.equals("강수없음")) {
                        forecast.setPrecipitation(0);
                    } else {
                        try {
                            forecast.setPrecipitation(Float.parseFloat(value));
                        } catch (NumberFormatException e) {
                            forecast.setPrecipitation(0);
                        }
                    }
                    break;
                case "REH": // 습도
                    forecast.setHumidity(Integer.parseInt(value));
                    break;
                case "WSD": // 풍속
                    forecast.setWindSpeed(Float.parseFloat(value));
                    break;
                case "PTY": // 강수형태 (0:없음, 1:비, 2:비/눈, 3:눈, 4:소나기)
                    int precipitationType = Integer.parseInt(value);
                    forecast.setPrecipitationType(precipitationType);
                    Log.d(TAG, "예보 강수형태 설정: " + precipitationType + " (시간: " + forecast.getForecastTime() + ")");
                    // 강수 형태에 따라 날씨 상태 설정
                    if (precipitationType == 0) {
                        forecast.setWeatherCondition("Clear");
                        Log.d(TAG, "예보 날씨 상태 설정: Clear (강수형태: " + precipitationType + ")");
                    } else if (precipitationType == 1 || precipitationType == 4) {
                        forecast.setWeatherCondition("Rain");
                        Log.d(TAG, "예보 날씨 상태 설정: Rain (강수형태: " + precipitationType + ")");
                    } else if (precipitationType == 2) {
                        forecast.setWeatherCondition("Rain/Snow");
                        Log.d(TAG, "예보 날씨 상태 설정: Rain/Snow (강수형태: " + precipitationType + ")");
                    } else if (precipitationType == 3) {
                        forecast.setWeatherCondition("Snow");
                        Log.d(TAG, "예보 날씨 상태 설정: Snow (강수형태: " + precipitationType + ")");
                    }
                    break;
                case "SKY": // 하늘상태 (1:맑음, 3:구름많음, 4:흐림)
                    int skyCondition = Integer.parseInt(value);
                    Log.d(TAG, "예보 하늘상태 설정: " + skyCondition + " (시간: " + forecast.getForecastTime() + ")");
                    // 강수가 없는 경우에만 하늘 상태로 날씨 상태 설정
                    if (forecast.getPrecipitationType() == 0) {
                        if (skyCondition == 1) {
                            forecast.setWeatherCondition("Clear");
                            Log.d(TAG, "예보 날씨 상태 설정: Clear (하늘상태: " + skyCondition + ")");
                        } else if (skyCondition == 3) {
                            forecast.setWeatherCondition("Partly Cloudy");
                            Log.d(TAG, "예보 날씨 상태 설정: Partly Cloudy (하늘상태: " + skyCondition + ")");
                        } else if (skyCondition == 4) {
                            forecast.setWeatherCondition("Clouds");
                            Log.d(TAG, "예보 날씨 상태 설정: Clouds (하늘상태: " + skyCondition + ")");
                        }
                    } else {
                        Log.d(TAG, "예보 강수가 있어서 하늘상태 무시 (강수형태: " + forecast.getPrecipitationType() + ")");
                    }
                    break;
                case "POP": // 강수확률
                    forecast.setPrecipitationProbability(Integer.parseInt(value));
                    break;
            }

            // 우산 필요 여부 결정
            String weatherCondition = forecast.getWeatherCondition();
            boolean hasRainOrSnow = weatherCondition != null &&
                    (weatherCondition.contains("Rain") || weatherCondition.contains("Snow"));

            forecast.setNeedUmbrella(forecast.getPrecipitation() > 0 ||
                    forecast.getPrecipitationProbability() >= 40 ||
                    hasRainOrSnow);
        } catch (NumberFormatException e) {
            Log.e(TAG, "예보 데이터 변환 실패: " + category + "=" + value, e);
        }
    }





    /**
     * 기상청 날씨 데이터를 앱 Weather 모델로 변환
     */
    public Weather convertToWeather(KmaWeather kmaWeather, double latitude, double longitude) {
        // 위치 문자열 생성
        String locationStr = latitude + "," + longitude;

        // 현재 시간
        long timestamp = System.currentTimeMillis();

        // 날씨 상태가 null인 경우 기본값 설정
        String weatherCondition = kmaWeather.getWeatherCondition();
        if (weatherCondition == null) {
            weatherCondition = "Clear"; // 기본값으로 맑음 설정
        }

        return new Weather(0, kmaWeather.getTemperature(), weatherCondition,
                kmaWeather.getPrecipitation(), kmaWeather.getHumidity(), kmaWeather.getWindSpeed(),
                locationStr, timestamp, kmaWeather.isNeedUmbrella());
    }
}
