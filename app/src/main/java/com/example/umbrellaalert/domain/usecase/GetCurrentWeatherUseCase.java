package com.example.umbrellaalert.domain.usecase;

import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.domain.repository.WeatherRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 현재 날씨 정보를 가져오는 UseCase
 * 비즈니스 로직을 캡슐화하여 ViewModel에서 사용
 */
@Singleton
public class GetCurrentWeatherUseCase {

    private final WeatherRepository weatherRepository;

    @Inject
    public GetCurrentWeatherUseCase(WeatherRepository weatherRepository) {
        this.weatherRepository = weatherRepository;
    }

    /**
     * 위치 기반 현재 날씨 정보 조회
     * @param latitude 위도
     * @param longitude 경도
     * @return 날씨 정보
     */
    public Weather execute(double latitude, double longitude) {
        return weatherRepository.getCurrentWeather(latitude, longitude);
    }

    /**
     * 캐시된 날씨 정보 조회
     * @param locationStr 위치 문자열
     * @return 캐시된 날씨 정보
     */
    public Weather getCachedWeather(String locationStr) {
        return weatherRepository.getCachedWeather(locationStr);
    }
}
