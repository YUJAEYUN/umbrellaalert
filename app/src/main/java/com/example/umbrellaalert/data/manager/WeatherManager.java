package com.example.umbrellaalert.data.manager;

import android.content.Context;
import android.util.Log;

import com.example.umbrellaalert.data.api.SimpleKmaApiClient;
import com.example.umbrellaalert.data.database.DatabaseHelper;
import com.example.umbrellaalert.data.database.WeatherDao;
import com.example.umbrellaalert.data.model.Location;
import com.example.umbrellaalert.data.model.Weather;

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
    private SimpleKmaApiClient simpleApiClient;
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
        simpleApiClient = SimpleKmaApiClient.getInstance(context);
        executorService = Executors.newCachedThreadPool();

        // 오래된 데이터 정리
        cleanupOldData();
    }

    // 현재 위치의 날씨 가져오기 (새로운 간단한 API 사용)
    public void getCurrentWeather(double latitude, double longitude, WeatherCallback callback) {
        Log.d(TAG, "🌤️ 간단한 기상청 API허브로 날씨 데이터 요청 시작");

        // 새로운 간단한 API 클라이언트 사용
        Future<Weather> weatherFuture = simpleApiClient.getCurrentWeather(latitude, longitude);

        // 백그라운드에서 API 응답 처리
        executorService.execute(() -> {
            try {
                // API 응답 대기 (최대 10초)
                Weather weather = weatherFuture.get(10, TimeUnit.SECONDS);

                if (weather != null) {
                    // 데이터베이스에 저장
                    try {
                        long id = weatherDao.insertWeather(weather);
                        weather.setId((int) id);
                        Log.d(TAG, "✅ 날씨 데이터 DB 저장 완료: ID=" + id + ", 온도=" + weather.getTemperature() + "°C");
                    } catch (Exception e) {
                        Log.e(TAG, "날씨 데이터 DB 저장 실패", e);
                    }

                    // 성공 콜백 호출
                    callback.onSuccess(weather);
                } else {
                    Log.w(TAG, "⚠️ 기상청 API허브에서 null 데이터 반환");
                    handleWeatherError(callback, latitude, longitude);
                }
            } catch (TimeoutException e) {
                Log.e(TAG, "⏰ 기상청 API허브 요청 타임아웃", e);
                handleWeatherError(callback, latitude, longitude);
            } catch (Exception e) {
                Log.e(TAG, "❌ 기상청 API허브 요청 실패", e);
                handleWeatherError(callback, latitude, longitude);
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



    // 에러 처리 헬퍼 메서드
    private void handleWeatherError(WeatherCallback callback, double latitude, double longitude) {
        // 캐시된 데이터 확인
        String locationStr = latitude + "," + longitude;
        Weather cachedWeather = weatherDao.getLatestWeatherByLocation(locationStr);

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