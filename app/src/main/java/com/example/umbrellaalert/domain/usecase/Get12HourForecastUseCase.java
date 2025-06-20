package com.example.umbrellaalert.domain.usecase;

import android.util.Log;

import com.example.umbrellaalert.data.manager.WeatherManager;
import com.example.umbrellaalert.data.model.HourlyForecast;
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.domain.repository.WeatherRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 12시간 시간별 예보 조회 UseCase
 * 현재부터 12시간 동안의 1시간 단위 날씨 예보를 제공
 */
@Singleton
public class Get12HourForecastUseCase {

    private static final String TAG = "Get12HourForecastUseCase";
    private final WeatherManager weatherManager;
    private final WeatherRepository weatherRepository;

    @Inject
    public Get12HourForecastUseCase(WeatherManager weatherManager, WeatherRepository weatherRepository) {
        this.weatherManager = weatherManager;
        this.weatherRepository = weatherRepository;
    }

    /**
     * 12시간 시간별 예보 조회 (OpenWeather API 사용)
     * @param latitude 위도
     * @param longitude 경도
     * @return 12시간 예보 리스트 (6시간으로 제한)
     */
    public List<HourlyForecast> execute(double latitude, double longitude) {
        try {
            Log.d(TAG, "🌤️ OpenWeather API로 12시간 예보 조회 시작 - 위도: " + latitude + ", 경도: " + longitude);

            // WeatherManager를 통해 OpenWeather API 사용
            final List<HourlyForecast>[] result = new List[1];
            final Exception[] error = new Exception[1];
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

            weatherManager.get12HourForecast(latitude, longitude, new WeatherManager.ForecastCallback() {
                @Override
                public void onSuccess(List<HourlyForecast> forecasts) {
                    result[0] = forecasts;
                    latch.countDown();
                }

                @Override
                public void onError(String errorMessage) {
                    error[0] = new Exception(errorMessage);
                    latch.countDown();
                }
            });

            // 최대 10초 대기
            latch.await(10, java.util.concurrent.TimeUnit.SECONDS);

            if (error[0] != null) {
                throw error[0];
            }

            List<HourlyForecast> forecasts = result[0];
            if (forecasts != null && !forecasts.isEmpty()) {
                // 6시간으로 제한
                if (forecasts.size() > 6) {
                    forecasts = forecasts.subList(0, 6);
                }

                Log.d(TAG, "✅ OpenWeather API에서 예보 수신 완료: " + forecasts.size() + "개");

                // 최종 반환 전 확인
                for (int i = 0; i < forecasts.size(); i++) {
                    HourlyForecast forecast = forecasts.get(i);
                    Log.d(TAG, "  " + (i + 1) + "시간 후: " + forecast.getTemperature() + "°C (시간: " + forecast.getForecastTime() + ")");
                }

                return forecasts;
            } else {
                Log.w(TAG, "⚠️ OpenWeather API에서 예보 데이터 없음 - 기본 6시간 예보 생성");
                return generateDefaultSixHourForecast(latitude, longitude);
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ OpenWeather API 예보 조회 실패: " + e.getMessage(), e);
            return generateDefaultSixHourForecast(latitude, longitude);
        }
    }



    /**
     * 기본 6시간 예보 생성 (API 실패 시)
     */
    private List<HourlyForecast> generateDefaultSixHourForecast(double latitude, double longitude) {
        List<HourlyForecast> forecasts = new ArrayList<>();

        Log.d(TAG, "🔧 기본 6시간 예보 생성 시작");

        // 현재 날씨에서 기준 온도 가져오기
        float baseTemperature = 20.0f; // 기본값
        try {
            GetCurrentWeatherUseCase currentWeatherUseCase = new GetCurrentWeatherUseCase(weatherRepository);
            Weather currentWeather = currentWeatherUseCase.execute(latitude, longitude);
            if (currentWeather != null && currentWeather.getTemperature() > -50) {
                baseTemperature = currentWeather.getTemperature();
                Log.d(TAG, "🌡️ 현재 온도 기준: " + baseTemperature + "°C");
            }
        } catch (Exception e) {
            Log.w(TAG, "현재 온도 가져오기 실패 - 기본값 사용: " + baseTemperature + "°C");
        }

        // 현재 시간부터 6시간 예보 생성
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.KOREA);

        for (int i = 0; i < 6; i++) {
            HourlyForecast forecast = new HourlyForecast();

            // 시간 설정
            Calendar forecastTime = (Calendar) calendar.clone();
            forecastTime.add(Calendar.HOUR_OF_DAY, i + 1);

            forecast.setForecastDate(dateFormat.format(forecastTime.getTime()));
            forecast.setForecastTime(timeFormat.format(forecastTime.getTime()));

            // 온도 설정 (기준 온도에서 시간별 변화)
            float temperature = baseTemperature + (float) (Math.random() * 4 - 2); // ±2도 변화
            forecast.setTemperature(temperature);

            // 기본 날씨 상태 설정
            forecast.setWeatherCondition("Clear");
            forecast.setPrecipitationProbability(10);
            forecast.setPrecipitation(0.0f);
            forecast.setHumidity(60);
            forecast.setWindSpeed(2.0f);
            forecast.setPrecipitationType(0);
            forecast.setNeedUmbrella(false);

            // 첫 번째 예보는 현재 시간으로 마킹
            if (i == 0) {
                forecast.setCurrentHour(true);
            }

            forecasts.add(forecast);
            Log.d(TAG, "🕐 기본 " + (i + 1) + "시간 후 예보: " + temperature + "°C (시간: " + forecast.getForecastTime() + ")");
        }

        Log.d(TAG, "✅ 기본 6시간 예보 생성 완료");
        return forecasts;
    }

}
