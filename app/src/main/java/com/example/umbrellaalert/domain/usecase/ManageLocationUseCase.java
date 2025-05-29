package com.example.umbrellaalert.domain.usecase;

import com.example.umbrellaalert.data.model.Location;
import com.example.umbrellaalert.domain.repository.LocationRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 위치 관리 UseCase
 * 사용자의 위치 정보 관리 비즈니스 로직
 */
@Singleton
public class ManageLocationUseCase {

    private final LocationRepository locationRepository;

    @Inject
    public ManageLocationUseCase(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    /**
     * 모든 저장된 위치 조회
     */
    public List<Location> getAllLocations() {
        return locationRepository.getAllLocations();
    }

    /**
     * 새 위치 추가
     */
    public long addLocation(Location location) {
        // 중복 위치 체크
        List<Location> existingLocations = locationRepository.getAllLocations();
        for (Location existing : existingLocations) {
            if (isSameLocation(existing, location)) {
                return -1; // 중복 위치
            }
        }
        
        return locationRepository.insertLocation(location);
    }

    /**
     * 위치 삭제
     */
    public void deleteLocation(int locationId) {
        locationRepository.deleteLocation(locationId);
    }

    /**
     * 위치 업데이트
     */
    public void updateLocation(Location location) {
        locationRepository.updateLocation(location);
    }

    /**
     * 기본 위치 설정
     */
    public void setDefaultLocation(int locationId) {
        // 모든 위치의 기본 설정 해제
        List<Location> locations = locationRepository.getAllLocations();
        for (Location location : locations) {
            if (location.isDefault()) {
                location.setDefault(false);
                locationRepository.updateLocation(location);
            }
        }
        
        // 선택된 위치를 기본으로 설정
        Location defaultLocation = locationRepository.getLocationById(locationId);
        if (defaultLocation != null) {
            defaultLocation.setDefault(true);
            locationRepository.updateLocation(defaultLocation);
        }
    }

    /**
     * 기본 위치 조회
     */
    public Location getDefaultLocation() {
        List<Location> locations = locationRepository.getAllLocations();
        for (Location location : locations) {
            if (location.isDefault()) {
                return location;
            }
        }
        return null;
    }

    /**
     * 두 위치가 같은지 확인 (반경 100m 이내)
     */
    private boolean isSameLocation(Location location1, Location location2) {
        double distance = calculateDistance(
            location1.getLatitude(), location1.getLongitude(),
            location2.getLatitude(), location2.getLongitude()
        );
        return distance < 0.1; // 100m 이내
    }

    /**
     * 두 지점 간의 거리 계산 (km)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구 반지름 (km)
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}
