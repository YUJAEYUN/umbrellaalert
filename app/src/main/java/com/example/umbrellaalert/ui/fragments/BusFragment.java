package com.example.umbrellaalert.ui.fragments;

import android.app.Activity;
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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import com.example.umbrellaalert.databinding.FragmentBusBinding;
import com.example.umbrellaalert.ui.bus.BusViewModel;
import com.example.umbrellaalert.ui.bus.RegisteredBusAdapter;

public class BusFragment extends Fragment {

    private static final int REQUEST_BUS_SETTINGS = 1001;

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

        // 새로고침 버튼 클릭 리스너
        binding.btnRefresh.setOnClickListener(v -> {
            busViewModel.refreshAllArrivalInfo();
            // 새로고침 애니메이션
            v.animate().rotation(360f).setDuration(500).start();
        });

        // 버스 설정 버튼 클릭 리스너
        binding.btnBusSettings.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.example.umbrellaalert.ui.bus.BusSettingsActivity.class);
            startActivityForResult(intent, REQUEST_BUS_SETTINGS);
        });

        // 빈 상태에서 버스 추가 버튼 클릭 리스너
        binding.btnAddBusEmpty.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.example.umbrellaalert.ui.bus.BusSettingsActivity.class);
            startActivityForResult(intent, REQUEST_BUS_SETTINGS);
        });

        // 데이터 관찰
        observeData();

        // 초기 데이터 로드
        busViewModel.loadRegisteredBuses();
    }

    private void setupRecyclerView() {
        adapter = new RegisteredBusAdapter(requireContext());
        binding.recyclerViewBuses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewBuses.setAdapter(adapter);

        // 어댑터 클릭 리스너 설정
        adapter.setOnBusClickListener(bus -> {
            // 버스 클릭 시 도착 정보 새로고침
            busViewModel.refreshArrivalInfo(bus);
        });

        adapter.setOnBusDeleteListener(bus -> {
            // 버스 삭제 확인 다이얼로그 표시
            showDeleteConfirmDialog(bus);
        });

        // 스와이프 삭제 기능 추가
        setupSwipeToDelete();
    }

    /**
     * 스와이프로 삭제하는 기능 설정
     */
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // 드래그 이동은 사용하지 않음
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    // 삭제할 버스 정보 가져오기
                    var buses = busViewModel.getRegisteredBuses().getValue();
                    if (buses != null && position < buses.size()) {
                        var busToDelete = buses.get(position);

                        // 스와이프 삭제 시에도 확인 다이얼로그 표시
                        showDeleteConfirmDialog(busToDelete);
                    }
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewBuses);
    }

    /**
     * 버스 삭제 확인 다이얼로그 표시
     */
    private void showDeleteConfirmDialog(com.example.umbrellaalert.data.model.RegisteredBus bus) {
        new AlertDialog.Builder(requireContext())
                .setTitle("버스 삭제")
                .setMessage(bus.getRouteNo() + "번 버스를 삭제하시겠습니까?\n\n정류장: " + bus.getNodeName())
                .setPositiveButton("삭제", (dialog, which) -> {
                    // 삭제 실행
                    busViewModel.deleteBus(bus.getId());

                    // 사용자에게 피드백
                    if (getView() != null) {
                        com.google.android.material.snackbar.Snackbar.make(
                            getView(),
                            bus.getRouteNo() + "번 버스가 삭제되었습니다",
                            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                        ).show();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
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

        // 삭제된 버스 관찰 (애니메이션용)
        busViewModel.getDeletedBus().observe(getViewLifecycleOwner(), deletedBus -> {
            if (deletedBus != null) {
                adapter.removeBus(deletedBus);
            }
        });

        busViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                // TODO: 에러 메시지 표시 (Toast 또는 Snackbar)
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_BUS_SETTINGS && resultCode == Activity.RESULT_OK) {
            // 버스 등록/삭제 후 즉시 새로고침
            busViewModel.loadRegisteredBuses();
        }
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
