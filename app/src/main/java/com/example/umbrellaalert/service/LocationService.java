package com.example.umbrellaalert.service;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;

public class LocationService implements LocationListener {

    private static final String TAG = "LocationService";
    private static final long MIN_TIME_BW_UPDATES = 5000; // 5초
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 5; // 5미터

    private static LocationService instance;
    private final Context context;
    private LocationManager locationManager;
    private Location lastLocation;
    private boolean isLocationEnabled;
    private LocationCallback callback;

    // 위치 갱신 콜백 인터페이스
    public interface LocationCallback {
        void onLocationUpdate(Location location);
    }

    // 싱글톤 패턴
    public static synchronized LocationService getInstance(Context context) {
        if (instance == null) {
            instance = new LocationService(context.getApplicationContext());
        }
        return instance;
    }

    private LocationService(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.isLocationEnabled = false;
    }

    // 위치 업데이트 시작
    public void startLocationUpdates(LocationCallback callback) {
        this.callback = callback;

        // 권한 확인
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permissions not granted");
            return;
        }

        try {
            // 기존 업데이트 중지 (중복 방지)
            stopLocationUpdates();

            // GPS 위치 업데이트 요청 (높은 정확도)
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        this);
                isLocationEnabled = true;
                Log.d(TAG, "GPS location updates started");
            }

            // 네트워크 위치 업데이트 요청 (대체 제공자)
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        this);
                isLocationEnabled = true;
                Log.d(TAG, "Network location updates started");
            }

            // 수동 위치 업데이트 요청 (배터리 효율적)
            if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.PASSIVE_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        this);
                isLocationEnabled = true;
                Log.d(TAG, "Passive location updates started");
            }

            // 위치 제공자가 없는 경우 로그 출력
            if (!isLocationEnabled) {
                Log.e(TAG, "No location providers are enabled");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when requesting location updates", e);
        } catch (Exception e) {
            Log.e(TAG, "Error requesting location updates", e);
        }

        // 마지막 알려진 위치 가져오기 (적극적으로 모든 제공자에서 시도)
        try {
            Location gpsLocation = null;
            Location networkLocation = null;
            Location passiveLocation = null;

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                passiveLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }

            // 가장 정확하고 최근 위치 선택
            Location bestLocation = null;

            if (gpsLocation != null) {
                bestLocation = gpsLocation;
            }

            if (networkLocation != null && (bestLocation == null || networkLocation.getTime() > bestLocation.getTime())) {
                bestLocation = networkLocation;
            }

            if (passiveLocation != null && (bestLocation == null || passiveLocation.getTime() > bestLocation.getTime())) {
                bestLocation = passiveLocation;
            }

            if (bestLocation != null) {
                lastLocation = bestLocation;
                Log.d(TAG, "Found last known location: " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
            } else {
                Log.d(TAG, "No last known location found");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when getting last location", e);
        } catch (Exception e) {
            Log.e(TAG, "Error getting last location", e);
        }

        // 마지막 위치로 콜백 호출
        if (lastLocation != null && callback != null) {
            callback.onLocationUpdate(lastLocation);
        }
    }

    // 위치 업데이트 중지
    public void stopLocationUpdates() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
            isLocationEnabled = false;
            Log.d(TAG, "Location updates stopped");
        }
    }

    // 마지막 위치 가져오기
    public Location getLastLocation() {
        return lastLocation;
    }

    // 위치 활성화 상태 확인
    public boolean isLocationEnabled() {
        return isLocationEnabled;
    }

    // LocationListener 메소드 구현
    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
        if (callback != null) {
            callback.onLocationUpdate(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // API 레벨 29 이상에서는 더 이상 사용되지 않음
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "Location provider enabled: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "Location provider disabled: " + provider);
    }

    /**
     * 위치 권한 확인
     */
    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 가장 좋은 마지막 알려진 위치 가져오기
     */
    private Location getBestLastKnownLocation() throws SecurityException {
        Location gpsLocation = null;
        Location networkLocation = null;
        Location passiveLocation = null;

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
            passiveLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        }

        // 가장 정확하고 최근 위치 선택
        Location bestLocation = null;

        if (gpsLocation != null) {
            bestLocation = gpsLocation;
        }

        if (networkLocation != null && (bestLocation == null || networkLocation.getTime() > bestLocation.getTime())) {
            bestLocation = networkLocation;
        }

        if (passiveLocation != null && (bestLocation == null || passiveLocation.getTime() > bestLocation.getTime())) {
            bestLocation = passiveLocation;
        }

        return bestLocation;
    }

    /**
     * 마지막으로 알려진 위치 가져오기 (동기)
     */
    public Location getLastKnownLocation() {
        if (!hasLocationPermission()) {
            return null;
        }

        try {
            return getBestLastKnownLocation();
        } catch (SecurityException e) {
            Log.e(TAG, "위치 권한이 없습니다", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "위치 가져오기 실패", e);
            return null;
        }
    }
}