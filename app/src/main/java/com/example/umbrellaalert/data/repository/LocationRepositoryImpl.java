package com.example.umbrellaalert.data.repository;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;

import androidx.core.app.ActivityCompat;

import com.example.umbrellaalert.data.database.DatabaseHelper;
import com.example.umbrellaalert.data.database.LocationDao;
import com.example.umbrellaalert.data.model.Location;
import com.example.umbrellaalert.domain.repository.LocationRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 위치 데이터 관리를 위한 Repository 구현체
 * 데이터 소스(로컬 DB)를 추상화하여 ViewModel에 제공
 */
@Singleton
public class LocationRepositoryImpl implements LocationRepository {

    private final Context context;
    private final LocationDao locationDao;

    @Inject
    public LocationRepositoryImpl(Context context) {
        this.context = context.getApplicationContext();
        this.locationDao = new LocationDao(DatabaseHelper.getInstance(context));
    }

    /**
     * 모든 위치 정보 가져오기
     */
    @Override
    public List<Location> getAllLocations() {
        return locationDao.getAllLocations();
    }

    /**
     * ID로 위치 조회
     */
    @Override
    public Location getLocationById(int id) {
        List<Location> locations = locationDao.getAllLocations();
        for (Location location : locations) {
            if (location.getId() == id) {
                return location;
            }
        }
        return null;
    }

    /**
     * 위치 추가
     */
    @Override
    public long insertLocation(Location location) {
        return locationDao.insertLocation(location);
    }

    /**
     * 위치 업데이트
     */
    @Override
    public void updateLocation(Location location) {
        locationDao.updateLocation(location);
    }

    /**
     * 위치 삭제
     */
    @Override
    public void deleteLocation(int id) {
        locationDao.deleteLocation(id);
    }

    /**
     * 현재 위치 조회 (GPS)
     */
    @Override
    public Location getCurrentLocation() {
        // TODO: GPS를 통한 현재 위치 조회 구현
        // 현재는 기본 위치 반환 (서울)
        return new Location(0, "현재 위치", 37.5665, 126.9780, true, true);
    }

    /**
     * 위치 권한 확인
     */
    @Override
    public boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
               ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 위치 서비스 활성화 여부 확인
     */
    @Override
    public boolean isLocationServiceEnabled() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null &&
               (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    /**
     * 자주 가는 위치 가져오기 (기존 메서드 유지)
     */
    public List<Location> getFrequentLocations() {
        return locationDao.getFrequentLocations();
    }
}
