package com.example.umbrellaalert.ui.home;

import android.app.Application;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.umbrellaalert.service.LocationService;

/**
 * 위치 정보를 관리하는 ViewModel
 */
public class LocationViewModel extends AndroidViewModel {

    private static final String TAG = "LocationViewModel";

    private final LocationService locationService;
    private final MutableLiveData<Location> currentLocation = new MutableLiveData<>();
    private final MutableLiveData<Boolean> locationPermissionGranted = new MutableLiveData<>(false);

    private LocationCallback locationCallback;

    public interface LocationCallback {
        void onLocationUpdate(Location location);
    }

    public LocationViewModel(@NonNull Application application) {
        super(application);
        locationService = LocationService.getInstance(application);
    }

    /**
     * 위치 업데이트 시작
     */
    public void startLocationUpdates() {
        Log.d(TAG, "Starting location updates");

        // 기존 업데이트 중지 (중복 방지)
        stopLocationUpdates();

        // 새로운 위치 업데이트 시작
        locationService.startLocationUpdates(new LocationService.LocationCallback() {
            @Override
            public void onLocationUpdate(Location location) {
                if (location != null) {
                    Log.d(TAG, "Location update received: " + location.getLatitude() + ", " + location.getLongitude());
                    currentLocation.postValue(location);

                    // 콜백 호출
                    if (locationCallback != null) {
                        locationCallback.onLocationUpdate(location);
                    }
                } else {
                    Log.d(TAG, "Received null location update");
                }
            }
        });

        // 마지막 위치 확인
        Location lastLocation = locationService.getLastLocation();
        if (lastLocation != null) {
            Log.d(TAG, "Using last known location: " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
            currentLocation.postValue(lastLocation);

            // 콜백 호출
            if (locationCallback != null) {
                locationCallback.onLocationUpdate(lastLocation);
            }
        } else {
            Log.d(TAG, "No last known location available");
        }
    }

    /**
     * 위치 업데이트 중지
     */
    public void stopLocationUpdates() {
        locationService.stopLocationUpdates();
    }

    /**
     * 마지막 위치 가져오기
     */
    public Location getLastLocation() {
        return locationService.getLastLocation();
    }

    /**
     * 위치 권한 상태 설정
     */
    public void setLocationPermissionGranted(boolean granted) {
        locationPermissionGranted.setValue(granted);

        if (granted) {
            startLocationUpdates();
        }
    }

    /**
     * 위치 콜백 설정
     */
    public void setLocationCallback(LocationCallback callback) {
        this.locationCallback = callback;
    }

    /**
     * 위치 권한 상태 가져오기
     */
    public LiveData<Boolean> getLocationPermissionGranted() {
        return locationPermissionGranted;
    }

    /**
     * 현재 위치 가져오기
     */
    public LiveData<Location> getCurrentLocation() {
        return currentLocation;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopLocationUpdates();
    }
}
