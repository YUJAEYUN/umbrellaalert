package com.example.umbrellaalert.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

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
import com.example.umbrellaalert.ui.location.LocationViewModel;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private FragmentHomeBinding binding;
    private WeatherViewModel weatherViewModel;
    private LocationViewModel locationViewModel;
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

        // LocationViewModel 초기화
        locationViewModel = new ViewModelProvider(this).get(LocationViewModel.class);

        // LocationService 초기화
        locationService = LocationService.getInstance(requireContext());

        // UI 관찰자 설정
        setupObservers();

        // 위치 권한 확인 및 날씨 정보 로드
        checkLocationPermissionAndLoadWeather();

        // 등록된 위치들의 날씨 체크 (우산 필요 여부 종합 판단)
        checkAllLocationsWeather();
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
        
        // 로딩 상태 관찰 (애니메이션 포함)
        weatherViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // 로딩 상태에 따른 UI 업데이트
            if (isLoading) {
                binding.catMessage.setText("날씨 정보를 불러오는 중이다냥~");
                showLoadingAnimation();
            } else {
                hideLoadingAnimation();
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
    }

    /**
     * 등록된 모든 위치의 날씨를 체크하여 우산 필요 여부 종합 판단
     */
    private void checkAllLocationsWeather() {
        locationViewModel.checkAllLocationsWeather(new LocationViewModel.WeatherCheckCallback() {
            @Override
            public void onWeatherCheckCompleted(boolean anyLocationNeedsUmbrella) {
                if (anyLocationNeedsUmbrella) {
                    Log.d(TAG, "🌧️ 등록된 위치 중 우산이 필요한 곳이 있습니다");
                    // 우산 필요 메시지 업데이트
                    updateUmbrellaMessageForMultipleLocations(true);
                } else {
                    Log.d(TAG, "☀️ 등록된 모든 위치에서 우산이 필요하지 않습니다");
                    // 우산 불필요 메시지 업데이트
                    updateUmbrellaMessageForMultipleLocations(false);
                }
            }
        });
    }

    /**
     * 여러 위치 날씨 정보를 종합한 우산 메시지 업데이트
     */
    private void updateUmbrellaMessageForMultipleLocations(boolean needUmbrella) {
        String message;
        if (needUmbrella) {
            String[] rainMessages = {
                "등록된 장소 중 비가 올 곳이 있다냥! ☔️",
                "우산 챙기는 게 좋겠다냥! 🌧️",
                "비 소식이 있다냥! 우산 준비! ☂️",
                "오늘은 우산이 필요한 날이다냥! 🌦️"
            };
            int randomIndex = (int) (Math.random() * rainMessages.length);
            message = rainMessages[randomIndex];
        } else {
            String[] sunnyMessages = {
                "모든 장소가 맑다냥! ☀️",
                "우산 없이도 괜찮다냥! 😸",
                "완벽한 날씨다냥! 🌤️",
                "비 걱정 없는 하루다냥! ☀️"
            };
            int randomIndex = (int) (Math.random() * sunnyMessages.length);
            message = sunnyMessages[randomIndex];
        }

        // UI 업데이트는 메인 스레드에서
        requireActivity().runOnUiThread(() -> {
            if (binding != null) {
                binding.umbrellaMessage.setText(message);
                binding.umbrellaMessage.setVisibility(View.VISIBLE);
            }
        });
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

    /**
     * 로딩 애니메이션 표시
     */
    private void showLoadingAnimation() {
        if (binding != null) {
            binding.loadingAnimation.setVisibility(View.VISIBLE);
            Animation rotation = AnimationUtils.loadAnimation(getContext(), R.anim.loading_rotation);
            binding.loadingAnimation.startAnimation(rotation);

            // 고양이 이미지 살짝 투명하게
            binding.catImage.setAlpha(0.5f);
        }
    }

    /**
     * 로딩 애니메이션 숨기기
     */
    private void hideLoadingAnimation() {
        if (binding != null) {
            binding.loadingAnimation.setVisibility(View.GONE);
            binding.loadingAnimation.clearAnimation();

            // 고양이 이미지 원래대로
            binding.catImage.setAlpha(1.0f);
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
