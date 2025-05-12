package com.example.umbrellaalert.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.umbrellaalert.R;
import com.example.umbrellaalert.data.manager.WeatherManager;
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.databinding.ActivityHomeBinding;
import com.example.umbrellaalert.service.LocationService;
import com.example.umbrellaalert.service.WeatherUpdateService;
import com.example.umbrellaalert.ui.location.LocationActivity;
import com.example.umbrellaalert.ui.settings.SettingsActivity;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity implements LocationService.LocationCallback {

    private static final String TAG = "HomeActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private ActivityHomeBinding binding;
    private LocationService locationService;
    private WeatherManager weatherManager;
    private ExecutorService executorService;
    private Weather currentWeather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 매니저 및 서비스 초기화
        locationService = LocationService.getInstance(this);
        weatherManager = WeatherManager.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();

        // 날씨 업데이트 서비스 시작
        WeatherUpdateService.startService(this);

        // UI 초기 설정
        setupUI();

        // 위치 권한 확인
        checkLocationPermission();
    }

    private void setupUI() {
        // 로딩 표시
        showLoading(true);

        // 장소 설정 버튼
        binding.btnLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, LocationActivity.class);
                startActivity(intent);
            }
        });

        // 알림 설정 버튼
        binding.btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
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
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 위치 권한 승인됨
                startLocationUpdates();
            } else {
                // 권한 거부됨
                Toast.makeText(this, "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show();

                // 기본 위치(서울) 사용
                updateWeatherWithDefaultLocation();
            }
        }
    }

    private void startLocationUpdates() {
        locationService.startLocationUpdates(this);
    }

    // 위치 업데이트 콜백
    @Override
    public void onLocationUpdate(Location location) {
        updateWeatherWithLocation(location);
        updateLocationName(location);
    }

    // 위치 기반 날씨 업데이트
    private void updateWeatherWithLocation(Location location) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                final Weather weather = weatherManager.getCurrentWeather(
                        location.getLatitude(), location.getLongitude());

                // UI 스레드에서 UI 업데이트
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null) {
                            updateWeatherUI(weather);
                        }
                        showLoading(false);
                    }
                });
            }
        });
    }

    // 기본 위치(서울) 사용
    private void updateWeatherWithDefaultLocation() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // 서울 좌표 (위도, 경도)
                final Weather weather = weatherManager.getCurrentWeather(37.5665, 126.9780);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null) {
                            updateWeatherUI(weather);
                            binding.locationText.setText("서울");
                        }
                        showLoading(false);
                    }
                });
            }
        });
    }

    // 위치명 업데이트 (지오코딩)
    private void updateLocationName(Location location) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                String locationName = getLocationName(location);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.locationText.setText(locationName);
                    }
                });
            }
        });
    }

    // 위도/경도로부터 위치명 가져오기
    private String getLocationName(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // 시/도 및 구/군 정보 가져오기
                String city = address.getAdminArea(); // 시/도
                String district = address.getLocality(); // 구/군

                if (city != null && district != null) {
                    return city + " " + district;
                } else if (city != null) {
                    return city;
                } else {
                    return "알 수 없는 위치";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "알 수 없는 위치";
    }

    // UI 업데이트
    private void updateWeatherUI(Weather weather) {
        currentWeather = weather;

        // 배경색 설정 (날씨에 따라)
        if (weather.isNeedUmbrella()) {
            binding.mainContainer.setBackgroundColor(getResources().getColor(R.color.rainy_bg, null));
            binding.catImage.setImageResource(R.drawable.cat_rainy);
        } else {
            binding.mainContainer.setBackgroundColor(getResources().getColor(R.color.sunny_bg, null));
            binding.catImage.setImageResource(R.drawable.cat_sunny);
        }

        // 고양이 메시지
        binding.catMessage.setText(weatherManager.getCatMessage(weather));

        // 온도
        binding.temperatureText.setText(String.format(Locale.getDefault(), "%.1f°C", weather.getTemperature()));

        // 날씨 상태
        binding.weatherCondition.setText(getWeatherConditionText(weather.getWeatherCondition()));

        // 강수량
        if (weather.getPrecipitation() > 0) {
            binding.precipitationText.setText(String.format(Locale.getDefault(), "강수량: %.1fmm", weather.getPrecipitation()));
        } else {
            binding.precipitationText.setText("강수량: 없음");
        }

        // 습도
        binding.humidityText.setText(String.format(Locale.getDefault(), "습도: %d%%", weather.getHumidity()));

        // 우산 필요 여부
        if (weather.isNeedUmbrella()) {
            binding.umbrellaText.setText("오늘은 우산이 필요하다냥!");
        } else {
            binding.umbrellaText.setText("오늘은 우산이 필요 없을 것 같다냥~");
        }
    }

    // 날씨 상태 텍스트 변환
    private String getWeatherConditionText(String condition) {
        if (condition.equalsIgnoreCase("Clear")) {
            return "맑음";
        } else if (condition.equalsIgnoreCase("Clouds")) {
            return "구름";
        } else if (condition.equalsIgnoreCase("Rain")) {
            return "비";
        } else if (condition.equalsIgnoreCase("Drizzle")) {
            return "이슬비";
        } else if (condition.equalsIgnoreCase("Thunderstorm")) {
            return "뇌우";
        } else if (condition.equalsIgnoreCase("Snow")) {
            return "눈";
        } else if (condition.equalsIgnoreCase("Atmosphere")) {
            return "안개";
        } else {
            return condition;
        }
    }

    // 로딩 표시
    private void showLoading(boolean show) {
        binding.loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}