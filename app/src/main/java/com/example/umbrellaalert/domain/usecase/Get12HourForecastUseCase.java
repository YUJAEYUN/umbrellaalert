package com.example.umbrellaalert.domain.usecase;

import com.example.umbrellaalert.data.model.HourlyForecast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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

    @Inject
    public Get12HourForecastUseCase() {
        // 현재는 모의 데이터만 사용
    }

    /**
     * 12시간 시간별 예보 조회
     * @param latitude 위도
     * @param longitude 경도
     * @return 12시간 예보 리스트
     */
    public List<HourlyForecast> execute(double latitude, double longitude) {
        try {
            // 현재는 모의 데이터만 사용 (실제 API 연동은 추후 구현)
            List<HourlyForecast> forecasts = new ArrayList<>();

            // 1. 초단기예보 모의 데이터 (6시간)
            List<HourlyForecast> ultraShortForecasts = createMockUltraShortForecasts();
            forecasts.addAll(ultraShortForecasts);

            // 2. 단기예보 모의 데이터 (추가 6시간)
            List<HourlyForecast> shortForecasts = createMockShortForecasts(6);
            forecasts.addAll(shortForecasts);

            // 3. 정확히 12시간만 반환
            if (forecasts.size() > 12) {
                forecasts = forecasts.subList(0, 12);
            }

            // 4. 현재 시간 마킹
            markCurrentHour(forecasts);

            return forecasts;

        } catch (Exception e) {
            android.util.Log.e("Get12HourForecastUseCase", "12시간 예보 조회 실패", e);
            return createDefaultForecasts();
        }
    }



    /**
     * 현재 시간 마킹
     */
    private void markCurrentHour(List<HourlyForecast> forecasts) {
        if (forecasts.isEmpty()) return;

        Calendar now = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH", Locale.KOREA);

        String currentDate = dateFormat.format(now.getTime());
        String currentHour = timeFormat.format(now.getTime());

        for (HourlyForecast forecast : forecasts) {
            if (forecast.getForecastDate() != null && forecast.getForecastTime() != null) {
                String forecastHour = forecast.getForecastTime().substring(0, 2);
                if (forecast.getForecastDate().equals(currentDate) &&
                    forecastHour.equals(currentHour)) {
                    forecast.setCurrentHour(true);
                    break;
                }
            }
        }
    }

    /**
     * 기본 12시간 예보 생성 (API 실패 시)
     */
    private List<HourlyForecast> createDefaultForecasts() {
        List<HourlyForecast> forecasts = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.KOREA);

        for (int i = 0; i < 12; i++) {
            HourlyForecast forecast = new HourlyForecast();
            forecast.setForecastDate(dateFormat.format(cal.getTime()));
            forecast.setForecastTime(timeFormat.format(cal.getTime()));
            forecast.setTemperature(20.0f + (float)(Math.random() * 10 - 5)); // 15-25도 랜덤
            forecast.setPrecipitation(0.0f);
            forecast.setPrecipitationProbability((int)(Math.random() * 30)); // 0-30% 랜덤
            forecast.setHumidity(50 + (int)(Math.random() * 30)); // 50-80% 랜덤
            forecast.setWindSpeed(1.0f + (float)(Math.random() * 3)); // 1-4m/s 랜덤
            forecast.setPrecipitationType(0);
            forecast.setWeatherCondition("Clear");
            forecast.setNeedUmbrella(false);
            forecast.setCurrentHour(i == 0); // 첫 번째가 현재 시간

            forecasts.add(forecast);

            // 1시간 추가
            cal.add(Calendar.HOUR_OF_DAY, 1);
        }

        return forecasts;
    }

    /**
     * 모의 초단기예보 데이터 생성 (6시간)
     */
    private List<HourlyForecast> createMockUltraShortForecasts() {
        List<HourlyForecast> forecasts = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.KOREA);

        String[] weatherConditions = {"Clear", "Clouds", "Clear", "Clouds", "Clear", "Rain"};
        int[] precipProbs = {0, 10, 5, 20, 0, 60};
        boolean[] umbrellaNeeded = {false, false, false, false, false, true};

        for (int i = 0; i < 6; i++) {
            HourlyForecast forecast = new HourlyForecast();
            forecast.setForecastDate(dateFormat.format(cal.getTime()));
            forecast.setForecastTime(timeFormat.format(cal.getTime()));
            forecast.setTemperature(22.0f + i * 0.5f); // 점진적 온도 변화
            forecast.setPrecipitation(umbrellaNeeded[i] ? 2.0f : 0.0f);
            forecast.setPrecipitationProbability(precipProbs[i]);
            forecast.setHumidity(55 + i * 2);
            forecast.setWindSpeed(2.0f + (float)(Math.random() * 2));
            forecast.setPrecipitationType(umbrellaNeeded[i] ? 1 : 0);
            forecast.setWeatherCondition(weatherConditions[i]);
            forecast.setNeedUmbrella(umbrellaNeeded[i]);
            forecast.setCurrentHour(i == 0);

            forecasts.add(forecast);

            // 1시간 추가
            cal.add(Calendar.HOUR_OF_DAY, 1);
        }

        return forecasts;
    }

    /**
     * 모의 단기예보 데이터 생성 (추가 6시간)
     */
    private List<HourlyForecast> createMockShortForecasts(int skipHours) {
        List<HourlyForecast> forecasts = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, skipHours); // 초단기예보 이후 시간부터

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.KOREA);

        String[] weatherConditions = {"Rain", "Clouds", "Clear", "Clear", "Clouds", "Clear"};
        int[] precipProbs = {70, 30, 10, 5, 15, 0};
        boolean[] umbrellaNeeded = {true, false, false, false, false, false};

        for (int i = 0; i < 6; i++) {
            HourlyForecast forecast = new HourlyForecast();
            forecast.setForecastDate(dateFormat.format(cal.getTime()));
            forecast.setForecastTime(timeFormat.format(cal.getTime()));
            forecast.setTemperature(25.0f - i * 0.3f); // 점진적 온도 하락
            forecast.setPrecipitation(umbrellaNeeded[i] ? 5.0f : 0.0f);
            forecast.setPrecipitationProbability(precipProbs[i]);
            forecast.setHumidity(65 + i);
            forecast.setWindSpeed(1.5f + (float)(Math.random() * 2));
            forecast.setPrecipitationType(umbrellaNeeded[i] ? 1 : 0);
            forecast.setWeatherCondition(weatherConditions[i]);
            forecast.setNeedUmbrella(umbrellaNeeded[i]);
            forecast.setCurrentHour(false);

            forecasts.add(forecast);

            // 1시간 추가
            cal.add(Calendar.HOUR_OF_DAY, 1);
        }

        return forecasts;
    }
}
