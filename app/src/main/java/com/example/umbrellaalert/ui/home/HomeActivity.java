package com.example.umbrellaalert.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.databinding.ActivityHomeBinding;
import com.example.umbrellaalert.service.WeatherUpdateService;
import com.example.umbrellaalert.ui.location.LocationActivity;
import com.example.umbrellaalert.ui.settings.SettingsActivity;

import java.util.Locale;

public class HomeActivity extends AppCompatActivity implements LocationViewModel.LocationCallback {

    private static final String TAG = "HomeActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private ActivityHomeBinding binding;
    private WeatherViewModel weatherViewModel;
    private LocationViewModel locationViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ViewModel 초기화
        weatherViewModel = new ViewModelProvider(this).get(WeatherViewModel.class);
        locationViewModel = new ViewModelProvider(this).get(LocationViewModel.class);

        // 위치 콜백 설정
        locationViewModel.setLocationCallback(this);

        // 날씨 업데이트 서비스 시작
        WeatherUpdateService.startService(this);

        // UI 초기 설정
        setupUI();

        // LiveData 관찰
        observeViewModel();

        // 위치 권한 확인
        checkLocationPermission();
    }

    private void setupUI() {
        // 장소 설정 버튼
        binding.btnLocation.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, LocationActivity.class);
            startActivity(intent);
        });

        // 알림 설정 버튼
        binding.btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void observeViewModel() {
        // 날씨 데이터 관찰
        weatherViewModel.getWeatherData().observe(this, this::updateWeatherDisplay);

        // 위치명 관찰
        weatherViewModel.getLocationName().observe(this, locationName ->
            binding.locationText.setText(locationName));

        // 로딩 상태 관찰
        weatherViewModel.getIsLoading().observe(this, isLoading ->
            binding.loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE));

        // 배경 리소스 관찰
        weatherViewModel.getBackgroundResource().observe(this, resource ->
            binding.mainContainer.setBackgroundResource(resource));

        // 고양이 이미지 관찰
        weatherViewModel.getCatImageResource().observe(this, resource ->
            binding.catImage.setImageResource(resource));

        // 고양이 메시지 관찰
        weatherViewModel.getCatMessage().observe(this, message ->
            binding.catMessage.setText(message));

        // 우산 메시지 관찰
        weatherViewModel.getUmbrellaMessage().observe(this, message ->
            binding.umbrellaText.setText(message));
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // 권한 요청
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // 권한이 이미 부여됨
            locationViewModel.setLocationPermissionGranted(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 위치 권한 승인됨
                locationViewModel.setLocationPermissionGranted(true);
            } else {
                // 권한 거부됨
                Toast.makeText(this, "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show();
                locationViewModel.setLocationPermissionGranted(false);

                // 기본 위치(서울) 사용
                weatherViewModel.updateWeatherWithDefaultLocation();
            }
        }
    }

    // 위치 업데이트 콜백 (LocationViewModel.LocationCallback 인터페이스 구현)
    @Override
    public void onLocationUpdate(Location location) {
        weatherViewModel.updateWeatherWithLocation(location);
    }

    // 날씨 정보 표시 업데이트
    private void updateWeatherDisplay(Weather weather) {
        if (weather == null) return;

        // 온도
        binding.temperatureText.setText(String.format(Locale.getDefault(), "%.1f°C", weather.getTemperature()));

        // 날씨 상태
        binding.weatherCondition.setText(weatherViewModel.getWeatherConditionText(weather.getWeatherCondition()));

        // 강수량
        if (weather.getPrecipitation() > 0) {
            binding.precipitationText.setText(String.format(Locale.getDefault(), "강수량: %.1fmm", weather.getPrecipitation()));
        } else {
            binding.precipitationText.setText("강수량: 없음");
        }

        // 습도
        binding.humidityText.setText(String.format(Locale.getDefault(), "습도: %d%%", weather.getHumidity()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}