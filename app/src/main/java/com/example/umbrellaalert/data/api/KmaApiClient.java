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

    // API 상수 (기상청 API 허브 주소)
    private static final String BASE_URL = "https://apihub.kma.go.kr/api/typ01/cgi-bin/url/nph-url_readtop.cgi";
    private static final String ULTRA_SRT_NCST_URL = BASE_URL + "?rtype=json&mode=current"; // 초단기실황
    private static final String ULTRA_SRT_FCST_URL = BASE_URL + "?rtype=json&mode=fcst"; // 초단기예보
    private static final String VILAGE_FCST_URL = BASE_URL + "?rtype=json&mode=fcst3"; // 단기예보

    // API 키 (ApiKeyUtil에서 로드)

    // 싱글톤 인스턴스
    private static KmaApiClient instance;
    private final ExecutorService executorService;
    private final Context context;
    private String apiKey;

    // API 타입 열거형
    public enum ApiType {
        ULTRA_SRT_NCST,    // 초단기실황
        ULTRA_SRT_FCST,    // 초단기예보
        VILAGE_FCST        // 단기예보
    }

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
     * 초단기실황 조회 (현재 날씨)
     * @param nx X 좌표 (기상청 격자 좌표)
     * @param ny Y 좌표 (기상청 격자 좌표)
     * @return 날씨 정보
     */
    public Future<KmaWeather> getUltraSrtNcst(int nx, int ny) {
        return executorService.submit(new Callable<KmaWeather>() {
            @Override
            public KmaWeather call() throws Exception {
                try {
                    // 현재 시간 기준 가장 최근 발표 시각 계산
                    String baseDate = getCurrentDate();
                    String baseTime = getBaseTimeForUltraSrtNcst();

                    Log.d(TAG, "초단기실황 조회 - 기준일자: " + baseDate + ", 기준시각: " + baseTime);
                    Log.d(TAG, "초단기실황 조회 - 좌표: nx=" + nx + ", ny=" + ny);

                    // API 요청 URL 생성
                    String urlStr = buildApiUrl(ULTRA_SRT_NCST_URL, baseDate, baseTime, nx, ny);

                    // API 호출
                    String response = requestApi(urlStr);

                    // 응답 내용 로그 출력 (처음 500자만)
                    String logResponse = response.length() > 500 ? response.substring(0, 500) + "..." : response;
                    Log.d(TAG, "초단기실황 응답 내용 미리보기: " + logResponse);

                    // 응답 파싱
                    KmaWeather weather = parseUltraSrtNcstResponse(response);
                    return weather;
                } catch (Exception e) {
                    Log.e(TAG, "초단기실황 조회 실패", e);

                    // 기본 날씨 정보 반환
                    KmaWeather defaultWeather = new KmaWeather();
                    setDefaultWeatherValues(defaultWeather);
                    return defaultWeather;
                }
            }
        });
    }

    /**
     * 초단기예보 조회 (향후 6시간 예보)
     * @param nx X 좌표 (기상청 격자 좌표)
     * @param ny Y 좌표 (기상청 격자 좌표)
     * @return 예보 목록
     */
    public Future<List<KmaForecast>> getUltraSrtFcst(int nx, int ny) {
        return executorService.submit(new Callable<List<KmaForecast>>() {
            @Override
            public List<KmaForecast> call() throws Exception {
                try {
                    // 현재 시간 기준 가장 최근 발표 시각 계산
                    String baseDate = getCurrentDate();
                    String baseTime = getBaseTimeForUltraSrtFcst();

                    // API 요청 URL 생성
                    String urlStr = buildApiUrl(ULTRA_SRT_FCST_URL, baseDate, baseTime, nx, ny);

                    // API 호출
                    String response = requestApi(urlStr);

                    // 응답 파싱
                    return parseUltraSrtFcstResponse(response);
                } catch (Exception e) {
                    Log.e(TAG, "초단기예보 조회 실패", e);
                    throw e;
                }
            }
        });
    }

    /**
     * 단기예보 조회 (3일 예보)
     * @param nx X 좌표 (기상청 격자 좌표)
     * @param ny Y 좌표 (기상청 격자 좌표)
     * @return 예보 목록
     */
    public Future<List<KmaForecast>> getVilageFcst(int nx, int ny) {
        return executorService.submit(new Callable<List<KmaForecast>>() {
            @Override
            public List<KmaForecast> call() throws Exception {
                try {
                    // 현재 시간 기준 가장 최근 발표 시각 계산
                    String baseDate = getCurrentDate();
                    String baseTime = getBaseTimeForVilageFcst();

                    // API 요청 URL 생성
                    String urlStr = buildApiUrl(VILAGE_FCST_URL, baseDate, baseTime, nx, ny);

                    // API 호출
                    String response = requestApi(urlStr);

                    // 응답 파싱
                    return parseVilageFcstResponse(response);
                } catch (Exception e) {
                    Log.e(TAG, "단기예보 조회 실패", e);
                    throw e;
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

    // API URL 생성 (기상청 API 허브 형식)
    private String buildApiUrl(String baseUrl, String baseDate, String baseTime, int nx, int ny) throws IOException {
        // 기상청 API 허브는 다른 파라미터 형식을 사용함
        // baseUrl에 이미 기본 파라미터(rtype, mode)가 포함되어 있음
        StringBuilder urlBuilder = new StringBuilder(baseUrl);

        // API 키 추가
        urlBuilder.append("&key=").append(apiKey);

        // 지점 코드 추가 (nx, ny 대신 지점 코드 사용)
        // 서울 지점 코드: 108 (임시로 서울 사용)
        urlBuilder.append("&code=108");

        // 요청 시간 추가 (선택적)
        // urlBuilder.append("&date=").append(baseDate);
        // urlBuilder.append("&time=").append(baseTime);

        String url = urlBuilder.toString();
        Log.d(TAG, "API 요청 URL: " + url);
        return url;
    }

    // API 요청 실행
    private String requestApi(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");
        conn.setRequestProperty("Accept", "application/json");

        // 연결 타임아웃 설정
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

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

    // 초단기실황 응답 파싱 (기상청 API 허브 형식)
    private KmaWeather parseUltraSrtNcstResponse(String response) throws JSONException {
        KmaWeather weather = new KmaWeather();

        try {
            // JSON 응답 파싱 (기상청 API 허브는 JSON 형식만 지원)
            JSONObject jsonObject = new JSONObject(response);

            // 현재 날짜와 시간 설정
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.KOREA);
            Date now = new Date();
            weather.setBaseDate(dateFormat.format(now));
            weather.setBaseTime(timeFormat.format(now));

            // 기상청 API 허브 응답 형식에 맞게 파싱
            // 응답 형식은 실제 API 문서를 참고하여 수정 필요
            try {
                // 온도 (기온)
                if (jsonObject.has("temp")) {
                    weather.setTemperature((float) jsonObject.getDouble("temp"));
                }

                // 강수량
                if (jsonObject.has("rain")) {
                    weather.setPrecipitation((float) jsonObject.getDouble("rain"));
                }

                // 습도
                if (jsonObject.has("humi")) {
                    weather.setHumidity(jsonObject.getInt("humi"));
                }

                // 풍속
                if (jsonObject.has("wind")) {
                    weather.setWindSpeed((float) jsonObject.getDouble("wind"));
                }

                // 날씨 상태 (맑음, 흐림, 비 등)
                if (jsonObject.has("sky")) {
                    String sky = jsonObject.getString("sky");
                    if (sky.contains("맑음")) {
                        weather.setWeatherCondition("Clear");
                        weather.setPrecipitationType(0);
                    } else if (sky.contains("구름")) {
                        weather.setWeatherCondition("Clouds");
                        weather.setPrecipitationType(0);
                    } else if (sky.contains("비")) {
                        weather.setWeatherCondition("Rain");
                        weather.setPrecipitationType(1);
                    } else if (sky.contains("눈")) {
                        weather.setWeatherCondition("Snow");
                        weather.setPrecipitationType(3);
                    } else {
                        weather.setWeatherCondition("Clear");
                        weather.setPrecipitationType(0);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "JSON 필드 파싱 실패", e);
            }
        } catch (Exception e) {
            Log.e(TAG, "응답 파싱 실패", e);
            // 기본 날씨 정보 설정
            setDefaultWeatherValues(weather);
        }

        // 우산 필요 여부 결정
        weather.setNeedUmbrella(weather.getPrecipitation() > 0 ||
                weather.getWeatherCondition().contains("Rain") ||
                weather.getWeatherCondition().contains("Snow"));

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

                // 응답이 성공이 아니면 기본값 설정 후 반환
                if (!"00".equals(resultCode)) {
                    Log.e(TAG, "API 오류 응답: " + resultCode);
                    setDefaultWeatherValues(weather);
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

                    processWeatherCategory(weather, category, value);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            Log.e(TAG, "XML 파싱 실패", e);
            setDefaultWeatherValues(weather);
        }

        return weather;
    }

    // 날씨 카테고리 처리
    private void processWeatherCategory(KmaWeather weather, String category, String value) {
        try {
            switch (category) {
                case "T1H": // 기온
                    weather.setTemperature(Float.parseFloat(value));
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
                    // 강수 형태에 따라 날씨 상태 설정
                    if (precipitationType == 0) {
                        weather.setWeatherCondition("Clear");
                    } else if (precipitationType == 1 || precipitationType == 4) {
                        weather.setWeatherCondition("Rain");
                    } else if (precipitationType == 2) {
                        weather.setWeatherCondition("Rain/Snow");
                    } else if (precipitationType == 3) {
                        weather.setWeatherCondition("Snow");
                    }
                    break;
                case "SKY": // 하늘상태 (1:맑음, 3:구름많음, 4:흐림)
                    int skyCondition = Integer.parseInt(value);
                    // 강수가 없는 경우에만 하늘 상태로 날씨 상태 설정
                    if (weather.getPrecipitationType() == 0) {
                        if (skyCondition == 1) {
                            weather.setWeatherCondition("Clear");
                        } else if (skyCondition == 3) {
                            weather.setWeatherCondition("Partly Cloudy");
                        } else if (skyCondition == 4) {
                            weather.setWeatherCondition("Clouds");
                        }
                    }
                    break;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "날씨 데이터 변환 실패: " + category + "=" + value, e);
        }
    }

    // 기본 날씨 값 설정
    private void setDefaultWeatherValues(KmaWeather weather) {
        // 현재 날짜와 시간 설정
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.KOREA);
        Date now = new Date();

        weather.setBaseDate(dateFormat.format(now));
        weather.setBaseTime(timeFormat.format(now));
        weather.setTemperature(20.0f); // 기본 온도 20도
        weather.setPrecipitation(0.0f); // 강수량 없음
        weather.setHumidity(50); // 습도 50%
        weather.setWindSpeed(1.0f); // 풍속 1m/s
        weather.setPrecipitationType(0); // 강수 없음
        weather.setWeatherCondition("Clear"); // 맑음
        weather.setNeedUmbrella(false); // 우산 필요 없음
    }

    // 초단기예보 응답 파싱 (기상청 API 허브 형식)
    private List<KmaForecast> parseUltraSrtFcstResponse(String response) throws JSONException {
        List<KmaForecast> forecasts = new ArrayList<>();

        try {
            // JSON 응답 파싱 (기상청 API 허브는 JSON 형식만 지원)
            JSONObject jsonObject = new JSONObject(response);

            // 현재 날짜와 시간
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.KOREA);
            Date now = new Date();
            String currentDate = dateFormat.format(now);

            // 기상청 API 허브 응답 형식에 맞게 파싱
            // 응답 형식은 실제 API 문서를 참고하여 수정 필요
            try {
                // 예보 데이터가 있는 경우
                if (jsonObject.has("fcst") && jsonObject.get("fcst") instanceof JSONArray) {
                    JSONArray fcstArray = jsonObject.getJSONArray("fcst");

                    for (int i = 0; i < fcstArray.length(); i++) {
                        JSONObject fcstItem = fcstArray.getJSONObject(i);
                        KmaForecast forecast = new KmaForecast();

                        // 예보 일시 설정
                        forecast.setForecastDate(currentDate);
                        if (fcstItem.has("time")) {
                            forecast.setForecastTime(fcstItem.getString("time"));
                        } else {
                            // 시간 정보가 없으면 현재 시간 + i시간 후로 설정
                            Calendar cal = Calendar.getInstance();
                            cal.add(Calendar.HOUR_OF_DAY, i + 1);
                            forecast.setForecastTime(timeFormat.format(cal.getTime()));
                        }

                        // 온도
                        if (fcstItem.has("temp")) {
                            forecast.setTemperature((float) fcstItem.getDouble("temp"));
                        }

                        // 강수량
                        if (fcstItem.has("rain")) {
                            forecast.setPrecipitation((float) fcstItem.getDouble("rain"));
                        }

                        // 강수확률
                        if (fcstItem.has("rainp")) {
                            forecast.setPrecipitationProbability(fcstItem.getInt("rainp"));
                        }

                        // 습도
                        if (fcstItem.has("humi")) {
                            forecast.setHumidity(fcstItem.getInt("humi"));
                        }

                        // 풍속
                        if (fcstItem.has("wind")) {
                            forecast.setWindSpeed((float) fcstItem.getDouble("wind"));
                        }

                        // 날씨 상태
                        if (fcstItem.has("sky")) {
                            String sky = fcstItem.getString("sky");
                            if (sky.contains("맑음")) {
                                forecast.setWeatherCondition("Clear");
                                forecast.setPrecipitationType(0);
                            } else if (sky.contains("구름")) {
                                forecast.setWeatherCondition("Clouds");
                                forecast.setPrecipitationType(0);
                            } else if (sky.contains("비")) {
                                forecast.setWeatherCondition("Rain");
                                forecast.setPrecipitationType(1);
                            } else if (sky.contains("눈")) {
                                forecast.setWeatherCondition("Snow");
                                forecast.setPrecipitationType(3);
                            } else {
                                forecast.setWeatherCondition("Clear");
                                forecast.setPrecipitationType(0);
                            }
                        }

                        // 우산 필요 여부 결정
                        forecast.setNeedUmbrella(forecast.getPrecipitation() > 0 ||
                                forecast.getPrecipitationProbability() >= 40 ||
                                forecast.getWeatherCondition().contains("Rain") ||
                                forecast.getWeatherCondition().contains("Snow"));

                        forecasts.add(forecast);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "JSON 필드 파싱 실패", e);
            }
        } catch (Exception e) {
            Log.e(TAG, "예보 응답 파싱 실패", e);
            // 기본 예보 정보 추가
            addDefaultForecast(forecasts);
        }

        // 예보가 없으면 기본 예보 추가
        if (forecasts.isEmpty()) {
            addDefaultForecast(forecasts);
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

                // 응답이 성공이 아니면 기본값 설정 후 반환
                if (!"00".equals(resultCode)) {
                    Log.e(TAG, "API 오류 응답: " + resultCode);
                    addDefaultForecast(forecasts);
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

                    // 새로운 시간대 예보 시작
                    if (!fcstDate.equals(currentFcstDate) || !fcstTime.equals(currentFcstTime)) {
                        currentFcstDate = fcstDate;
                        currentFcstTime = fcstTime;
                        currentForecast = new KmaForecast();
                        currentForecast.setForecastDate(fcstDate);
                        currentForecast.setForecastTime(fcstTime);
                        forecasts.add(currentForecast);
                    }

                    processForecastCategory(currentForecast, category, value);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            Log.e(TAG, "XML 예보 파싱 실패", e);
            addDefaultForecast(forecasts);
        }

        return forecasts;
    }

    // 예보 카테고리 처리
    private void processForecastCategory(KmaForecast forecast, String category, String value) {
        try {
            switch (category) {
                case "T1H": // 기온
                    forecast.setTemperature(Float.parseFloat(value));
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
                    // 강수 형태에 따라 날씨 상태 설정
                    if (precipitationType == 0) {
                        forecast.setWeatherCondition("Clear");
                    } else if (precipitationType == 1 || precipitationType == 4) {
                        forecast.setWeatherCondition("Rain");
                    } else if (precipitationType == 2) {
                        forecast.setWeatherCondition("Rain/Snow");
                    } else if (precipitationType == 3) {
                        forecast.setWeatherCondition("Snow");
                    }
                    break;
                case "SKY": // 하늘상태 (1:맑음, 3:구름많음, 4:흐림)
                    int skyCondition = Integer.parseInt(value);
                    // 강수가 없는 경우에만 하늘 상태로 날씨 상태 설정
                    if (forecast.getPrecipitationType() == 0) {
                        if (skyCondition == 1) {
                            forecast.setWeatherCondition("Clear");
                        } else if (skyCondition == 3) {
                            forecast.setWeatherCondition("Partly Cloudy");
                        } else if (skyCondition == 4) {
                            forecast.setWeatherCondition("Clouds");
                        }
                    }
                    break;
                case "POP": // 강수확률
                    forecast.setPrecipitationProbability(Integer.parseInt(value));
                    break;
            }

            // 우산 필요 여부 결정
            forecast.setNeedUmbrella(forecast.getPrecipitation() > 0 ||
                    forecast.getPrecipitationProbability() >= 40 ||
                    forecast.getWeatherCondition().contains("Rain") ||
                    forecast.getWeatherCondition().contains("Snow"));
        } catch (NumberFormatException e) {
            Log.e(TAG, "예보 데이터 변환 실패: " + category + "=" + value, e);
        }
    }

    // 기본 예보 정보 추가
    private void addDefaultForecast(List<KmaForecast> forecasts) {
        // 현재 시간 기준으로 3시간 간격으로 기본 예보 3개 추가
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.KOREA);

        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();

        for (int i = 0; i < 3; i++) {
            KmaForecast forecast = new KmaForecast();
            forecast.setForecastDate(dateFormat.format(now));
            forecast.setForecastTime(timeFormat.format(now));
            forecast.setTemperature(20.0f); // 기본 온도 20도
            forecast.setPrecipitation(0.0f); // 강수량 없음
            forecast.setHumidity(50); // 습도 50%
            forecast.setWindSpeed(1.0f); // 풍속 1m/s
            forecast.setPrecipitationType(0); // 강수 없음
            forecast.setWeatherCondition("Clear"); // 맑음
            forecast.setPrecipitationProbability(0); // 강수확률 0%
            forecast.setNeedUmbrella(false); // 우산 필요 없음

            forecasts.add(forecast);

            // 3시간 추가
            cal.add(Calendar.HOUR_OF_DAY, 3);
            now = cal.getTime();
        }
    }

    // 단기예보 응답 파싱
    private List<KmaForecast> parseVilageFcstResponse(String response) throws JSONException {
        // 초단기예보와 동일한 방식으로 파싱
        return parseUltraSrtFcstResponse(response);
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
