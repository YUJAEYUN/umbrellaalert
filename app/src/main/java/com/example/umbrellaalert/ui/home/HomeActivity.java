package com.example.umbrellaalert.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.umbrellaalert.R;

import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.data.model.HourlyForecast;
import com.example.umbrellaalert.databinding.ActivityHomeBinding;
import com.example.umbrellaalert.ui.adapter.HourlyForecastAdapter;

import com.example.umbrellaalert.service.WeatherUpdateService;
import com.example.umbrellaalert.ui.location.LocationActivity;
import com.example.umbrellaalert.ui.settings.SettingsActivity;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeActivity extends AppCompatActivity implements LocationViewModel.LocationCallback {

    private static final String TAG = "HomeActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private ActivityHomeBinding binding;
    private WeatherViewModel weatherViewModel;
    private LocationViewModel locationViewModel;
    private HourlyForecastAdapter hourlyForecastAdapter;

    // 스와이프 관련 변수들
    private GestureDetector gestureDetector;
    private int currentWeatherPage = 0;
    private static final int WEATHER_PAGE_COUNT = 3;


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

        // 6시간 예보 어댑터 초기화
        hourlyForecastAdapter = new HourlyForecastAdapter();
        binding.forecastRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.forecastRecyclerView.setAdapter(hourlyForecastAdapter);



        // UI 초기 설정
        setupUI();

        // LiveData 관찰
        observeViewModel();

        // 위치 권한 확인
        checkLocationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 앱이 포그라운드로 돌아올 때 위치 업데이트 다시 시작
        if (locationViewModel.getLocationPermissionGranted().getValue() == Boolean.TRUE) {
            locationViewModel.startLocationUpdates();
        }
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

        // 6시간 예보 카드는 항상 표시
        binding.forecastCard.setVisibility(View.VISIBLE);

        // 날씨 카드 스와이프 제스처 설정
        setupWeatherCardGesture();
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
        weatherViewModel.getCatImageResource().observe(this, resource -> {
            try {
                binding.catImage.setImageResource(resource);
            } catch (Exception e) {
                Log.e("HomeActivity", "Failed to load cat image resource: " + resource, e);
                // 기본 이미지로 폴백
                binding.catImage.setImageResource(R.drawable.cat_sunny);
            }
        });

        // 고양이 메시지 관찰 (애니메이션 효과 추가)
        weatherViewModel.getCatMessage().observe(this, message -> {
            binding.catMessage.setText(message);
            // 메시지 변경 시 페이드 인 애니메이션
            android.view.animation.Animation fadeIn = android.view.animation.AnimationUtils
                .loadAnimation(this, R.anim.cat_message_fade_in);
            binding.messageCard.startAnimation(fadeIn);

            // 고양이 이미지에 바운스 효과
            android.view.animation.Animation bounce = android.view.animation.AnimationUtils
                .loadAnimation(this, R.anim.cat_bounce);
            binding.catImage.startAnimation(bounce);
        });

        // 우산 메시지 관찰 (애니메이션 효과 추가)
        weatherViewModel.getUmbrellaMessage().observe(this, message -> {
            binding.umbrellaText.setText(message);

            // 우산이 필요한 경우 강조 애니메이션
            if (message.contains("우산을 꼭") || message.contains("폭우") || message.contains("비가")) {
                android.view.animation.Animation shake = android.view.animation.AnimationUtils
                    .loadAnimation(this, R.anim.umbrella_shake);
                binding.umbrellaIcon.startAnimation(shake);

                // 우산 카드 강조 효과
                binding.umbrellaCard.setCardBackgroundColor(
                    getResources().getColor(R.color.alert_color_light, getTheme()));

                // 3초 후 원래 색상으로 복원
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    binding.umbrellaCard.setCardBackgroundColor(
                        getResources().getColor(R.color.ios_card_background, getTheme()));
                }, 3000);
            }
        });

        // 온도 메시지 관찰 (UI에 표시)
        weatherViewModel.getTemperatureMessage().observe(this, message -> {
            binding.temperatureMessage.setText(message);

            // 메시지 내용에 따라 이모지 변경
            String emoji = "🌡️"; // 기본 온도계
            if (message.contains("🥵") || message.contains("덥다")) {
                emoji = "🥵";
            } else if (message.contains("🥶") || message.contains("춥다")) {
                emoji = "🥶";
            } else if (message.contains("😊") || message.contains("따뜻")) {
                emoji = "😊";
            } else if (message.contains("⏰") || message.contains("러시아워")) {
                emoji = "⏰";
            } else if (message.contains("🎉") || message.contains("주말")) {
                emoji = "🎉";
            } else if (message.contains("🌙") || message.contains("늦은")) {
                emoji = "🌙";
            }

            binding.temperatureEmoji.setText(emoji);

            // 온도 카드에 페이드 인 애니메이션
            android.view.animation.Animation fadeIn = android.view.animation.AnimationUtils
                .loadAnimation(this, R.anim.cat_message_fade_in);
            binding.temperatureCard.startAnimation(fadeIn);
        });

        // 6시간 예보 데이터 관찰
        weatherViewModel.getHourlyForecastData().observe(this, forecasts -> {
            if (forecasts != null && !forecasts.isEmpty()) {
                Log.d("HomeActivity", "🏠 HomeActivity에서 받은 예보 데이터 " + forecasts.size() + "개:");
                for (int i = 0; i < Math.min(3, forecasts.size()); i++) {
                    HourlyForecast forecast = forecasts.get(i);
                    Log.d("HomeActivity", "  " + i + "시간 후: " + forecast.getTemperature() + "°C, 시간: " + forecast.getForecastTime());
                }
                hourlyForecastAdapter.setForecasts(forecasts);
                binding.forecastCard.setVisibility(View.VISIBLE);
            } else {
                Log.w("HomeActivity", "⚠️ 받은 예보 데이터가 없음");
                binding.forecastCard.setVisibility(View.GONE);
            }
        });

        // 예보 업데이트 시간 관찰
        weatherViewModel.getForecastUpdateTime().observe(this, updateTime -> {
            if (updateTime != null) {
                binding.forecastUpdateTime.setText(updateTime);
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
                Toast.makeText(this, "위치 권한이 필요합니다. 정확한 날씨 정보를 위해 위치 권한을 허용해주세요.", Toast.LENGTH_LONG).show();
                locationViewModel.setLocationPermissionGranted(false);

                // 권한이 없으면 기본 날씨 정보만 표시
                weatherViewModel.updateWeatherWithDefaultLocation();
            }
        }
    }

    // 위치 업데이트 콜백 (LocationViewModel.LocationCallback 인터페이스 구현)
    @Override
    public void onLocationUpdate(Location location) {
        weatherViewModel.updateWeatherWithLocation(location);
    }

    // 날씨 카드 스와이프 제스처 설정
    private void setupWeatherCardGesture() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onDown(MotionEvent e) {
                // 터치 시작 시 부모의 터치 이벤트 가로채기 방지
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                // 수평 스와이프가 수직 스와이프보다 더 클 때만 처리
                if (Math.abs(diffX) > Math.abs(diffY) * 1.5) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // 오른쪽 스와이프 (이전 페이지)
                            currentWeatherPage = (currentWeatherPage - 1 + WEATHER_PAGE_COUNT) % WEATHER_PAGE_COUNT;
                        } else {
                            // 왼쪽 스와이프 (다음 페이지)
                            currentWeatherPage = (currentWeatherPage + 1) % WEATHER_PAGE_COUNT;
                        }
                        updateWeatherPageDisplay();
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                // 수직 스크롤이 더 클 때는 부모에게 이벤트 전달
                if (Math.abs(distanceY) > Math.abs(distanceX)) {
                    return false;
                }
                return true;
            }
        });

        binding.weatherCard.setOnTouchListener((v, event) -> {
            boolean gestureHandled = gestureDetector.onTouchEvent(event);
            // 제스처가 처리되지 않았거나 수직 스크롤인 경우 부모에게 이벤트 전달
            if (!gestureHandled) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return gestureHandled;
        });
    }

    // 날씨 페이지 표시 업데이트
    private void updateWeatherPageDisplay() {
        Weather weather = weatherViewModel.getWeatherData().getValue();
        if (weather == null) return;

        switch (currentWeatherPage) {
            case 0: // 기본 날씨 정보
                binding.weatherTitle.setText("현재 날씨");
                updateBasicWeatherDisplay(weather);
                break;
            case 1: // 상세 정보
                binding.weatherTitle.setText("상세 정보");
                updateDetailedWeatherDisplay(weather);
                break;
            case 2: // 추가 정보
                binding.weatherTitle.setText("추가 정보");
                updateAdditionalWeatherDisplay(weather);
                break;
        }
    }

    // 기본 날씨 정보 표시
    private void updateBasicWeatherDisplay(Weather weather) {
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

    // 상세 날씨 정보 표시
    private void updateDetailedWeatherDisplay(Weather weather) {
        // 체감온도 (간단한 계산)
        double feelsLike = weather.getTemperature() + (weather.getHumidity() > 70 ? 2 : -1);
        binding.temperatureText.setText(String.format(Locale.getDefault(), "%.1f°C", feelsLike));

        // 체감온도 설명
        binding.weatherCondition.setText("체감온도");

        // 바람 정보 (임시 데이터)
        binding.precipitationText.setText("바람: 2.5m/s");

        // 기압 정보 (임시 데이터)
        binding.humidityText.setText("기압: 1013hPa");
    }

    // 추가 날씨 정보 표시
    private void updateAdditionalWeatherDisplay(Weather weather) {
        // 일출 시간 (임시 데이터)
        binding.temperatureText.setText("06:30");

        // 일출 설명
        binding.weatherCondition.setText("일출 시간");

        // 일몰 시간 (임시 데이터)
        binding.precipitationText.setText("일몰: 18:45");

        // 가시거리 (임시 데이터)
        binding.humidityText.setText("가시거리: 10km");
    }

    // 날씨 정보 표시 업데이트 (기본 호출)
    private void updateWeatherDisplay(Weather weather) {
        if (weather == null) {
            Log.w("HomeActivity", "⚠️ 받은 날씨 데이터가 null");
            return;
        }
        Log.d("HomeActivity", "🏠 HomeActivity에서 받은 날씨 데이터: " + weather.getTemperature() + "°C, 상태: " + weather.getWeatherCondition());
        updateWeatherPageDisplay();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 앱이 백그라운드로 갈 때 위치 업데이트 중지 (배터리 절약)
        locationViewModel.stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 앱이 종료될 때 위치 업데이트 중지
        locationViewModel.stopLocationUpdates();
    }
}