package com.example.umbrellaalert.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.umbrellaalert.databinding.FragmentBusBinding;
import com.example.umbrellaalert.ui.bus.BusViewModel;
import com.example.umbrellaalert.ui.bus.RegisteredBusAdapter;

public class BusFragment extends Fragment {

    private FragmentBusBinding binding;
    private BusViewModel busViewModel;
    private RegisteredBusAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBusBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ViewModel 초기화
        busViewModel = new ViewModelProvider(this).get(BusViewModel.class);

        // RecyclerView 설정
        setupRecyclerView();

        // 버스 설정 버튼 클릭 리스너
        binding.btnBusSettings.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.example.umbrellaalert.ui.bus.BusSettingsActivity.class);
            startActivity(intent);
        });

        // 빈 상태에서 버스 추가 버튼 클릭 리스너
        binding.btnAddBusEmpty.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.example.umbrellaalert.ui.bus.BusSettingsActivity.class);
            startActivity(intent);
        });

        // 데이터 관찰
        observeData();

        // 초기 데이터 로드
        busViewModel.loadRegisteredBuses();
    }

    private void setupRecyclerView() {
        adapter = new RegisteredBusAdapter();
        binding.recyclerViewBuses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewBuses.setAdapter(adapter);

        // 어댑터 클릭 리스너 설정
        adapter.setOnBusClickListener(bus -> {
            // 버스 클릭 시 도착 정보 새로고침
            busViewModel.refreshArrivalInfo(bus);
        });

        adapter.setOnBusDeleteListener(bus -> {
            // 버스 삭제 확인 다이얼로그 표시
            // TODO: 삭제 확인 다이얼로그 구현
            busViewModel.deleteBus(bus.getId());
        });
    }

    private void observeData() {
        busViewModel.getRegisteredBuses().observe(getViewLifecycleOwner(), buses -> {
            if (buses != null && !buses.isEmpty()) {
                binding.emptyStateLayout.setVisibility(View.GONE);
                binding.recyclerViewBuses.setVisibility(View.VISIBLE);
                adapter.updateBuses(buses);
            } else {
                binding.emptyStateLayout.setVisibility(View.VISIBLE);
                binding.recyclerViewBuses.setVisibility(View.GONE);
            }
        });

        busViewModel.getArrivalInfoMap().observe(getViewLifecycleOwner(), arrivalInfoMap -> {
            if (arrivalInfoMap != null) {
                adapter.updateArrivalInfo(arrivalInfoMap);
            }
        });

        busViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        busViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                // TODO: 에러 메시지 표시 (Toast 또는 Snackbar)
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // 화면이 다시 보일 때 데이터 새로고침
        busViewModel.loadRegisteredBuses();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
