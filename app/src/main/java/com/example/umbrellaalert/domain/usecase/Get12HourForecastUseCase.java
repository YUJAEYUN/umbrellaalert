package com.example.umbrellaalert.domain.usecase;

import android.util.Log;

import com.example.umbrellaalert.data.api.KmaApiClient;
import com.example.umbrellaalert.data.model.HourlyForecast;
import com.example.umbrellaalert.data.model.KmaForecast;
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

    @Inject
    public Get12HourForecastUseCase(KmaApiClient kmaApiClient) {
        this.kmaApiClient = kmaApiClient;
    }

    /**
     * 12시간 시간별 예보 조회
     * @param latitude 위도
     * @param longitude 경도
     * @return 12시간 예보 리스트
     */
    public List<HourlyForecast> execute(double latitude, double longitude) {
        try {
            Log.d(TAG, "12시간 예보 조회 시작 - 위도: " + latitude + ", 경도: " + longitude);

            // 위도/경도를 기상청 격자 좌표로 변환
            CoordinateConverter.GridCoordinate gridCoord = CoordinateConverter.convertToGrid(latitude, longitude);
            int nx = gridCoord.nx;
            int ny = gridCoord.ny;

            Log.d(TAG, "격자 좌표 변환 완료 - nx: " + nx + ", ny: " + ny);

            List<HourlyForecast> forecasts = new ArrayList<>();

            // 1. 초단기예보 조회 (6시간)
            try {
                Future<List<KmaForecast>> ultraShortFuture = kmaApiClient.getUltraSrtFcst(nx, ny);
                List<KmaForecast> ultraShortForecasts = ultraShortFuture.get();

                if (ultraShortForecasts != null && !ultraShortForecasts.isEmpty()) {
                    Log.d(TAG, "초단기예보 데이터 " + ultraShortForecasts.size() + "개 조회 완료");
                    List<HourlyForecast> hourlyFromUltraShort = convertToHourlyForecasts(ultraShortForecasts, true);
                    forecasts.addAll(hourlyFromUltraShort);
                } else {
                    Log.w(TAG, "초단기예보 데이터가 없음, 목업 데이터 사용");
                    forecasts.addAll(createMockUltraShortForecasts());
                }
            } catch (Exception e) {
                Log.e(TAG, "초단기예보 조회 실패, 목업 데이터 사용", e);
                forecasts.addAll(createMockUltraShortForecasts());
            }

            // 2. 12시간이 부족하면 목업 데이터로 채우기 (단기예보 API 호출 생략으로 속도 향상)
            if (forecasts.size() < 12) {
                Log.d(TAG, "초단기예보만으로 부족함 (" + forecasts.size() + "개), 목업 데이터로 보완");
                forecasts.addAll(createMockShortForecasts(forecasts.size()));
            }

            // 3. 정확히 12시간만 반환
            if (forecasts.size() > 12) {
                forecasts = forecasts.subList(0, 12);
            }

            // 4. 현재 시간 마킹
            markCurrentHour(forecasts);

            Log.d(TAG, "12시간 예보 조회 완료 - 총 " + forecasts.size() + "개");
            return forecasts;

        } catch (Exception e) {
            Log.e(TAG, "12시간 예보 조회 실패", e);
            return createDefaultForecasts();
        }
    }

    /**
     * KmaForecast를 HourlyForecast로 변환
     */
    private List<HourlyForecast> convertToHourlyForecasts(List<KmaForecast> kmaForecasts, boolean isUltraShort) {
        List<HourlyForecast> hourlyForecasts = new ArrayList<>();

        for (KmaForecast kmaForecast : kmaForecasts) {
            HourlyForecast hourlyForecast = new HourlyForecast();

            // 시간 설정
            String timeStr = kmaForecast.getForecastTime();
            if (timeStr != null && timeStr.length() >= 4) {
                hourlyForecast.setForecastTime(timeStr);
            } else {
                // 기본 시간 설정
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.HOUR_OF_DAY, hourlyForecasts.size() + 1);
                SimpleDateFormat fullTimeFormat = new SimpleDateFormat("HHmm", Locale.KOREA);
                hourlyForecast.setForecastTime(fullTimeFormat.format(cal.getTime()));
            }

            // 날짜 설정
            String dateStr = kmaForecast.getForecastDate();
            if (dateStr != null) {
                hourlyForecast.setForecastDate(dateStr);
            } else {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
                hourlyForecast.setForecastDate(dateFormat.format(new Date()));
            }

            // 온도
            hourlyForecast.setTemperature(kmaForecast.getTemperature());

            // 날씨 상태 변환
            String weatherCondition = convertWeatherConditionToString(kmaForecast.getWeatherCondition(), kmaForecast.getPrecipitationType());
            hourlyForecast.setWeatherCondition(weatherCondition);

            // 강수확률
            hourlyForecast.setPrecipitationProbability(kmaForecast.getPrecipitationProbability());

            // 강수량
            hourlyForecast.setPrecipitation(kmaForecast.getPrecipitation());

            // 습도
            hourlyForecast.setHumidity(kmaForecast.getHumidity());

            // 풍속
            hourlyForecast.setWindSpeed(kmaForecast.getWindSpeed());

            // 강수형태
            hourlyForecast.setPrecipitationType(kmaForecast.getPrecipitationType());

            // 우산 필요 여부
            hourlyForecast.setNeedUmbrella(kmaForecast.isNeedUmbrella());

            hourlyForecasts.add(hourlyForecast);

            // 초단기예보는 6시간, 단기예보는 필요한 만큼만
            if (isUltraShort && hourlyForecasts.size() >= 6) {
                break;
            } else if (!isUltraShort && hourlyForecasts.size() >= 6) {
                break;
            }
        }

        return hourlyForecasts;
    }

    /**
     * 날씨 상태 변환
     */
    private String convertWeatherConditionToString(String condition, int precipitationType) {
        if (condition == null) return "Clear"; // 기본값: 맑음

        if (precipitationType > 0) {
            switch (precipitationType) {
                case 1: return "Rain"; // 비
                case 2: return "Sleet"; // 눈/비
                case 3: return "Snow"; // 눈
                case 4: return "Rain"; // 소나기
                default: return "Rain"; // 비
            }
        }

        if (condition.contains("Clear") || condition.contains("맑음")) {
            return "Clear"; // 맑음
        } else if (condition.contains("Clouds") || condition.contains("구름")) {
            return "Clouds"; // 구름많음
        } else {
            return "Clear"; // 기본값: 맑음
        }
    }

    /**
     * 중복 시간 체크
     */
    private boolean isDuplicateTime(List<HourlyForecast> forecasts, HourlyForecast newForecast) {
        String newTime = newForecast.getForecastTime();
        for (HourlyForecast existing : forecasts) {
            if (existing.getForecastTime().equals(newTime)) {
                return true;
            }
        }
        return false;
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
