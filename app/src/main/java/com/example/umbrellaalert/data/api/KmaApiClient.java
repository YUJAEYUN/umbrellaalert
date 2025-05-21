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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

/**
 * 기상청 API 클라이언트
 */
public class KmaApiClient {

    private static final String TAG = "KmaApiClient";

    // API 상수
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

                    // API 요청 URL 생성
                    String urlStr = buildApiUrl(ULTRA_SRT_NCST_URL, baseDate, baseTime, nx, ny);

                    // API 호출
                    String response = requestApi(urlStr);

                    // 응답 파싱
                    return parseUltraSrtNcstResponse(response);
                } catch (Exception e) {
                    Log.e(TAG, "초단기실황 조회 실패", e);
                    throw e;
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

        return new int[] {nx, ny};
    }

    // API URL 생성
    private String buildApiUrl(String baseUrl, String baseDate, String baseTime, int nx, int ny) throws IOException {
        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        urlBuilder.append("?").append(URLEncoder.encode("serviceKey", "UTF-8")).append("=").append(apiKey);
        urlBuilder.append("&").append(URLEncoder.encode("pageNo", "UTF-8")).append("=").append(URLEncoder.encode("1", "UTF-8"));
        urlBuilder.append("&").append(URLEncoder.encode("numOfRows", "UTF-8")).append("=").append(URLEncoder.encode("1000", "UTF-8"));
        urlBuilder.append("&").append(URLEncoder.encode("dataType", "UTF-8")).append("=").append(URLEncoder.encode("JSON", "UTF-8"));
        urlBuilder.append("&").append(URLEncoder.encode("base_date", "UTF-8")).append("=").append(URLEncoder.encode(baseDate, "UTF-8"));
        urlBuilder.append("&").append(URLEncoder.encode("base_time", "UTF-8")).append("=").append(URLEncoder.encode(baseTime, "UTF-8"));
        urlBuilder.append("&").append(URLEncoder.encode("nx", "UTF-8")).append("=").append(URLEncoder.encode(String.valueOf(nx), "UTF-8"));
        urlBuilder.append("&").append(URLEncoder.encode("ny", "UTF-8")).append("=").append(URLEncoder.encode(String.valueOf(ny), "UTF-8"));

        return urlBuilder.toString();
    }

    // API 요청 실행
    private String requestApi(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");

        BufferedReader rd;
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();

        return sb.toString();
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

    // 초단기실황 응답 파싱
    private KmaWeather parseUltraSrtNcstResponse(String response) throws JSONException {
        JSONObject jsonObject = new JSONObject(response);
        JSONObject responseObj = jsonObject.getJSONObject("response");
        JSONObject bodyObj = responseObj.getJSONObject("body");
        JSONObject itemsObj = bodyObj.getJSONObject("items");
        JSONArray itemArray = itemsObj.getJSONArray("item");

        KmaWeather weather = new KmaWeather();

        // 발표 일시 설정
        if (itemArray.length() > 0) {
            JSONObject firstItem = itemArray.getJSONObject(0);
            weather.setBaseDate(firstItem.getString("baseDate"));
            weather.setBaseTime(firstItem.getString("baseTime"));
        }

        // 각 항목별 값 설정
        for (int i = 0; i < itemArray.length(); i++) {
            JSONObject item = itemArray.getJSONObject(i);
            String category = item.getString("category");
            String value = item.getString("obsrValue");

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
        }

        // 우산 필요 여부 결정
        weather.setNeedUmbrella(weather.getPrecipitation() > 0 ||
                weather.getWeatherCondition().contains("Rain") ||
                weather.getWeatherCondition().contains("Snow"));

        return weather;
    }

    // 초단기예보 응답 파싱
    private List<KmaForecast> parseUltraSrtFcstResponse(String response) throws JSONException {
        JSONObject jsonObject = new JSONObject(response);
        JSONObject responseObj = jsonObject.getJSONObject("response");
        JSONObject bodyObj = responseObj.getJSONObject("body");
        JSONObject itemsObj = bodyObj.getJSONObject("items");
        JSONArray itemArray = itemsObj.getJSONArray("item");

        // 시간별로 예보 데이터 그룹화
        List<KmaForecast> forecasts = new ArrayList<>();
        String currentFcstDate = "";
        String currentFcstTime = "";
        KmaForecast currentForecast = null;

        for (int i = 0; i < itemArray.length(); i++) {
            JSONObject item = itemArray.getJSONObject(i);
            String fcstDate = item.getString("fcstDate");
            String fcstTime = item.getString("fcstTime");

            // 새로운 시간대 예보 시작
            if (!fcstDate.equals(currentFcstDate) || !fcstTime.equals(currentFcstTime)) {
                currentFcstDate = fcstDate;
                currentFcstTime = fcstTime;
                currentForecast = new KmaForecast();
                currentForecast.setForecastDate(fcstDate);
                currentForecast.setForecastTime(fcstTime);
                forecasts.add(currentForecast);
            }

            // 항목별 값 설정
            String category = item.getString("category");
            String value = item.getString("fcstValue");

            switch (category) {
                case "T1H": // 기온
                    currentForecast.setTemperature(Float.parseFloat(value));
                    break;
                case "RN1": // 1시간 강수량
                    // 강수량이 "강수없음"인 경우 0으로 처리
                    if (value.equals("강수없음")) {
                        currentForecast.setPrecipitation(0);
                    } else {
                        try {
                            currentForecast.setPrecipitation(Float.parseFloat(value));
                        } catch (NumberFormatException e) {
                            currentForecast.setPrecipitation(0);
                        }
                    }
                    break;
                case "REH": // 습도
                    currentForecast.setHumidity(Integer.parseInt(value));
                    break;
                case "WSD": // 풍속
                    currentForecast.setWindSpeed(Float.parseFloat(value));
                    break;
                case "PTY": // 강수형태 (0:없음, 1:비, 2:비/눈, 3:눈, 4:소나기)
                    int precipitationType = Integer.parseInt(value);
                    currentForecast.setPrecipitationType(precipitationType);
                    // 강수 형태에 따라 날씨 상태 설정
                    if (precipitationType == 0) {
                        currentForecast.setWeatherCondition("Clear");
                    } else if (precipitationType == 1 || precipitationType == 4) {
                        currentForecast.setWeatherCondition("Rain");
                    } else if (precipitationType == 2) {
                        currentForecast.setWeatherCondition("Rain/Snow");
                    } else if (precipitationType == 3) {
                        currentForecast.setWeatherCondition("Snow");
                    }
                    break;
                case "SKY": // 하늘상태 (1:맑음, 3:구름많음, 4:흐림)
                    int skyCondition = Integer.parseInt(value);
                    // 강수가 없는 경우에만 하늘 상태로 날씨 상태 설정
                    if (currentForecast.getPrecipitationType() == 0) {
                        if (skyCondition == 1) {
                            currentForecast.setWeatherCondition("Clear");
                        } else if (skyCondition == 3) {
                            currentForecast.setWeatherCondition("Partly Cloudy");
                        } else if (skyCondition == 4) {
                            currentForecast.setWeatherCondition("Clouds");
                        }
                    }
                    break;
                case "POP": // 강수확률
                    currentForecast.setPrecipitationProbability(Integer.parseInt(value));
                    break;
            }

            // 우산 필요 여부 결정
            currentForecast.setNeedUmbrella(currentForecast.getPrecipitation() > 0 ||
                    currentForecast.getPrecipitationProbability() >= 40 ||
                    currentForecast.getWeatherCondition().contains("Rain") ||
                    currentForecast.getWeatherCondition().contains("Snow"));
        }

        return forecasts;
    }

    // 단기예보 응답 파싱
    private List<KmaForecast> parseVilageFcstResponse(String response) throws JSONException {
        // 초단기예보와 유사한 방식으로 파싱
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

        return new Weather(0, kmaWeather.getTemperature(), kmaWeather.getWeatherCondition(),
                kmaWeather.getPrecipitation(), kmaWeather.getHumidity(), kmaWeather.getWindSpeed(),
                locationStr, timestamp, kmaWeather.isNeedUmbrella());
    }
}
