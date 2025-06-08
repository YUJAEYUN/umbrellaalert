package com.example.umbrellaalert.ui.location;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.umbrellaalert.R;
import com.example.umbrellaalert.databinding.ActivityMapLocationPickerBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.util.FusedLocationSource;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * 지도에서 위치를 선택하는 Activity
 */
public class MapLocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {
    
    private static final String TAG = "MapLocationPicker";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    
    public static final String EXTRA_LOCATION_NAME = "location_name";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    
    private ActivityMapLocationPickerBinding binding;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker selectedLocationMarker;
    private Geocoder geocoder;
    
    private double selectedLatitude = 0;
    private double selectedLongitude = 0;
    private String selectedLocationName = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapLocationPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        geocoder = new Geocoder(this, Locale.getDefault());
        
        // UI 설정
        setupUI();
        
        // 지도 초기화
        setupMap();
    }
    
    private void setupUI() {
        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener(v -> finish());
        
        // 현재 위치 버튼
        binding.btnCurrentLocation.setOnClickListener(v -> moveToCurrentLocation());
        
        // 위치 선택 완료 버튼
        binding.btnSelectLocation.setOnClickListener(v -> {
            if (selectedLatitude != 0 && selectedLongitude != 0) {
                returnSelectedLocation();
            } else {
                Toast.makeText(this, "지도에서 위치를 선택해주세요", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 초기 상태에서는 선택 버튼 비활성화
        binding.btnSelectLocation.setEnabled(false);
    }
    
    private void setupMap() {
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                .add(R.id.map_fragment, mapFragment)
                .commit();
        }
        mapFragment.getMapAsync(this);
    }
    
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;

        // 지도 설정
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.NoFollow);
        naverMap.getUiSettings().setLocationButtonEnabled(false);

        // 지도 클릭 리스너
        naverMap.setOnMapClickListener((point, coord) -> {
            selectLocation(coord.latitude, coord.longitude);
        });

        // 기본 위치로 먼저 이동 (세종시청)
        LatLng defaultPosition = new LatLng(36.4800, 127.2890);
        naverMap.moveCamera(CameraUpdate.scrollTo(defaultPosition));

        // 현재 위치로 이동 시도
        moveToCurrentLocation();
    }
    
    private void selectLocation(double latitude, double longitude) {
        selectedLatitude = latitude;
        selectedLongitude = longitude;
        
        // 기존 마커 제거
        if (selectedLocationMarker != null) {
            selectedLocationMarker.setMap(null);
        }
        
        // 새 마커 추가
        selectedLocationMarker = new Marker();
        selectedLocationMarker.setPosition(new LatLng(latitude, longitude));
        selectedLocationMarker.setMap(naverMap);
        
        // 주소 정보 가져오기
        getAddressFromCoordinates(latitude, longitude);
        
        // 선택 버튼 활성화
        binding.btnSelectLocation.setEnabled(true);
        
        Log.d(TAG, String.format("위치 선택: %.6f, %.6f", latitude, longitude));
    }
    
    private void getAddressFromCoordinates(double latitude, double longitude) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                
                // 주소 정보 조합
                StringBuilder addressBuilder = new StringBuilder();
                
                if (address.getThoroughfare() != null) {
                    addressBuilder.append(address.getThoroughfare());
                }
                if (address.getSubThoroughfare() != null) {
                    if (addressBuilder.length() > 0) addressBuilder.append(" ");
                    addressBuilder.append(address.getSubThoroughfare());
                }
                if (address.getSubLocality() != null) {
                    if (addressBuilder.length() > 0) addressBuilder.append(", ");
                    addressBuilder.append(address.getSubLocality());
                }
                if (address.getLocality() != null) {
                    if (addressBuilder.length() > 0) addressBuilder.append(", ");
                    addressBuilder.append(address.getLocality());
                }
                
                selectedLocationName = addressBuilder.toString();
                if (selectedLocationName.isEmpty()) {
                    selectedLocationName = String.format("위치 (%.4f, %.4f)", latitude, longitude);
                }
                
                // UI 업데이트
                binding.textSelectedLocation.setText(selectedLocationName);
                
            } else {
                selectedLocationName = String.format("위치 (%.4f, %.4f)", latitude, longitude);
                binding.textSelectedLocation.setText(selectedLocationName);
            }
        } catch (IOException e) {
            Log.e(TAG, "주소 변환 실패", e);
            selectedLocationName = String.format("위치 (%.4f, %.4f)", latitude, longitude);
            binding.textSelectedLocation.setText(selectedLocationName);
        }
    }
    
    private void moveToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null && naverMap != null) {
                        LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
                        naverMap.moveCamera(CameraUpdate.scrollTo(currentPosition));
                        Log.d(TAG, "현재 위치로 이동: " + location.getLatitude() + ", " + location.getLongitude());
                    } else {
                        Log.d(TAG, "현재 위치가 null이므로 기본 위치 사용");
                        moveToDefaultLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "현재 위치 가져오기 실패", e);
                    moveToDefaultLocation();
                });
        } else {
            Log.d(TAG, "위치 권한이 없으므로 기본 위치 사용");
            moveToDefaultLocation();

            // 권한 요청
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * 기본 위치로 이동 (세종시청)
     */
    private void moveToDefaultLocation() {
        LatLng defaultPosition = new LatLng(36.4800, 127.2890);
        if (naverMap != null) {
            naverMap.moveCamera(CameraUpdate.scrollTo(defaultPosition));
            Log.d(TAG, "기본 위치로 이동: 세종시청");
        }

        // 사용자에게 안내 메시지 표시
        Toast.makeText(this, "기본 위치(세종시)에서 시작합니다. 지도를 클릭하여 원하는 위치를 선택하세요.", Toast.LENGTH_LONG).show();
    }
    
    private void returnSelectedLocation() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_LOCATION_NAME, selectedLocationName);
        resultIntent.putExtra(EXTRA_LATITUDE, selectedLatitude);
        resultIntent.putExtra(EXTRA_LONGITUDE, selectedLongitude);
        
        setResult(RESULT_OK, resultIntent);
        finish();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) {
                naverMap.setLocationTrackingMode(LocationTrackingMode.None);
                Log.d(TAG, "위치 권한이 거부됨, 기본 위치 사용");
                moveToDefaultLocation();
            } else {
                Log.d(TAG, "위치 권한이 승인됨, 현재 위치로 이동");
                moveToCurrentLocation();
            }
            return;
        }

        // 직접 권한 요청 처리
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "위치 권한 승인됨");
                moveToCurrentLocation();
            } else {
                Log.d(TAG, "위치 권한 거부됨, 기본 위치 사용");
                moveToDefaultLocation();
            }
            return;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
