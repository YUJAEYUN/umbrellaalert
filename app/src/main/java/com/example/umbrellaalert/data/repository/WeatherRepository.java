package com.example.umbrellaalert.data.repository;

import android.content.Context;

import com.example.umbrellaalert.data.database.DatabaseHelper;
import com.example.umbrellaalert.data.database.WeatherDao;
import com.example.umbrellaalert.data.api.KmaApiClient;
import com.example.umbrellaalert.data.model.KmaForecast;
import com.example.umbrellaalert.data.model.KmaWeather;
import com.example.umbrellaalert.data.model.Weather;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 날씨 데이터 관리를 위한 Repository 클래스
 * 데이터 소스(API, 로컬 DB)를 추상화하여 ViewModel에 제공
 */
public class WeatherRepository {

    private static final long CACHE_EXPIRATION_TIME = TimeUnit.HOURS.toMillis(1); // 1시간

    private final Context context;
    private final KmaApiClient kmaApiClient;
    private final WeatherDao weatherDao;

    // API 타입 (기본값: 초단기실황)
    private KmaApiClient.ApiType currentApiType = KmaApiClient.ApiType.ULTRA_SRT_NCST;

    public WeatherRepository(Context context) {
        this.context = context.getApplicationContext();
        this.kmaApiClient = KmaApiClient.getInstance(context);
        this.weatherDao = new WeatherDao(DatabaseHelper.getInstance(context));
    }

    /**
     * API 타입 설정
     * @param apiType API 타입 (초단기실황, 초단기예보, 단기예보)
     */
    public void setApiType(KmaApiClient.ApiType apiType) {
        this.currentApiType = apiType;
    }

    /**
     * 현재 위치의 날씨 정보 가져오기
     * 캐시된 데이터가 있으면 사용하고, 없거나 만료되었으면 API에서 새로 가져옴
     */
    public Weather getCurrentWeather(double latitude, double longitude) {
        try {
            // 위치 문자열 생성 (위도,경도)
            String locationKey = String.format("%f,%f", latitude, longitude);

            // 캐시된 날씨 데이터 확인
            Weather cachedWeather = weatherDao.getLatestWeatherByLocation(locationKey);

            // 캐시가 유효한지 확인
            if (isCacheValid(cachedWeather)) {
                return cachedWeather;
            }

            // 캐시가 없거나 만료된 경우 API에서 새로 가져옴
            // 위도/경도를 기상청 격자 좌표로 변환
            int[] gridCoord = kmaApiClient.convertToGridCoord(latitude, longitude);
            int nx = gridCoord[0];
            int ny = gridCoord[1];

            // API 타입에 따라 다른 API 호출
            Weather freshWeather = null;

            if (currentApiType == KmaApiClient.ApiType.ULTRA_SRT_NCST) {
                // 초단기실황 API 호출
                KmaWeather kmaWeather = kmaApiClient.getUltraSrtNcst(nx, ny).get();
                freshWeather = kmaApiClient.convertToWeather(kmaWeather, latitude, longitude);
            } else {
                // 예보 API는 현재 날씨만 필요한 경우 초단기실황으로 대체
                KmaWeather kmaWeather = kmaApiClient.getUltraSrtNcst(nx, ny).get();
                freshWeather = kmaApiClient.convertToWeather(kmaWeather, latitude, longitude);
            }

            // 새 데이터 캐싱
            if (freshWeather != null) {
                weatherDao.insertWeather(freshWeather);

                // 오래된 데이터 정리
                cleanupOldData();
            }

            return freshWeather;
        } catch (Exception e) {
            e.printStackTrace();

            // API 호출 실패 시 캐시된 데이터 반환 (있는 경우)
            String locationKey = String.format("%f,%f", latitude, longitude);
            Weather cachedWeather = weatherDao.getLatestWeatherByLocation(locationKey);
            if (cachedWeather != null) {
                return cachedWeather;
            }

            // 캐시된 데이터도 없는 경우 기본 날씨 객체 반환
            return createDefaultWeather(latitude, longitude);
        }
    }

    /**
     * 초단기예보 가져오기 (향후 6시간)
     */
    public List<KmaForecast> getUltraSrtForecast(double latitude, double longitude) {
        try {
            // 위도/경도를 기상청 격자 좌표로 변환
            int[] gridCoord = kmaApiClient.convertToGridCoord(latitude, longitude);
            int nx = gridCoord[0];
            int ny = gridCoord[1];

            // 초단기예보 API 호출
            return kmaApiClient.getUltraSrtFcst(nx, ny).get();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 단기예보 가져오기 (3일)
     */
    public List<KmaForecast> getVilageForecast(double latitude, double longitude) {
        try {
            // 위도/경도를 기상청 격자 좌표로 변환
            int[] gridCoord = kmaApiClient.convertToGridCoord(latitude, longitude);
            int nx = gridCoord[0];
            int ny = gridCoord[1];

            // 단기예보 API 호출
            return kmaApiClient.getVilageFcst(nx, ny).get();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 기본 날씨 객체 생성 (API 호출 실패 시 사용)
     */
    private Weather createDefaultWeather(double latitude, double longitude) {
        String locationKey = String.format("%f,%f", latitude, longitude);
        long timestamp = System.currentTimeMillis();

        return new Weather(
                0,
                20.0f,  // 기본 온도 20도
                "Clear", // 기본 날씨 상태
                0.0f,   // 강수량 없음
                50,     // 습도 50%
                2.0f,   // 풍속 2m/s
                locationKey,
                timestamp,
                false   // 우산 필요 없음
        );
    }

    /**
     * 캐시된 날씨 데이터가 유효한지 확인
     */
    private boolean isCacheValid(Weather weather) {
        if (weather == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long cacheTime = weather.getTimestamp();

        return (currentTime - cacheTime) < CACHE_EXPIRATION_TIME;
    }

    /**
     * 오래된 날씨 데이터 정리 (24시간 이상)
     */
    private void cleanupOldData() {
        long threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        weatherDao.deleteOldWeatherData(threshold);
    }

    /**
     * 날씨에 따른 고양이 메시지 생성
     */
    public String getCatMessage(Weather weather) {
        // 날씨 상태에 따른 고양이 메시지 생성
        if (weather.isNeedUmbrella()) {
            if (weather.getWeatherCondition().contains("Snow")) {
                return "오늘은 눈이 내린다냥! 따뜻하게 입고 우산도 챙기라냥~";
            } else {
                return "비가 올 것 같다냥! 우산 꼭 챙기라냥!";
            }
        } else {
            if (weather.getTemperature() > 28) {
                return "오늘은 날씨가 덥다냥! 시원하게 지내라냥~";
            } else if (weather.getTemperature() < 10) {
                return "오늘은 날씨가 춥다냥! 따뜻하게 입으라냥~";
            } else {
                return "오늘은 날씨가 좋다냥! 즐거운 하루 보내라냥~";
            }
        }
    }
}
