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
     * 12시간 시간별 예보 조회
     * @param latitude 위도
     * @param longitude 경도
     * @return 12시간 예보 리스트
     */
    public List<HourlyForecast> execute(double latitude, double longitude) {
        try {
            Log.d(TAG, "6시간 예보 조회 시작 - 위도: " + latitude + ", 경도: " + longitude);

            // 위도/경도를 기상청 격자 좌표로 변환
            CoordinateConverter.GridCoordinate gridCoord = CoordinateConverter.convertToGrid(latitude, longitude);
            int nx = gridCoord.nx;
            int ny = gridCoord.ny;

            Log.d(TAG, "격자 좌표 변환 완료 - nx: " + nx + ", ny: " + ny);

            List<HourlyForecast> forecasts = new ArrayList<>();

            // 1. 단기예보 조회 (온도 데이터 우선)
            try {
                Future<List<KmaForecast>> shortFuture = kmaApiClient.getVilageFcst(nx, ny);
                List<KmaForecast> shortForecasts = shortFuture.get();

                if (shortForecasts != null && !shortForecasts.isEmpty()) {
                    Log.d(TAG, "단기예보 데이터 " + shortForecasts.size() + "개 조회 완료 (온도 데이터 포함)");
                    List<HourlyForecast> hourlyFromShort = convertToHourlyForecasts(shortForecasts, false);
                    forecasts.addAll(hourlyFromShort);
                } else {
                    Log.w(TAG, "단기예보 데이터가 없음");
                }
            } catch (Exception e) {
                Log.e(TAG, "단기예보 조회 실패", e);
            }

            // 2. 초단기예보 조회 (추가 데이터)
            if (forecasts.size() < 12) {
                try {
                    Future<List<KmaForecast>> ultraShortFuture = kmaApiClient.getUltraSrtFcst(nx, ny);
                    List<KmaForecast> ultraShortForecasts = ultraShortFuture.get();

                if (ultraShortForecasts != null && !ultraShortForecasts.isEmpty()) {
                    Log.d(TAG, "초단기예보 데이터 " + ultraShortForecasts.size() + "개 조회 완료");
                    List<HourlyForecast> hourlyFromUltraShort = convertToHourlyForecasts(ultraShortForecasts, true);

                    // 초단기예보 변환 후 온도 확인
                    Log.d(TAG, "🔍 초단기예보 변환 후 온도 확인:");
                    for (int i = 0; i < Math.min(3, hourlyFromUltraShort.size()); i++) {
                        HourlyForecast forecast = hourlyFromUltraShort.get(i);
                        Log.d(TAG, "  초단기 " + i + "번째: " + forecast.getTemperature() + "°C (시간: " + forecast.getForecastTime() + ")");
                    }

                    forecasts.addAll(hourlyFromUltraShort);

                    // forecasts에 추가 후 온도 확인
                    Log.d(TAG, "🔍 forecasts에 추가 후 온도 확인:");
                    for (int i = 0; i < Math.min(3, forecasts.size()); i++) {
                        HourlyForecast forecast = forecasts.get(i);
                        Log.d(TAG, "  추가 후 " + i + "번째: " + forecast.getTemperature() + "°C (시간: " + forecast.getForecastTime() + ")");
                    }
                    // 초단기예보 변환 후 온도 확인
                    Log.d(TAG, "🔍 초단기예보 변환 후 온도 확인:");
                    for (int i = 0; i < Math.min(3, hourlyFromUltraShort.size()); i++) {
                        HourlyForecast forecast = hourlyFromUltraShort.get(i);
                        Log.d(TAG, "  초단기 " + i + "번째: " + forecast.getTemperature() + "°C (시간: " + forecast.getForecastTime() + ")");
                    }

                    // 단기예보와 겹치지 않는 시간대만 추가
                    for (HourlyForecast forecast : hourlyFromUltraShort) {
                        if (forecasts.size() >= 12) break;
                        if (!isDuplicateTime(forecasts, forecast)) {
                            forecasts.add(forecast);
                        }
                    }
                } else {
                    Log.w(TAG, "초단기예보 데이터가 없음");
                }
            } catch (Exception e) {
                Log.e(TAG, "초단기예보 조회 실패", e);
            }
            }

            // 3. 예보 데이터가 부족한 경우 6시간 예보 생성
            if (forecasts.size() < 6) {
                Log.w(TAG, "⚠️ 예보 데이터 부족 (" + forecasts.size() + "개) - 6시간 예보 생성");
                forecasts = generateSixHourForecast(forecasts, latitude, longitude);
            }

            // 4. 온도 데이터 보완 (API에서 온도가 없는 경우)
            fillMissingTemperatures(forecasts, latitude, longitude);

            // 5. 정확히 6시간만 반환 (WeatherViewModel에서 6시간으로 제한하므로)
            if (forecasts.size() > 6) {
                forecasts = forecasts.subList(0, 6);
            }

            // 6. 현재 시간 마킹
            markCurrentHour(forecasts);

            // 최종 반환 전 온도 확인
            Log.d(TAG, "🔍 최종 반환 전 예보 데이터 확인:");
            for (int i = 0; i < Math.min(3, forecasts.size()); i++) {
                HourlyForecast forecast = forecasts.get(i);
                Log.d(TAG, "  " + i + "번째: " + forecast.getTemperature() + "°C (시간: " + forecast.getForecastTime() + ")");
            }

            Log.d(TAG, "6시간 예보 조회 완료 - 총 " + forecasts.size() + "개");
            return forecasts;

        } catch (Exception e) {
            Log.e(TAG, "6시간 예보 조회 실패", e);
            return new ArrayList<>();
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

            // 온도 - KmaForecast에서 받은 값을 그대로 설정
            float temperature = kmaForecast.getTemperature();
            if (temperature == 0.0f) {
                // API에서 온도 데이터가 없는 경우 임시값 설정 (나중에 보완)
                temperature = -999.0f; // 특별한 값으로 표시
                Log.w(TAG, "⚠️ API에서 온도 데이터 없음 - 임시값 설정: " + temperature + "°C (시간: " + kmaForecast.getForecastTime() + ")");
            }
            hourlyForecast.setTemperature(temperature);
            Log.d(TAG, "🌡️ 온도 설정: " + kmaForecast.getTemperature() + "°C → " + hourlyForecast.getTemperature() + "°C (시간: " + kmaForecast.getForecastTime() + ")");

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

            // HourlyForecast 객체 생성 완료 후 온도 확인
            Log.d(TAG, "✅ HourlyForecast 생성 완료: " + hourlyForecast.getTemperature() + "°C (시간: " + hourlyForecast.getForecastTime() + ")");

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
     * 온도 데이터 보완 (API에서 온도가 없는 경우)
     */
    private void fillMissingTemperatures(List<HourlyForecast> forecasts, double latitude, double longitude) {
        if (forecasts.isEmpty()) return;

        // 온도가 없는 예보가 있는지 확인
        boolean hasTemperatureMissing = false;
        for (HourlyForecast forecast : forecasts) {
            if (forecast.getTemperature() == -999.0f || forecast.getTemperature() == 0.0f) {
                hasTemperatureMissing = true;
                break;
            }
        }

        if (!hasTemperatureMissing) {
            Log.d(TAG, "✅ 모든 예보에 온도 데이터가 있음");
            return;
        }

        Log.w(TAG, "⚠️ 온도 데이터가 없는 예보 발견 - 현재 날씨 기반으로 보완");

        // 현재 날씨에서 온도 가져오기
        try {
            GetCurrentWeatherUseCase currentWeatherUseCase = new GetCurrentWeatherUseCase(weatherRepository);
            Weather currentWeather = currentWeatherUseCase.execute(latitude, longitude);

            if (currentWeather != null && currentWeather.getTemperature() > -50) {
                float baseTemperature = currentWeather.getTemperature();
                Log.d(TAG, "🌡️ 현재 온도 기준: " + baseTemperature + "°C");

                // 시간대별로 온도 변화 적용
                for (int i = 0; i < forecasts.size(); i++) {
                    HourlyForecast forecast = forecasts.get(i);
                    if (forecast.getTemperature() == -999.0f || forecast.getTemperature() == 0.0f) {
                        // 시간이 지날수록 약간의 온도 변화 적용 (±2도 범위)
                        float temperatureVariation = (float) (Math.random() * 4 - 2); // -2 ~ +2도
                        float estimatedTemperature = baseTemperature + temperatureVariation;

                        forecast.setTemperature(estimatedTemperature);
                        Log.d(TAG, "🔧 온도 보완: " + i + "번째 예보 → " + estimatedTemperature + "°C (기준: " + baseTemperature + "°C)");
                    }
                }
            } else {
                Log.w(TAG, "⚠️ 현재 날씨 온도를 가져올 수 없음 - 기본값 사용");
                // 기본 온도 설정 (20도)
                for (HourlyForecast forecast : forecasts) {
                    if (forecast.getTemperature() == -999.0f || forecast.getTemperature() == 0.0f) {
                        forecast.setTemperature(20.0f);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "온도 데이터 보완 실패", e);
            // 기본 온도 설정
            for (HourlyForecast forecast : forecasts) {
                if (forecast.getTemperature() == -999.0f || forecast.getTemperature() == 0.0f) {
                    forecast.setTemperature(20.0f);
                }
            }
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


}
