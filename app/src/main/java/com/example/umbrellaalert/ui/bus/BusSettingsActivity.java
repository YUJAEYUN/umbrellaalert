package com.example.umbrellaalert.ui.bus;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.umbrellaalert.databinding.ActivityBusSettingsBinding;
import com.example.umbrellaalert.data.model.BusStop;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.ArrayList;
import java.util.List;

/**
 * 버스 설정 화면 - 지도에서 정류장 선택
 */
public class BusSettingsActivity extends AppCompatActivity implements OnMapReadyCallback {
    
    private static final String TAG = "BusSettingsActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    
    private ActivityBusSettingsBinding binding;
    private BusSettingsViewModel viewModel;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;
    private FusedLocationProviderClient fusedLocationClient;
    
    private List<Marker> busStopMarkers = new ArrayList<>();
    private BusStopAdapter busStopAdapter;
    private BusArrivalAdapter busArrivalAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBusSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // ViewModel 초기화
        viewModel = new ViewModelProvider(this).get(BusSettingsViewModel.class);
        
        // 위치 서비스 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        
        // UI 설정
        setupUI();
        
        // 지도 초기화
        setupMap();
        
        // RecyclerView 설정
        setupRecyclerView();
        
        // 데이터 관찰
        observeData();
        
        // 위치 권한 확인
        checkLocationPermission();
    }

    private void setupUI() {
        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener(v -> finish());
        
        // 현재 위치 버튼
        binding.btnCurrentLocation.setOnClickListener(v -> moveToCurrentLocation());
        
        // 새로고침 버튼
        binding.btnRefresh.setOnClickListener(v -> refreshNearbyStops());
    }

    private void setupMap() {
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(binding.mapFragment.getId());
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                .add(binding.mapFragment.getId(), mapFragment)
                .commit();
        }
        mapFragment.getMapAsync(this);
    }

    private void setupRecyclerView() {
        // 정류장 목록 RecyclerView
        busStopAdapter = new BusStopAdapter();
        binding.recyclerViewBusStops.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewBusStops.setAdapter(busStopAdapter);

        // 정류장 클릭 리스너
        busStopAdapter.setOnBusStopClickListener(busStop -> {
            // 지도에서 해당 정류장으로 이동
            if (naverMap != null) {
                LatLng position = new LatLng(busStop.getGpsLati(), busStop.getGpsLong());
                naverMap.moveCamera(com.naver.maps.map.CameraUpdate.scrollTo(position));
            }

            // 정류장 선택 및 버스 목록 조회
            viewModel.selectBusStop(busStop);
            viewModel.loadBusArrivals(busStop);
        });

        // 버스 도착 정보 RecyclerView
        busArrivalAdapter = new BusArrivalAdapter();
        binding.recyclerViewBusArrivals.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewBusArrivals.setAdapter(busArrivalAdapter);

        // 버스 등록 클릭 리스너
        busArrivalAdapter.setOnBusArrivalClickListener(busArrival -> {
            BusStop selectedStop = viewModel.getSelectedBusStop().getValue();
            if (selectedStop != null) {
                viewModel.registerBus(selectedStop, busArrival);
                Toast.makeText(this, busArrival.getRouteNo() + "번 버스가 등록되었습니다", Toast.LENGTH_SHORT).show();
                finish(); // 등록 후 화면 닫기
            }
        });
    }

    private void observeData() {
        // 근처 정류장 목록
        viewModel.getNearbyBusStops().observe(this, busStops -> {
            if (busStops != null) {
                busStopAdapter.updateBusStops(busStops);
                updateMapMarkers(busStops);
                
                if (busStops.isEmpty()) {
                    binding.emptyStateText.setVisibility(View.VISIBLE);
                    binding.emptyStateText.setText("주변에 정류장이 없습니다");
                } else {
                    binding.emptyStateText.setVisibility(View.GONE);
                }
            }
        });
        
        // 로딩 상태
        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        
        // 에러 메시지
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        
        // 선택된 정류장
        viewModel.getSelectedBusStop().observe(this, busStop -> {
            if (busStop != null) {
                // 하단 시트 표시
                showBusStopBottomSheet(busStop);
            } else {
                // 하단 시트 숨기기
                hideBusStopBottomSheet();
            }
        });

        // 버스 도착 정보
        viewModel.getBusArrivals().observe(this, busArrivals -> {
            if (busArrivals != null) {
                busArrivalAdapter.updateBusArrivals(busArrivals);

                // 빈 목록 처리
                if (busArrivals.isEmpty()) {
                    binding.recyclerViewBusArrivals.setVisibility(View.GONE);
                    binding.tvNoBusMessage.setVisibility(View.VISIBLE);
                } else {
                    binding.recyclerViewBusArrivals.setVisibility(View.VISIBLE);
                    binding.tvNoBusMessage.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;

        // 임시: API 키가 설정되지 않은 경우 여기서 직접 설정
        // NaverMapSdk.getInstance(this).setClient(
        //     new NaverMapSdk.NaverCloudPlatformClient("YOUR_CLIENT_ID_HERE")
        // );
        
        // 지도 설정
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.NoFollow);
        naverMap.getUiSettings().setLocationButtonEnabled(false); // 커스텀 버튼 사용
        
        // 지도 클릭 리스너
        naverMap.setOnMapClickListener((point, coord) -> {
            // 정류장 선택 해제
            viewModel.clearSelectedBusStop();
            hideBusStopBottomSheet();
        });
        
        // 현재 위치로 이동
        moveToCurrentLocation();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) {
                naverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void moveToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null && naverMap != null) {
                        LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
                        naverMap.moveCamera(com.naver.maps.map.CameraUpdate.scrollTo(currentPosition));
                        
                        // 현재 위치 주변 정류장 검색
                        viewModel.loadNearbyBusStops(location.getLatitude(), location.getLongitude());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "현재 위치 가져오기 실패", e);
                    Toast.makeText(this, "현재 위치를 가져올 수 없습니다", Toast.LENGTH_SHORT).show();
                    
                    // 기본 위치 (세종시청)
                    LatLng defaultPosition = new LatLng(36.4800, 127.2890);
                    if (naverMap != null) {
                        naverMap.moveCamera(com.naver.maps.map.CameraUpdate.scrollTo(defaultPosition));
                        viewModel.loadNearbyBusStops(36.4800, 127.2890);
                    }
                });
        } else {
            // 권한이 없으면 기본 위치로
            LatLng defaultPosition = new LatLng(36.4800, 127.2890);
            if (naverMap != null) {
                naverMap.moveCamera(com.naver.maps.map.CameraUpdate.scrollTo(defaultPosition));
                viewModel.loadNearbyBusStops(36.4800, 127.2890);
            }
        }
    }

    private void refreshNearbyStops() {
        if (naverMap != null) {
            LatLng center = naverMap.getCameraPosition().target;
            viewModel.loadNearbyBusStops(center.latitude, center.longitude);
        }
    }

    private void updateMapMarkers(List<BusStop> busStops) {
        if (naverMap == null) return;
        
        // 기존 마커 제거
        for (Marker marker : busStopMarkers) {
            marker.setMap(null);
        }
        busStopMarkers.clear();
        
        // 새 마커 추가
        for (BusStop busStop : busStops) {
            Marker marker = new Marker();
            marker.setPosition(new LatLng(busStop.getGpsLati(), busStop.getGpsLong()));
            // 텍스트 제거 - 마커만 표시
            marker.setWidth(25);  // 마커 크기 더 줄임
            marker.setHeight(25); // 마커 크기 더 줄임
            marker.setMap(naverMap);
            
            // 마커 클릭 리스너
            marker.setOnClickListener(overlay -> {
                viewModel.selectBusStop(busStop);
                viewModel.loadBusArrivals(busStop); // 버스 도착 정보 로드
                return true;
            });
            
            busStopMarkers.add(marker);
        }
    }

    private void showBusStopBottomSheet(BusStop busStop) {
        // TODO: 하단 시트에서 버스 목록 표시
        binding.bottomSheetLayout.setVisibility(View.VISIBLE);
        binding.tvSelectedStopName.setText(busStop.getNodeName());
        
        // 해당 정류장의 버스 도착 정보 로드
        viewModel.loadBusArrivals(busStop);
    }

    private void hideBusStopBottomSheet() {
        binding.bottomSheetLayout.setVisibility(View.GONE);
    }
}
