package com.example.umbrellaalert.domain.repository;

import com.example.umbrellaalert.data.model.Location;

import java.util.List;

/**
 * 위치 데이터 Repository 인터페이스
 * Domain 계층에서 정의하여 Data 계층의 구현체와 분리
 */
public interface LocationRepository {

    /**
     * 모든 위치 조회
     * @return 위치 목록
     */
    List<Location> getAllLocations();

    /**
     * ID로 위치 조회
     * @param id 위치 ID
     * @return 위치 정보
     */
    Location getLocationById(int id);

    /**
     * 새 위치 추가
     * @param location 위치 정보
     * @return 추가된 위치의 ID
     */
    long insertLocation(Location location);

    /**
     * 위치 정보 업데이트
     * @param location 위치 정보
     */
    void updateLocation(Location location);

    /**
     * 위치 삭제
     * @param id 위치 ID
     */
    void deleteLocation(int id);

    /**
     * 현재 위치 조회 (GPS)
     * @return 현재 위치
     */
    Location getCurrentLocation();

    /**
     * 위치 권한 확인
     * @return 권한 여부
     */
    boolean hasLocationPermission();

    /**
     * 위치 서비스 활성화 여부 확인
     * @return 활성화 여부
     */
    boolean isLocationServiceEnabled();
}
