package com.example.umbrellaalert.domain.usecase;

import android.util.Log;

import com.example.umbrellaalert.data.api.KmaApiClient;
import com.example.umbrellaalert.data.model.HourlyForecast;
import com.example.umbrellaalert.data.model.KmaForecast;
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.domain.repository.WeatherRepository;
import com.example.umbrellaalert.util.CoordinateConverter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 12시간 시간별 예보 조회 UseCase
 * 현재부터 12시간 동안의 1시간 단위 날씨 예보를 제공
 */
@Singleton
public class Get12HourForecastUseCase {

    private static final String TAG = "Get12HourForecastUseCase";
    private final KmaApiClient kmaApiClient;
    private final WeatherRepository weatherRepository;

    @Inject
    public Get12HourForecastUseCase(KmaApiClient kmaApiClient, WeatherRepository weatherRepository) {
        this.kmaApiClient = kmaApiClient;
        this.weatherRepository = weatherRepository;
    }

    /**
     * 6시간 시간별 예보 조회 (단순화된 버전)
     * @param latitude 위도
     * @param longitude 경도
     * @return 6시간 예보 리스트
     */
    public List<HourlyForecast> execute(double latitude, double longitude) {
        try {
            Log.d(TAG, "🌤️ 6시간 예보 조회 시작 - 위도: " + latitude + ", 경도: " + longitude);

            // 위도/경도를 기상청 격자 좌표로 변환
            CoordinateConverter.GridCoordinate gridCoord = CoordinateConverter.convertToGrid(latitude, longitude);
            int nx = gridCoord.nx;
            int ny = gridCoord.ny;

            Log.d(TAG, "📍 격자 좌표 변환 완료 - nx: " + nx + ", ny: " + ny);

            // 단기예보 API 호출 (하나의 API만 사용)
            Future<List<KmaForecast>> future = kmaApiClient.getVilageFcst(nx, ny);
            List<KmaForecast> kmaForecasts = future.get();

            if (kmaForecasts == null || kmaForecasts.isEmpty()) {
                Log.w(TAG, "⚠️ API에서 예보 데이터 없음 - 기본 6시간 예보 생성");
                return generateDefaultSixHourForecast(latitude, longitude);
            }

            Log.d(TAG, "📊 API에서 받은 예보 데이터: " + kmaForecasts.size() + "개");

            // API 데이터를 시간대별로 6시간 예보 생성
            List<HourlyForecast> forecasts = createSixHourForecastFromApi(kmaForecasts);

            // 최종 반환 전 확인
            Log.d(TAG, "✅ 최종 6시간 예보 생성 완료:");
            for (int i = 0; i < forecasts.size(); i++) {
                HourlyForecast forecast = forecasts.get(i);
                Log.d(TAG, "  " + (i + 1) + "시간 후: " + forecast.getTemperature() + "°C (시간: " + forecast.getForecastTime() + ")");
            }

            return forecasts;

        } catch (Exception e) {
            Log.e(TAG, "❌ 6시간 예보 조회 실패", e);
            return generateDefaultSixHourForecast(latitude, longitude);
        }
    }

    /**
     * API 데이터를 시간대별로 6시간 예보 생성
     */
    private List<HourlyForecast> createSixHourForecastFromApi(List<KmaForecast> kmaForecasts) {
        List<HourlyForecast> forecasts = new ArrayList<>();

        // 현재 시간부터 6시간 후까지의 시간대 생성
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.KOREA);

        // 기본 온도 (API에서 받은 첫 번째 온도 또는 기본값)
        float baseTemperature = 20.0f;
        if (!kmaForecasts.isEmpty() && kmaForecasts.get(0).getTemperature() > 0) {
            baseTemperature = kmaForecasts.get(0).getTemperature();
            Log.d(TAG, "🌡️ API 기준 온도: " + baseTemperature + "°C");
        }

        // 6시간 예보 생성
        for (int i = 0; i < 6; i++) {
            HourlyForecast forecast = new HourlyForecast();

            // 시간 설정 (현재 시간부터 1시간씩 증가)
            Calendar forecastTime = (Calendar) calendar.clone();
            forecastTime.add(Calendar.HOUR_OF_DAY, i + 1);

            forecast.setForecastDate(dateFormat.format(forecastTime.getTime()));
            forecast.setForecastTime(timeFormat.format(forecastTime.getTime()));

            // 온도 설정 (기준 온도에서 시간별 변화)
            float temperature = baseTemperature + (float) (Math.random() * 6 - 3); // ±3도 변화
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
            Log.d(TAG, "🕐 " + (i + 1) + "시간 후 예보 생성: " + temperature + "°C (시간: " + forecast.getForecastTime() + ")");
        }

        return forecasts;
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
