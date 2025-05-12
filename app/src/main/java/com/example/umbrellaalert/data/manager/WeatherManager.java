package com.example.umbrellaalert.data.manager;

import android.content.Context;
import android.util.Log;

import com.example.umbrellaalert.data.api.WeatherApiClient;
import com.example.umbrellaalert.data.database.DatabaseHelper;
import com.example.umbrellaalert.data.database.WeatherDao;
import com.example.umbrellaalert.data.model.Location;
import com.example.umbrellaalert.data.model.Weather;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class WeatherManager {

    private static final String TAG = "WeatherManager";
    private static final long CACHE_DURATION = TimeUnit.HOURS.toMillis(1); // 1시간 캐시

    private static WeatherManager instance;
    private WeatherDao weatherDao;
    private WeatherApiClient apiClient;

    // 싱글톤 패턴
    public static synchronized WeatherManager getInstance(Context context) {
        if (instance == null) {
            instance = new WeatherManager(context.getApplicationContext());
        }
        return instance;
    }

    private WeatherManager(Context context) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        weatherDao = new WeatherDao(dbHelper);
        apiClient = WeatherApiClient.getInstance();

        // 오래된 데이터 정리
        cleanupOldData();
    }

    // 현재 위치의 날씨 가져오기
    public Weather getCurrentWeather(double latitude, double longitude) {
        // 위치 문자열 생성
        String locationStr = latitude + "," + longitude;

        // 캐시된 데이터 확인
        Weather cachedWeather = weatherDao.getLatestWeatherByLocation(locationStr);

        // 캐시가 유효한지 확인
        if (cachedWeather != null &&
                System.currentTimeMillis() - cachedWeather.getTimestamp() < CACHE_DURATION) {
            Log.d(TAG, "Using cached weather data");
            return cachedWeather;
        }

        // 새로운 데이터 가져오기
        try {
            Future<Weather> weatherFuture = apiClient.getCurrentWeather(latitude, longitude);
            Weather freshWeather = weatherFuture.get(); // 결과 기다리기

            // 데이터베이스에 저장
            long id = weatherDao.insertWeather(freshWeather);
            freshWeather.setId((int) id);

            Log.d(TAG, "Retrieved fresh weather data");
            return freshWeather;
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error retrieving weather data", e);

            // 에러 발생 시 캐시된 데이터 반환 (없으면 null)
            return cachedWeather;
        }
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

    // 콜백 인터페이스
    public interface WeatherCheckCallback {
        void onWeatherCheckCompleted(boolean anyLocationNeedsUmbrella);
    }
}