package com.example.umbrellaalert.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.umbrellaalert.data.model.HourlyForecast;
import com.example.umbrellaalert.databinding.FragmentWeatherBinding;
import com.example.umbrellaalert.ui.home.WeatherViewModel;
import com.example.umbrellaalert.ui.adapter.HourlyForecastAdapter;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WeatherFragment extends Fragment {

    private static final String TAG = "WeatherFragment";

    private FragmentWeatherBinding binding;
    private WeatherViewModel weatherViewModel;
    private HourlyForecastAdapter forecastAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWeatherBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Activity의 공유 ViewModel 사용 (홈에서 이미 로드된 데이터 재사용)
        weatherViewModel = ((com.example.umbrellaalert.ui.main.MainActivity) requireActivity()).getSharedWeatherViewModel();

        // RecyclerView 설정
        setupRecyclerView();

        // UI 관찰자 설정 (이미 로드된 데이터 표시)
        setupObservers();

        // 홈에서 이미 데이터를 로드했으므로 추가 API 호출 불필요
        Log.d(TAG, "홈에서 로드된 6시간 예보 데이터 재사용");
    }

    private void setupRecyclerView() {
        forecastAdapter = new HourlyForecastAdapter();
        binding.forecastRecyclerView.setLayoutManager(
            new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
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



    private void updateForecastDisplay(List<HourlyForecast> forecasts) {
        if (forecasts != null && !forecasts.isEmpty()) {
            Log.d(TAG, "6시간 예보 데이터 " + forecasts.size() + "개 수신");
            forecastAdapter.setForecasts(forecasts);
        } else {
            Log.w(TAG, "예보 데이터가 없음");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
