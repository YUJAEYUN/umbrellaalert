package com.example.umbrellaalert.service;

import com.example.umbrellaalert.data.model.HourlyForecast;

import java.util.Calendar;
import java.util.List;

/**
 * 고양이 날씨 분석관 서비스
 * 12시간 예보 데이터를 분석해서 고양이 스타일로 날씨 정보를 제공
 */
public class CatWeatherAnalystService {
    
    /**
     * 12시간 예보를 분석해서 고양이 스타일 분석 결과 생성
     */
    public static String analyzeWeatherForecast(List<HourlyForecast> forecasts) {
        if (forecasts == null || forecasts.isEmpty()) {
            return "예보 데이터가 없어서 분석할 수 없다냥... 😿";
        }
        
        WeatherAnalysis analysis = performDetailedAnalysis(forecasts);
        return generateCatAnalysisMessage(analysis);
    }
    
    /**
     * 상세 날씨 분석 수행
     */
    private static WeatherAnalysis performDetailedAnalysis(List<HourlyForecast> forecasts) {
        WeatherAnalysis analysis = new WeatherAnalysis();
        
        // 강수 관련 분석
        analyzeRainProbability(forecasts, analysis);
        
        // 온도 관련 분석
        analyzeTemperature(forecasts, analysis);
        
        // 날씨 변화 패턴 분석
        analyzeWeatherPattern(forecasts, analysis);
        
        // 시간대별 특이사항 분석
        analyzeTimeSpecificEvents(forecasts, analysis);
        
        return analysis;
    }
    
    /**
     * 강수 확률 분석
     */
    private static void analyzeRainProbability(List<HourlyForecast> forecasts, WeatherAnalysis analysis) {
        int maxRainProb = 0;
        int rainHours = 0;
        int highRainHours = 0; // 70% 이상
        
        for (HourlyForecast forecast : forecasts) {
            int rainProb = forecast.getPrecipitationProbability();
            maxRainProb = Math.max(maxRainProb, rainProb);

            if (rainProb > 30) rainHours++;
            if (rainProb > 70) highRainHours++;

            // 비 오는 시간대 기록
            if (rainProb > 50) {
                // 예보 시간을 파싱해서 시간 추출
                String timeStr = forecast.getForecastTime();
                if (timeStr != null && timeStr.length() >= 2) {
                    int hour = Integer.parseInt(timeStr.substring(0, 2));
                
                    if (hour >= 6 && hour < 12) {
                        analysis.morningRain = true;
                    } else if (hour >= 12 && hour < 18) {
                        analysis.afternoonRain = true;
                    } else if (hour >= 18 && hour < 24) {
                        analysis.eveningRain = true;
                    }
                }
            }
        }
        
        analysis.maxRainProbability = maxRainProb;
        analysis.rainHours = rainHours;
        analysis.highRainHours = highRainHours;
        analysis.willRain = maxRainProb > 50;
    }
    
    /**
     * 온도 분석
     */
    private static void analyzeTemperature(List<HourlyForecast> forecasts, WeatherAnalysis analysis) {
        float minTemp = Float.MAX_VALUE;
        float maxTemp = Float.MIN_VALUE;
        float totalTemp = 0;
        
        for (HourlyForecast forecast : forecasts) {
            float temp = forecast.getTemperature();
            minTemp = Math.min(minTemp, temp);
            maxTemp = Math.max(maxTemp, temp);
            totalTemp += temp;
        }
        
        analysis.minTemperature = minTemp;
        analysis.maxTemperature = maxTemp;
        analysis.avgTemperature = totalTemp / forecasts.size();
        analysis.temperatureRange = maxTemp - minTemp;
        
        // 온도 특성 분석
        analysis.isHot = maxTemp > 30;
        analysis.isCold = minTemp < 5;
        analysis.isComfortable = analysis.avgTemperature >= 18 && analysis.avgTemperature <= 25;
    }
    
    /**
     * 날씨 변화 패턴 분석
     */
    private static void analyzeWeatherPattern(List<HourlyForecast> forecasts, WeatherAnalysis analysis) {
        String firstCondition = forecasts.get(0).getWeatherCondition();
        String lastCondition = forecasts.get(forecasts.size() - 1).getWeatherCondition();
        
        // 날씨 변화 추세
        if (isGettingWorse(firstCondition, lastCondition)) {
            analysis.weatherTrend = "악화";
        } else if (isGettingBetter(firstCondition, lastCondition)) {
            analysis.weatherTrend = "개선";
        } else {
            analysis.weatherTrend = "안정";
        }
        
        // 급격한 변화 감지
        for (int i = 1; i < forecasts.size(); i++) {
            String prev = forecasts.get(i-1).getWeatherCondition();
            String curr = forecasts.get(i).getWeatherCondition();
            
            if (isSignificantChange(prev, curr)) {
                analysis.hasSignificantChange = true;
                break;
            }
        }
    }
    
    /**
     * 시간대별 특이사항 분석
     */
    private static void analyzeTimeSpecificEvents(List<HourlyForecast> forecasts, WeatherAnalysis analysis) {
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        
        // 출근/등교 시간대 (7-9시) 날씨
        // 점심 시간대 (12-13시) 날씨
        // 퇴근/하교 시간대 (17-19시) 날씨
        
        for (HourlyForecast forecast : forecasts) {
            // 예보 시간을 파싱해서 시간 추출
            String timeStr = forecast.getForecastTime();
            if (timeStr != null && timeStr.length() >= 2) {
                int hour = Integer.parseInt(timeStr.substring(0, 2));

                if (hour >= 7 && hour <= 9 && forecast.getPrecipitationProbability() > 50) {
                    analysis.morningCommuteRain = true;
                }
                if (hour >= 17 && hour <= 19 && forecast.getPrecipitationProbability() > 50) {
                    analysis.eveningCommuteRain = true;
                }
            }
        }
    }
    
    /**
     * 고양이 스타일 간결한 분석 메시지 생성 (간단하고 명확하게)
     */
    private static String generateCatAnalysisMessage(WeatherAnalysis analysis) {
        // 강수 관련 메시지 (최우선) - 간단하게
        if (analysis.highRainHours > 0) {
            return "비가 많이 올 예정이다냥! ☔";
        } else if (analysis.rainHours > 0) {
            return "가끔 비가 올 수 있다냥! 🌦️";
        } else if (analysis.maxRainProbability > 30) {
            return "하늘이 흐릴 예정이다냥! ☁️";
        }

        // 온도 관련 메시지 (강수가 없을 때)
        if (analysis.isHot) {
            return "너무 더운 날이다냥! 🥵";
        } else if (analysis.isCold) {
            return "추운 날이다냥! 🥶";
        } else if (analysis.temperatureRange > 8) {
            return "온도 변화가 큰 날이다냥! 🌡️";
        } else {
            return "완벽한 날씨다냥! ☀️";
        }
    }
    
    // 헬퍼 메서드들
    private static boolean isGettingWorse(String from, String to) {
        return (from.equals("맑음") && (to.equals("비") || to.equals("소나기"))) ||
               (from.equals("구름많음") && (to.equals("비") || to.equals("소나기"))) ||
               (from.equals("맑음") && to.equals("흐림"));
    }
    
    private static boolean isGettingBetter(String from, String to) {
        return (from.equals("비") && to.equals("맑음")) ||
               (from.equals("소나기") && to.equals("맑음")) ||
               (from.equals("흐림") && to.equals("맑음"));
    }
    
    private static boolean isSignificantChange(String from, String to) {
        return (from.equals("맑음") && to.equals("비")) ||
               (from.equals("비") && to.equals("맑음")) ||
               (from.equals("맑음") && to.equals("소나기"));
    }
    
    /**
     * 날씨 분석 결과를 담는 내부 클래스
     */
    private static class WeatherAnalysis {
        boolean willRain = false;
        int maxRainProbability = 0;
        int rainHours = 0;
        int highRainHours = 0;
        
        boolean morningRain = false;
        boolean afternoonRain = false;
        boolean eveningRain = false;
        boolean morningCommuteRain = false;
        boolean eveningCommuteRain = false;
        
        float minTemperature = 0;
        float maxTemperature = 0;
        float avgTemperature = 0;
        float temperatureRange = 0;
        
        boolean isHot = false;
        boolean isCold = false;
        boolean isComfortable = false;
        
        String weatherTrend = "안정";
        boolean hasSignificantChange = false;
    }
}
