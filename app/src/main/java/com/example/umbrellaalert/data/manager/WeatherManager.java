package com.example.umbrellaalert.data.manager;

import android.content.Context;
import android.util.Log;

import com.example.umbrellaalert.data.api.KmaApiClient;
import com.example.umbrellaalert.data.database.DatabaseHelper;
import com.example.umbrellaalert.data.database.WeatherDao;
import com.example.umbrellaalert.data.model.KmaForecast;
import com.example.umbrellaalert.data.model.KmaWeather;
import com.example.umbrellaalert.data.model.Location;
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.util.CoordinateConverter;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WeatherManager {

    private static final String TAG = "WeatherManager";
    private static final long CACHE_DURATION = TimeUnit.HOURS.toMillis(1); // 1시간 캐시

    private static WeatherManager instance;
    private WeatherDao weatherDao;
    private Context context;
    private ExecutorService executorService;

    // 싱글톤 패턴
    public static synchronized WeatherManager getInstance(Context context) {
        if (instance == null) {
            instance = new WeatherManager(context.getApplicationContext());
        }
        return instance;
    }

    private WeatherManager(Context context) {
        this.context = context.getApplicationContext();
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        weatherDao = new WeatherDao(dbHelper);
        executorService = Executors.newCachedThreadPool();

        // 오래된 데이터 정리
        cleanupOldData();
    }

    // 현재 위치의 날씨 가져오기
    public void getCurrentWeather(double latitude, double longitude, WeatherCallback callback) {
        // 위치 문자열 생성
        String locationStr = latitude + "," + longitude;

        // 캐시된 데이터 확인
        Weather cachedWeather = weatherDao.getLatestWeatherByLocation(locationStr);

        // 캐시가 유효한지 확인
        if (cachedWeather != null &&
                System.currentTimeMillis() - cachedWeather.getTimestamp() < CACHE_DURATION) {
            Log.d(TAG, "Using cached weather data");
            callback.onSuccess(cachedWeather);
            return;
        }

        // 새로운 데이터 가져오기
        // 위도/경도를 기상청 격자 좌표로 변환
        CoordinateConverter.GridCoordinate gridCoord = CoordinateConverter.convertToGrid(latitude, longitude);
        int nx = gridCoord.nx;
        int ny = gridCoord.ny;

        Log.d(TAG, "Fetching weather data from KMA API for coordinates: nx=" + nx + ", ny=" + ny);

        // KmaApiClient를 사용하여 날씨 정보 조회 (초단기예보에서 현재 온도 가져오기)
        KmaApiClient apiClient = KmaApiClient.getInstance(context);

        Future<List<KmaForecast>> forecastFuture = apiClient.getUltraSrtFcst(nx, ny);

        // 백그라운드에서 API 응답 처리
        executorService.execute(() -> {
            try {
                // API 응답 대기 (최대 8초로 단축)
                List<KmaForecast> forecasts = forecastFuture.get(8, TimeUnit.SECONDS);

                if (forecasts != null && !forecasts.isEmpty()) {
                    // 온도 정보가 있는 첫 번째 예보 데이터를 현재 날씨로 사용
                    KmaForecast currentForecast = null;
                    for (KmaForecast forecast : forecasts) {
                        if (forecast.getTemperature() > 0) {
                            currentForecast = forecast;
                            break;
                        }
                    }

                    if (currentForecast == null) {
                        currentForecast = forecasts.get(0); // 온도 정보가 없으면 첫 번째 사용
                    }

                    // KmaForecast를 Weather 객체로 변환
                    Weather weather = convertKmaForecastToWeather(currentForecast, locationStr);

                    // 데이터베이스에 저장
                    try {
                        long id = weatherDao.insertWeather(weather);
                        weather.setId((int) id);
                        Log.d(TAG, "Weather data saved to database with ID: " + id);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to save weather data to database", e);
                    }

                    // 성공 콜백 호출
                    callback.onSuccess(weather);
                } else {
                    Log.w(TAG, "KMA API returned null weather data");
                    handleWeatherError(callback, cachedWeather, latitude, longitude);
                }
            } catch (TimeoutException e) {
                Log.e(TAG, "KMA API request timeout", e);
                handleWeatherError(callback, cachedWeather, latitude, longitude);
            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch weather data from KMA API", e);
                handleWeatherError(callback, cachedWeather, latitude, longitude);
            }
        });
    }

    // 기본 날씨 정보 생성
    private Weather createDefaultWeather(double latitude, double longitude) {
        String locationStr = latitude + "," + longitude;
        long timestamp = System.currentTimeMillis();

        // 기본 날씨 정보 생성
        Weather defaultWeather = new Weather(
                0,                  // id
                20.0f,              // 기본 온도 20도
                "Clear",            // 맑음
                0.0f,               // 강수량 없음
                50,                 // 습도 50%
                1.0f,               // 풍속 1m/s
                locationStr,        // 위치
                timestamp,          // 현재 시간
                false               // 우산 필요 없음
        );

        // 데이터베이스에 저장
        try {
            long id = weatherDao.insertWeather(defaultWeather);
            defaultWeather.setId((int) id);
        } catch (Exception e) {
            Log.e(TAG, "기본 날씨 정보 저장 실패", e);
        }

        Log.d(TAG, "Created default weather data");
        return defaultWeather;
    }

    // 오래된 데이터 정리
    private void cleanupOldData() {
        // 24시간 이상 지난 데이터 삭제
        long threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        int deletedRows = weatherDao.deleteOldWeatherData(threshold);
        Log.d(TAG, "Deleted " + deletedRows + " old weather records");
    }

    // 우산 필요 여부 판단 메시지 생성
    public String getCatMessage(Weather weather) {
        if (weather == null) {
            return "날씨 정보를 가져올 수 없다냥...";
        }

        // 강수 확률을 기반으로 메시지 생성
        if (weather.isNeedUmbrella()) {
            if (weather.getPrecipitation() > 5) {
                return "비가 많이 올 예정이다냥! 우산을 꼭 챙겨라냥!";
            } else {
                return "우산을 챙겨야 할 것 같다냥!";
            }
        } else {
            // 날씨 상태에 따른 메시지
            String condition = weather.getWeatherCondition();

            // 날씨 상태가 null인 경우 처리
            if (condition == null) {
                return "오늘은 우산이 필요 없을 것 같다냥!";
            }

            if (condition.equalsIgnoreCase("Clear")) {
                return "오늘은 맑은 하루다냥~";
            } else if (condition.equalsIgnoreCase("Clouds")) {
                return "구름이 조금 있지만 비는 안 올 것 같다냥~";
            } else {
                return "오늘은 우산이 필요 없을 것 같다냥!";
            }
        }
    }

    // 우산 알림 서비스를 위한 모든 위치에 대한 날씨 체크
    public void checkAllLocationsWeather(List<Location> locations, WeatherCheckCallback callback) {
        // TODO: 모든 위치에 대한 날씨 확인 구현
    }

    // KmaWeather를 Weather 객체로 변환
    private Weather convertKmaWeatherToWeather(KmaWeather kmaWeather, String locationStr) {
        long timestamp = System.currentTimeMillis();

        // 우산 필요 여부 판단 (강수량 또는 강수 타입 기준)
        boolean needUmbrella = kmaWeather.getPrecipitation() > 0 ||
                              kmaWeather.getPrecipitationType() > 0;

        Weather weather = new Weather(
                0,  // id (데이터베이스 저장 시 자동 생성)
                kmaWeather.getTemperature(),
                kmaWeather.getWeatherCondition(),
                kmaWeather.getPrecipitation(),
                kmaWeather.getHumidity(),
                kmaWeather.getWindSpeed(),
                locationStr,
                timestamp,
                needUmbrella
        );

        Log.d(TAG, "Converted KmaWeather to Weather: temp=" + weather.getTemperature() +
                  ", condition=" + weather.getWeatherCondition() +
                  ", needUmbrella=" + weather.isNeedUmbrella());

        return weather;
    }

    // KmaForecast를 Weather 객체로 변환
    private Weather convertKmaForecastToWeather(KmaForecast kmaForecast, String locationStr) {
        long timestamp = System.currentTimeMillis();

        // 우산 필요 여부 판단 (강수량 또는 강수 타입 기준)
        boolean needUmbrella = kmaForecast.getPrecipitation() > 0 ||
                              kmaForecast.getPrecipitationType() > 0 ||
                              kmaForecast.getPrecipitationProbability() >= 40;

        Weather weather = new Weather(
                0,  // id (데이터베이스 저장 시 자동 생성)
                kmaForecast.getTemperature(),
                kmaForecast.getWeatherCondition(),
                kmaForecast.getPrecipitation(),
                kmaForecast.getHumidity(),
                kmaForecast.getWindSpeed(),
                locationStr,
                timestamp,
                needUmbrella
        );

        Log.d(TAG, "✅ 실제 API 데이터 변환 완료: temp=" + weather.getTemperature() +
                  "°C, condition=" + weather.getWeatherCondition() +
                  ", needUmbrella=" + weather.isNeedUmbrella());

        return weather;
    }

    // 에러 처리 헬퍼 메서드
    private void handleWeatherError(WeatherCallback callback, Weather cachedWeather, double latitude, double longitude) {
        if (cachedWeather != null) {
            Log.d(TAG, "Using cached weather data due to API error");
            callback.onSuccess(cachedWeather);
        } else {
            Log.d(TAG, "Creating default weather data due to API error");
            callback.onSuccess(createDefaultWeather(latitude, longitude));
        }
    }

    // 레거시 메서드들 제거됨

    // 콜백 인터페이스
    public interface WeatherCallback {
        void onSuccess(Weather weather);
        void onError(String error);
    }

    public interface WeatherCheckCallback {
        void onWeatherCheckCompleted(boolean anyLocationNeedsUmbrella);
    }
}