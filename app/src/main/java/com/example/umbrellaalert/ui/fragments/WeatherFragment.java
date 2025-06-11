package com.example.umbrellaalert.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.umbrellaalert.R;
import com.example.umbrellaalert.data.manager.WeatherManager;
import com.example.umbrellaalert.data.model.HourlyForecast;
import com.example.umbrellaalert.databinding.FragmentWeatherBinding;
import com.example.umbrellaalert.service.CatWeatherAnalystService;
import com.example.umbrellaalert.service.MockWeatherForecastService;
import com.example.umbrellaalert.ui.home.WeatherViewModel;
import com.example.umbrellaalert.ui.adapter.HourlyForecastAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 날씨 탭 프래그먼트 - 12시간 예보 및 고양이 분석 (OpenWeather API 사용)
 */
@AndroidEntryPoint
public class WeatherFragment extends Fragment {

    private static final String TAG = "WeatherFragment";

    private FragmentWeatherBinding binding;
    private WeatherViewModel weatherViewModel;
    private HourlyForecastAdapter forecastAdapter;

    @Inject
    WeatherManager weatherManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWeatherBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Activity의 공유 ViewModel 사용
        weatherViewModel = ((com.example.umbrellaalert.ui.main.MainActivity) requireActivity()).getSharedWeatherViewModel();

        // RecyclerView 설정
        setupRecyclerView();

        // UI 관찰자 설정
        setupObservers();

        // 8시간 예보 목업 데이터 생성 및 고양이 분석 (3시간 단위)
        load12HourForecastAndAnalysis();

        Log.d(TAG, "8시간 예보 (3시간 단위) 및 고양이 분석 시작");
    }

    private void setupRecyclerView() {
        forecastAdapter = new HourlyForecastAdapter();
        binding.forecastRecyclerView.setLayoutManager(
            new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        binding.forecastRecyclerView.setAdapter(forecastAdapter);
    }

    private void setupObservers() {
        // 현재 날씨 데이터 관찰 (고양이 이미지 업데이트만)
        weatherViewModel.getWeatherData().observe(getViewLifecycleOwner(), weather -> {
            if (weather != null) {
                updateCatAnalystImage(weather);
                // 실제 위치 기반으로 OpenWeather API 예보 데이터 다시 로드
                loadRealForecastDataFromWeather(weather);
            }
        });
    }



    /**
     * 12시간 예보 데이터 로드 및 고양이 분석 - OpenWeather API 사용
     */
    private void load12HourForecastAndAnalysis() {
        // 로딩 애니메이션 시작
        showWeatherLoadingAnimation();

        // 현재 날씨 데이터에서 위치 정보 추출
        com.example.umbrellaalert.data.model.Weather currentWeather = weatherViewModel.getWeatherData().getValue();

        if (currentWeather != null && currentWeather.getLocation() != null) {
            // 현재 날씨 데이터에서 위치 정보 파싱
            String[] locationParts = currentWeather.getLocation().split(",");
            if (locationParts.length >= 2) {
                try {
                    double latitude = Double.parseDouble(locationParts[0]);
                    double longitude = Double.parseDouble(locationParts[1]);
                    loadRealForecastData(latitude, longitude);
                } catch (NumberFormatException e) {
                    // 파싱 실패 시 기본 위치 사용
                    loadRealForecastData(37.5665, 126.9780); // 서울 기본 좌표
                }
            } else {
                loadRealForecastData(37.5665, 126.9780); // 서울 기본 좌표
            }
        } else {
            // 위치 정보가 없으면 기본 위치로 예보 데이터 가져오기
            loadRealForecastData(37.5665, 126.9780); // 서울 기본 좌표
        }
    }

    /**
     * 실제 OpenWeather API로 예보 데이터 가져오기
     */
    private void loadRealForecastData(double latitude, double longitude) {
        weatherManager.get12HourForecast(latitude, longitude, new WeatherManager.ForecastCallback() {
            @Override
            public void onSuccess(List<HourlyForecast> forecasts) {
                requireActivity().runOnUiThread(() -> {
                    // RecyclerView에 데이터 설정
                    updateForecastDisplay(forecasts);

                    // 토스 스타일 고양이 분석 카드 업데이트
                    updateCatAnalysisCard(forecasts);

                    // 업데이트 시간 표시 (실제 API 데이터임을 표시)
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.KOREA);
                    binding.updateTime.setText(sdf.format(new Date()) + " 실시간");

                    // 로딩 애니메이션 종료
                    hideWeatherLoadingAnimation();

                    Log.d(TAG, "✅ OpenWeather API 실제 12시간 예보 " + forecasts.size() + "개 로드 및 분석 완료");

                    // 각 예보 데이터 로그 출력 (실제 데이터 확인용)
                    for (int i = 0; i < Math.min(forecasts.size(), 4); i++) {
                        HourlyForecast forecast = forecasts.get(i);
                        Log.d(TAG, "  실제 예보 " + (i+1) + ": " + forecast.getTemperature() + "°C, " +
                              forecast.getWeatherCondition() + ", 강수확률: " + forecast.getPrecipitationProbability() + "%");
                    }
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    // 에러 발생 시 기본 데이터 표시
                    Log.e(TAG, "❌ OpenWeather API 예보 데이터 로드 실패: " + error);
                    Log.w(TAG, "⚠️ 임시로 목업 데이터 사용 (실제 서비스에서는 API 문제 해결 필요)");

                    // 현재 날씨 데이터가 있으면 그에 맞춰 생성, 없으면 기본값
                    com.example.umbrellaalert.data.model.Weather currentWeather = weatherViewModel.getWeatherData().getValue();
                    List<HourlyForecast> forecasts;

                    if (currentWeather != null) {
                        forecasts = MockWeatherForecastService.generateForecastBasedOnWeather(currentWeather);
                        Log.d(TAG, "현재 날씨 기반 목업 예보 데이터 생성: " + currentWeather.getWeatherCondition());
                    } else {
                        forecasts = MockWeatherForecastService.generate12HourForecast();
                        Log.d(TAG, "기본 목업 예보 데이터 생성");
                    }

                    // RecyclerView에 데이터 설정
                    updateForecastDisplay(forecasts);

                    // 토스 스타일 고양이 분석 카드 업데이트
                    updateCatAnalysisCard(forecasts);

                    // 업데이트 시간 표시 (목업 데이터임을 표시)
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.KOREA);
                    binding.updateTime.setText(sdf.format(new Date()) + " (목업)");

                    // 로딩 애니메이션 종료
                    hideWeatherLoadingAnimation();
                });
            }
        });
    }

    /**
     * 현재 날씨 데이터에서 위치 정보를 추출하여 실제 API 예보 데이터 로드
     */
    private void loadRealForecastDataFromWeather(com.example.umbrellaalert.data.model.Weather weather) {
        if (weather != null && weather.getLocation() != null) {
            // 현재 날씨 데이터에서 위치 정보 파싱
            String[] locationParts = weather.getLocation().split(",");
            if (locationParts.length >= 2) {
                try {
                    double latitude = Double.parseDouble(locationParts[0]);
                    double longitude = Double.parseDouble(locationParts[1]);
                    Log.d(TAG, "홈탭 날씨 업데이트로 인한 실제 API 예보 데이터 재로드: " + latitude + ", " + longitude);
                    loadRealForecastData(latitude, longitude);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "위치 정보 파싱 실패, 기본 위치 사용");
                    loadRealForecastData(37.5665, 126.9780); // 서울 기본 좌표
                }
            } else {
                loadRealForecastData(37.5665, 126.9780); // 서울 기본 좌표
            }
        } else {
            Log.w(TAG, "날씨 데이터에 위치 정보 없음, 기본 위치 사용");
            loadRealForecastData(37.5665, 126.9780); // 서울 기본 좌표
        }
    }

    /**
     * 우산 상태 카드 업데이트 (간소화)
     */
    private void updateCatAnalysisCard(List<HourlyForecast> forecasts) {
        if (forecasts == null || forecasts.isEmpty()) return;

        // 온도 범위 계산
        float minTemp = Float.MAX_VALUE;
        float maxTemp = Float.MIN_VALUE;
        boolean hasRain = false;
        int maxRainProb = 0;

        for (HourlyForecast forecast : forecasts) {
            minTemp = Math.min(minTemp, forecast.getTemperature());
            maxTemp = Math.max(maxTemp, forecast.getTemperature());
            maxRainProb = Math.max(maxRainProb, forecast.getPrecipitationProbability());
            if (forecast.getPrecipitationProbability() > 30) {
                hasRain = true;
            }
        }
        float tempRange = maxTemp - minTemp;

        // 우산 상태 - 강수 확률에 따라 결정 (색상과 함께)
        if (maxRainProb > 70) {
            binding.umbrellaStatus.setText("필수");
            binding.umbrellaStatus.setTextColor(getResources().getColor(R.color.umbrella_required, null));
            binding.umbrellaIcon.setText("☔");
        } else if (maxRainProb > 30) {
            binding.umbrellaStatus.setText("권장");
            binding.umbrellaStatus.setTextColor(getResources().getColor(R.color.umbrella_recommended, null));
            binding.umbrellaIcon.setText("🌦️");
        } else {
            binding.umbrellaStatus.setText("불필요");
            binding.umbrellaStatus.setTextColor(getResources().getColor(R.color.umbrella_not_needed, null));
            binding.umbrellaIcon.setText("☀️");
        }

        // 온도차
        binding.temperatureRange.setText(String.format("%.0f°C", tempRange));

        // 추천 활동 - 날씨와 온도에 따라 결정
        if (hasRain) {
            binding.recommendation.setText("실내활동");
        } else if (maxTemp >= 30) {
            binding.recommendation.setText("선크림");
        } else if (minTemp <= 5) {
            binding.recommendation.setText("따뜻한 옷");
        } else {
            binding.recommendation.setText("나들이");
        }

        // 날씨 상태 배지 - 3가지 날씨에 따라 색상과 텍스트 결정
        if (maxRainProb > 50) {
            binding.weatherStatusBadge.setText("비");
            binding.weatherStatusBadge.setBackgroundResource(R.drawable.status_badge_rainy);
        } else if (maxRainProb > 30) {
            binding.weatherStatusBadge.setText("흐림");
            binding.weatherStatusBadge.setBackgroundResource(R.drawable.status_badge_cloudy);
        } else {
            binding.weatherStatusBadge.setText("맑음");
            binding.weatherStatusBadge.setBackgroundResource(R.drawable.status_badge_sunny);
        }
    }

    /**
     * 예보 데이터 표시 업데이트
     */
    private void updateForecastDisplay(List<HourlyForecast> forecasts) {
        if (forecasts != null && !forecasts.isEmpty()) {
            Log.d(TAG, "예보 데이터 " + forecasts.size() + "개 표시");
            forecastAdapter.setForecasts(forecasts);
        } else {
            Log.w(TAG, "예보 데이터가 없음");
        }
    }

    /**
     * 날씨에 따른 고양이 분석관 이미지 업데이트 (3가지 날씨)
     */
    private void updateCatAnalystImage(com.example.umbrellaalert.data.model.Weather weather) {
        int catImageResource;
        String condition = weather.getWeatherCondition();

        if (condition != null && condition.contains("비")) {
            catImageResource = R.drawable.cat_rainy;
        } else if (condition != null && condition.contains("흐림")) {
            catImageResource = R.drawable.cat_cloudy;
        } else {
            // 맑음 (기본값)
            catImageResource = R.drawable.cat_sunny;
        }

        binding.catAnalystImage.setImageResource(catImageResource);
    }

    /**
     * 날씨 로딩 애니메이션 표시
     */
    private void showWeatherLoadingAnimation() {
        if (binding != null) {
            binding.weatherLoadingAnimation.setVisibility(View.VISIBLE);
            Animation rotation = AnimationUtils.loadAnimation(getContext(), R.anim.loading_rotation);
            binding.weatherLoadingAnimation.startAnimation(rotation);

            // 고양이 분석관 이미지 살짝 투명하게
            binding.catAnalystImage.setAlpha(0.5f);
        }
    }

    /**
     * 날씨 로딩 애니메이션 숨기기
     */
    private void hideWeatherLoadingAnimation() {
        if (binding != null) {
            binding.weatherLoadingAnimation.setVisibility(View.GONE);
            binding.weatherLoadingAnimation.clearAnimation();

            // 고양이 분석관 이미지 원래대로
            binding.catAnalystImage.setAlpha(1.0f);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
