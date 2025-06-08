package com.example.umbrellaalert.service;

import com.example.umbrellaalert.data.model.HourlyForecast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * 12시간 예보 목업 데이터 생성 서비스
 * 고양이 날씨 분석관이 사용할 데이터를 제공
 */
public class MockWeatherForecastService {
    
    private static final String TAG = "MockWeatherForecast";
    
    // 날씨 상태 목록
    private static final String[] WEATHER_CONDITIONS = {
        "맑음", "구름많음", "흐림", "비", "소나기", "눈"
    };
    
    // 강수 확률 (%)
    private static final int[] RAIN_PROBABILITIES = {
        0, 10, 20, 30, 40, 50, 60, 70, 80, 90
    };
    
    /**
     * 8시간 예보 목업 데이터 생성 (6월 9일 세종 날씨 기반, 3시간 단위)
     */
    public static List<HourlyForecast> generate12HourForecast() {
        List<HourlyForecast> forecasts = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        // 3시간 단위 시간대: 02시, 05시, 08시, 11시, 14시, 17시, 20시, 23시
        int[] forecastHours = {2, 5, 8, 11, 14, 17, 20, 23};

        // 6월 9일 세종 실제 날씨 데이터 기반 (3시간 단위)
        // 온도: 02시(21°) → 05시(21°) → 08시(21°) → 11시(23°) → 14시(31°) → 17시(29°) → 20시(23°) → 23시(19°)
        float[] hourlyTemperatures = {21.0f, 21.0f, 21.0f, 23.0f, 31.0f, 29.0f, 23.0f, 19.0f};

        for (int i = 0; i < 8; i++) {
            Calendar forecastTime = (Calendar) calendar.clone();
            forecastTime.set(Calendar.HOUR_OF_DAY, forecastHours[i]);
            forecastTime.set(Calendar.MINUTE, 0);
            forecastTime.set(Calendar.SECOND, 0);

            int currentHour = forecastHours[i];

            // 실제 데이터 기반 온도
            float temperature = hourlyTemperatures[i];

            // 맑은 날씨 (6월 9일 세종 기준)
            String weatherCondition = "맑음";

            // 강수확률: 오전 2시~10시만 10%, 나머지는 0%
            int rainProbability = (currentHour >= 2 && currentHour <= 10) ? 10 : 0;

            // 세종 6월 9일 기준 습도 (63% 기준으로 시간대별 변화)
            int humidity = calculateSejongjuneHumidity(currentHour);

            // 세종 6월 9일 풍속 (2m/s 기준)
            float windSpeed = 1.8f + (float)(Math.random() * 0.4f); // 1.8-2.2 m/s
            
            // 날짜와 시간 포맷 생성
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", java.util.Locale.KOREA);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", java.util.Locale.KOREA);

            String forecastDate = dateFormat.format(forecastTime.getTime());
            String forecastTimeStr = timeFormat.format(forecastTime.getTime());

            // 강수형태 결정 (맑은 날이므로 없음)
            int precipitationType = 0; // 없음

            // 강수량 (맑은 날이므로 0)
            float precipitation = 0.0f;

            // 우산 필요 여부 (맑은 날이므로 불필요)
            boolean needUmbrella = false;

            HourlyForecast forecast = new HourlyForecast(
                forecastDate,
                forecastTimeStr,
                temperature,
                precipitation,
                rainProbability,
                humidity,
                windSpeed,
                precipitationType,
                weatherCondition,
                needUmbrella
            );

            forecast.setDataSource("MOCK");
            
            forecasts.add(forecast);
        }
        
        return forecasts;
    }
    
    /**
     * 날씨 시나리오 정의
     */
    private enum WeatherScenario {
        SUNNY_ALL_DAY,      // 하루 종일 맑음
        CLOUDY_TO_RAIN,     // 구름 → 비
        RAIN_TO_CLEAR,      // 비 → 맑음
        AFTERNOON_SHOWER,   // 오후 소나기
        GRADUAL_CLOUDY      // 점점 흐려짐
    }
    
    /**
     * 랜덤 날씨 시나리오 선택
     */
    private static WeatherScenario getRandomWeatherScenario() {
        WeatherScenario[] scenarios = WeatherScenario.values();
        Random random = new Random();
        return scenarios[random.nextInt(scenarios.length)];
    }
    
    /**
     * 시간대별 온도 계산 (자연스러운 일교차)
     */
    private static float calculateTemperatureForHour(float baseTemp, int hour) {
        // 새벽 4시가 최저, 오후 2시가 최고
        double hourRadians = (hour - 4) * Math.PI / 12.0;
        float tempVariation = (float) (Math.sin(hourRadians) * 5.0); // ±5도 변화
        
        return baseTemp + tempVariation;
    }
    
    /**
     * 시나리오에 따른 날씨 상태 결정
     */
    private static String getWeatherConditionForScenario(WeatherScenario scenario, int hourIndex) {
        switch (scenario) {
            case SUNNY_ALL_DAY:
                return hourIndex < 2 ? "구름많음" : "맑음";
                
            case CLOUDY_TO_RAIN:
                if (hourIndex < 3) return "구름많음";
                else if (hourIndex < 6) return "흐림";
                else return "비";
                
            case RAIN_TO_CLEAR:
                if (hourIndex < 4) return "비";
                else if (hourIndex < 7) return "흐림";
                else return "구름많음";
                
            case AFTERNOON_SHOWER:
                if (hourIndex >= 4 && hourIndex <= 7) return "소나기";
                else if (hourIndex >= 3 && hourIndex <= 8) return "구름많음";
                else return "맑음";
                
            case GRADUAL_CLOUDY:
                if (hourIndex < 3) return "맑음";
                else if (hourIndex < 8) return "구름많음";
                else return "흐림";
                
            default:
                return "맑음";
        }
    }
    
    /**
     * 강수 확률 계산
     */
    private static int calculateRainProbability(WeatherScenario scenario, int hourIndex) {
        switch (scenario) {
            case SUNNY_ALL_DAY:
                return 0;
                
            case CLOUDY_TO_RAIN:
                if (hourIndex < 3) return 10;
                else if (hourIndex < 6) return 30 + hourIndex * 10;
                else return 80;
                
            case RAIN_TO_CLEAR:
                if (hourIndex < 4) return 90;
                else if (hourIndex < 7) return 60 - hourIndex * 10;
                else return 10;
                
            case AFTERNOON_SHOWER:
                if (hourIndex >= 4 && hourIndex <= 7) return 70 + (new Random().nextInt(20));
                else return 20;
                
            case GRADUAL_CLOUDY:
                return Math.min(hourIndex * 10, 50);
                
            default:
                return 0;
        }
    }
    
    /**
     * 습도 계산
     */
    private static int calculateHumidity(String weatherCondition, int rainProbability) {
        int baseHumidity = 50;
        
        switch (weatherCondition) {
            case "비":
            case "소나기":
                baseHumidity = 80;
                break;
            case "흐림":
                baseHumidity = 70;
                break;
            case "구름많음":
                baseHumidity = 60;
                break;
            case "맑음":
                baseHumidity = 45;
                break;
        }
        
        // 강수 확률에 따른 조정
        baseHumidity += rainProbability / 5;
        
        return Math.min(Math.max(baseHumidity, 30), 95);
    }

    /**
     * 세종 6월 9일 기준 시간대별 습도 계산
     */
    private static int calculateSejongjuneHumidity(int hour) {
        // 6월 9일 세종 기준 습도 63%를 바탕으로 시간대별 변화
        int baseHumidity = 63;

        // 새벽/아침: 높은 습도
        if (hour >= 0 && hour < 6) {
            return baseHumidity + 10; // 73%
        }
        // 아침: 기준 습도
        else if (hour >= 6 && hour < 12) {
            return baseHumidity; // 63%
        }
        // 오후: 낮은 습도 (기온 상승으로)
        else if (hour >= 12 && hour < 18) {
            return baseHumidity - 8; // 55%
        }
        // 저녁/밤: 다시 상승
        else {
            return baseHumidity + 5; // 68%
        }
    }
}
