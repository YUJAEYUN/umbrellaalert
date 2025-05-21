package com.example.umbrellaalert.data.repository;

import android.content.Context;

import com.example.umbrellaalert.data.database.DatabaseHelper;
import com.example.umbrellaalert.data.database.LocationDao;
import com.example.umbrellaalert.data.model.Location;

import java.util.List;

/**
 * 위치 데이터 관리를 위한 Repository 클래스
 * 데이터 소스(로컬 DB)를 추상화하여 ViewModel에 제공
 */
public class LocationRepository {

    private final Context context;
    private final LocationDao locationDao;
    
    public LocationRepository(Context context) {
        this.context = context.getApplicationContext();
        this.locationDao = new LocationDao(DatabaseHelper.getInstance(context));
    }
    
    /**
     * 모든 위치 정보 가져오기
     */
    public List<Location> getAllLocations() {
        return locationDao.getAllLocations();
    }
    
    /**
     * 자주 가는 위치 가져오기
     */
    public List<Location> getFrequentLocations() {
        return locationDao.getFrequentLocations();
    }
    
    /**
     * 위치 추가
     */
    public long insertLocation(Location location) {
        return locationDao.insertLocation(location);
    }
    
    /**
     * 위치 업데이트
     */
    public int updateLocation(Location location) {
        return locationDao.updateLocation(location);
    }
    
    /**
     * 위치 삭제
     */
    public int deleteLocation(int id) {
        return locationDao.deleteLocation(id);
    }
}
