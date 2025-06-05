package com.example.umbrellaalert.domain.repository;


import com.example.umbrellaalert.data.model.Weather;

import java.util.List;

/**
 * 날씨 데이터 Repository 인터페이스
 * Domain 계층에서 정의하여 Data 계층의 구현체와 분리
 */
public interface WeatherRepository {

    /**
     * 현재 위치의 날씨 정보 조회
     * @param latitude 위도
     * @param longitude 경도
     * @return 날씨 정보
     */
    Weather getCurrentWeather(double latitude, double longitude);

    /**
     * 캐시된 날씨 정보 조회
     * @param locationStr 위치 문자열
     * @return 캐시된 날씨 정보
     */
    Weather getCachedWeather(String locationStr);



    /**
     * 날씨 정보 저장
     * @param weather 날씨 정보
     * @return 저장된 ID
     */
    long saveWeather(Weather weather);

    /**
     * 오래된 날씨 데이터 정리
     * @param threshold 임계값 (timestamp)
     * @return 삭제된 행 수
     */
    int cleanupOldWeatherData(long threshold);

    /**
     * 고양이 메시지 생성
     * @param weather 날씨 정보
     * @return 고양이 메시지
     */
    String getCatMessage(Weather weather);
}
