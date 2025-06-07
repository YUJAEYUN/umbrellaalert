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

import com.example.umbrellaalert.R;
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.databinding.FragmentHomeBinding;
import com.example.umbrellaalert.service.LocationService;
import com.example.umbrellaalert.ui.home.WeatherViewModel;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private FragmentHomeBinding binding;
    private WeatherViewModel weatherViewModel;
    private LocationService locationService;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Activity의 공유 ViewModel 사용
        weatherViewModel = ((com.example.umbrellaalert.ui.main.MainActivity) requireActivity()).getSharedWeatherViewModel();
        
        // LocationService 초기화
        locationService = LocationService.getInstance(requireContext());
        
        // UI 관찰자 설정
        setupObservers();
        
        // 위치 권한 확인 및 날씨 정보 로드
        checkLocationPermissionAndLoadWeather();
    }

    private void setupObservers() {
        // 날씨 데이터 관찰
        weatherViewModel.getWeatherData().observe(getViewLifecycleOwner(), this::updateWeatherDisplay);
        
        // 위치명 관찰
        weatherViewModel.getLocationName().observe(getViewLifecycleOwner(), locationName -> {
            if (locationName != null) {
                binding.locationText.setText(locationName);
            }
        });
        
        // 로딩 상태 관찰
        weatherViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // 로딩 상태에 따른 UI 업데이트
            if (isLoading) {
                binding.catMessage.setText("날씨 정보를 불러오는 중이다냥~");
            }
        });
        
        // 고양이 이미지 관찰
        weatherViewModel.getCatImageResource().observe(getViewLifecycleOwner(), imageResource -> {
            if (imageResource != null) {
                binding.catImage.setImageResource(imageResource);
            }
        });
        
        // 고양이 메시지 관찰
        weatherViewModel.getCatMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                binding.catMessage.setText(message);
            }
        });
        
        // 온도 메시지 관찰
        weatherViewModel.getTemperatureMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                binding.temperatureMessage.setText(message);
                binding.temperatureMessage.setVisibility(View.VISIBLE);
            } else {
                binding.temperatureMessage.setVisibility(View.GONE);
            }
        });
        
        // 우산 메시지 관찰
        weatherViewModel.getUmbrellaMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                binding.umbrellaMessage.setText(message);
                binding.umbrellaMessage.setVisibility(View.VISIBLE);
            } else {
                binding.umbrellaMessage.setVisibility(View.GONE);
            }
        });
    }

    private void checkLocationPermissionAndLoadWeather() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            loadWeatherWithLocation();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void loadWeatherWithLocation() {
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

    private void updateWeatherDisplay(Weather weather) {
        if (weather == null) {
            Log.w(TAG, "받은 날씨 데이터가 null");
            return;
        }

        Log.d(TAG, "HomeFragment에서 받은 날씨 데이터: " + weather.getTemperature() + "°C, 상태: " + weather.getWeatherCondition());

        // 온도 표시
        binding.temperatureText.setText(String.format(Locale.getDefault(), "%.1f°C", weather.getTemperature()));

        // 날씨 상태 표시
        binding.weatherCondition.setText(weatherViewModel.getWeatherConditionText(weather.getWeatherCondition()));

        // 날씨에 따른 프레임 변경
        updateWeatherFrame(weather.getWeatherCondition());
    }

    /**
     * 날씨 상태에 따라 프레임 배경 변경
     */
    private void updateWeatherFrame(String weatherCondition) {
        int frameResource;

        if (weatherCondition == null) {
            frameResource = R.drawable.weather_frame_default;
        } else {
            String condition = weatherCondition.toLowerCase();

            if (condition.contains("clear") || condition.contains("sunny") || condition.equals("맑음")) {
                frameResource = R.drawable.weather_frame_sunny;
            } else if (condition.contains("rain") || condition.contains("drizzle") ||
                      condition.contains("thunderstorm") || condition.equals("비") ||
                      condition.equals("이슬비") || condition.equals("뇌우") || condition.equals("소나기")) {
                frameResource = R.drawable.weather_frame_rainy;
            } else if (condition.contains("snow") || condition.equals("눈")) {
                frameResource = R.drawable.weather_frame_snowy;
            } else if (condition.contains("cloud") || condition.contains("overcast") ||
                      condition.equals("구름많음") || condition.equals("흐림")) {
                frameResource = R.drawable.weather_frame_cloudy;
            } else {
                frameResource = R.drawable.weather_frame_default;
            }
        }

        // 프레임 배경 변경
        binding.weatherFrameContainer.setBackgroundResource(frameResource);

        Log.d(TAG, "날씨 프레임 변경: " + weatherCondition + " -> " + frameResource);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadWeatherWithLocation();
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
