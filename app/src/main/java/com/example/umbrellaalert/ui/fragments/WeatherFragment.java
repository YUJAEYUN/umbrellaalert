package com.example.umbrellaalert.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.umbrellaalert.data.model.HourlyForecast;
import com.example.umbrellaalert.databinding.FragmentWeatherBinding;
import com.example.umbrellaalert.service.LocationService;
import com.example.umbrellaalert.ui.home.WeatherViewModel;
import com.example.umbrellaalert.ui.adapter.HourlyForecastAdapter;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WeatherFragment extends Fragment {

    private static final String TAG = "WeatherFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;

    private FragmentWeatherBinding binding;
    private WeatherViewModel weatherViewModel;
    private LocationService locationService;
    private HourlyForecastAdapter forecastAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWeatherBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // ViewModel 초기화
        weatherViewModel = new ViewModelProvider(this).get(WeatherViewModel.class);
        
        // LocationService 초기화
        locationService = LocationService.getInstance(requireContext());
        
        // RecyclerView 설정
        setupRecyclerView();
        
        // UI 관찰자 설정
        setupObservers();
        
        // 위치 권한 확인 및 예보 정보 로드
        checkLocationPermissionAndLoadForecast();
    }

    private void setupRecyclerView() {
        forecastAdapter = new HourlyForecastAdapter();
        binding.forecastRecyclerView.setLayoutManager(
            new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.forecastRecyclerView.setAdapter(forecastAdapter);
    }

    private void setupObservers() {
        // 6시간 예보 데이터 관찰
        weatherViewModel.getHourlyForecastData().observe(getViewLifecycleOwner(), this::updateForecastDisplay);
        
        // 예보 업데이트 시간 관찰
        weatherViewModel.getForecastUpdateTime().observe(getViewLifecycleOwner(), updateTime -> {
            if (updateTime != null) {
                binding.updateTime.setText(updateTime);
            }
        });
    }

    private void checkLocationPermissionAndLoadForecast() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            loadForecastWithLocation();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void loadForecastWithLocation() {
        locationService.startLocationUpdates(new LocationService.LocationCallback() {
            @Override
            public void onLocationUpdate(Location location) {
                Log.d(TAG, "위치 수신: " + location.getLatitude() + ", " + location.getLongitude());
                weatherViewModel.updateWeatherWithLocation(location);
            }
        });

        // 마지막 위치가 있으면 즉시 사용
        Location lastLocation = locationService.getLastLocation();
        if (lastLocation != null) {
            Log.d(TAG, "마지막 위치 사용: " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
            weatherViewModel.updateWeatherWithLocation(lastLocation);
        } else {
            Log.w(TAG, "위치 정보 없음 - 기본 위치 사용");
            weatherViewModel.updateWeatherWithDefaultLocation();
        }
    }

    private void updateForecastDisplay(List<HourlyForecast> forecasts) {
        if (forecasts != null && !forecasts.isEmpty()) {
            Log.d(TAG, "6시간 예보 데이터 " + forecasts.size() + "개 수신");
            forecastAdapter.setForecasts(forecasts);
        } else {
            Log.w(TAG, "예보 데이터가 없음");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadForecastWithLocation();
            } else {
                weatherViewModel.updateWeatherWithDefaultLocation();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (locationService != null) {
            locationService.stopLocationUpdates();
        }
        binding = null;
    }
}
