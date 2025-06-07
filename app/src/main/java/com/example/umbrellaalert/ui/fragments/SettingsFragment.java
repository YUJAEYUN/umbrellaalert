package com.example.umbrellaalert.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.umbrellaalert.databinding.FragmentSettingsBinding;
import com.example.umbrellaalert.ui.location.LocationActivity;
import com.example.umbrellaalert.ui.settings.SettingsActivity;
import com.example.umbrellaalert.ui.settings.ThemeActivity;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupClickListeners();
    }

    private void setupClickListeners() {
        // 장소 설정 카드 클릭
        binding.locationSettingsCard.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LocationActivity.class);
            startActivity(intent);
        });

        // 버스 설정 카드 클릭
        binding.busSettingsCard.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.example.umbrellaalert.ui.bus.BusSettingsActivity.class);
            startActivity(intent);
        });

        // 테마 설정 카드 클릭
        binding.themeSettingsCard.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ThemeActivity.class);
            startActivity(intent);
        });

        // 알림 설정 카드 클릭
        binding.notificationSettingsCard.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SettingsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
