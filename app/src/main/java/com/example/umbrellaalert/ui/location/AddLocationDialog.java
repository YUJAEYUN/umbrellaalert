package com.example.umbrellaalert.ui.location;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.umbrellaalert.R;
import com.example.umbrellaalert.data.model.Location;
import com.example.umbrellaalert.data.model.SearchLocation;
import com.example.umbrellaalert.databinding.DialogAddLocationBinding;
import com.example.umbrellaalert.service.LocationSearchService;
import com.example.umbrellaalert.ui.adapter.LocationSearchAdapter;

import java.util.List;

public class AddLocationDialog extends DialogFragment implements LocationSearchAdapter.OnLocationSelectedListener {

    private static final int MAP_PICKER_REQUEST_CODE = 1001;

    private DialogAddLocationBinding binding;
    private LocationAddedListener listener;
    private LocationSearchAdapter searchAdapter;
    private SearchLocation selectedSearchLocation;

    // 위치 추가 완료 리스너
    public interface LocationAddedListener {
        void onLocationAdded(Location location);
    }

    public void setLocationAddedListener(LocationAddedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogAddLocationBinding.inflate(LayoutInflater.from(getContext()));

        // 검색 기능 설정
        setupSearchFunctionality();

        // 지도에서 선택 버튼 설정
        binding.btnSelectFromMap.setOnClickListener(v -> openMapPicker());

        // 다이얼로그 빌더
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("장소 추가")
                .setView(binding.getRoot())
                .setPositiveButton("추가", null) // 나중에 설정
                .setNegativeButton("취소", (dialog, which) -> dismiss());

        AlertDialog dialog = builder.create();

        // 다이얼로그 표시 후 긍정 버튼 설정 (자동 닫힘 방지)
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (validateInput()) {
                    addLocation();
                    dismiss();
                }
            });
        });

        return dialog;
    }

    /**
     * 검색 기능 설정
     */
    private void setupSearchFunctionality() {
        // 검색 결과 어댑터 설정
        searchAdapter = new LocationSearchAdapter(this);
        binding.recyclerSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerSearchResults.setAdapter(searchAdapter);

        // 검색 입력 리스너
        binding.editSearchLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    performSearch(query);
                } else {
                    hideSearchResults();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * 장소 검색 수행 (네이버 API 사용, 비동기)
     */
    private void performSearch(String query) {
        // 검색 중 표시
        binding.recyclerSearchResults.setVisibility(View.VISIBLE);

        // 로딩 상태 표시 (빈 어댑터로 설정)
        searchAdapter.setSearchResults(null);
        searchAdapter.setLoading(true);

        // 비동기 검색 수행
        new Thread(() -> {
            try {
                List<SearchLocation> results = LocationSearchService.searchByName(query);

                // UI 업데이트는 메인 스레드에서
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        searchAdapter.setLoading(false);
                        if (results.isEmpty()) {
                            searchAdapter.setNoResults(true);
                            binding.recyclerSearchResults.setVisibility(View.VISIBLE);
                        } else {
                            searchAdapter.setSearchResults(results);
                            searchAdapter.setNoResults(false);
                            binding.recyclerSearchResults.setVisibility(View.VISIBLE);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("AddLocationDialog", "검색 중 오류 발생", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        searchAdapter.setLoading(false);
                        searchAdapter.setNoResults(true);
                        binding.recyclerSearchResults.setVisibility(View.VISIBLE);
                    });
                }
            }
        }).start();
    }

    /**
     * 검색 결과 숨기기
     */
    private void hideSearchResults() {
        binding.recyclerSearchResults.setVisibility(View.GONE);
        searchAdapter.setSearchResults(null);
        searchAdapter.setLoading(false);
        searchAdapter.setNoResults(false);
    }

    /**
     * 검색 결과에서 장소 선택 시 호출
     */
    @Override
    public void onLocationSelected(SearchLocation location) {
        selectedSearchLocation = location;

        // 검색 입력창에 선택된 장소 이름 표시
        binding.editSearchLocation.setText(location.getName());

        // 선택된 위치 정보 표시
        binding.textSelectedLocationName.setText(location.getName() + "\n" + location.getAddress());
        binding.selectedLocationContainer.setVisibility(View.VISIBLE);
        binding.nameInputLayout.setVisibility(View.VISIBLE);
        binding.editName.setText(location.getName());

        // 좌표 정보 저장
        binding.editLatitude.setText(String.valueOf(location.getLatitude()));
        binding.editLongitude.setText(String.valueOf(location.getLongitude()));

        // 검색 결과 숨기기
        hideSearchResults();
    }

    private void openMapPicker() {
        Intent intent = new Intent(getContext(), MapLocationPickerActivity.class);
        startActivityForResult(intent, MAP_PICKER_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MAP_PICKER_REQUEST_CODE && resultCode == getActivity().RESULT_OK && data != null) {
            String locationName = data.getStringExtra(MapLocationPickerActivity.EXTRA_LOCATION_NAME);
            double latitude = data.getDoubleExtra(MapLocationPickerActivity.EXTRA_LATITUDE, 0);
            double longitude = data.getDoubleExtra(MapLocationPickerActivity.EXTRA_LONGITUDE, 0);

            // UI 업데이트
            binding.textSelectedLocationName.setText(locationName);
            binding.selectedLocationContainer.setVisibility(android.view.View.VISIBLE);
            binding.nameInputLayout.setVisibility(android.view.View.VISIBLE);
            binding.editName.setText(locationName);
            binding.editLatitude.setText(String.valueOf(latitude));
            binding.editLongitude.setText(String.valueOf(longitude));
        }
    }

    private boolean validateInput() {
        String latitudeStr = binding.editLatitude.getText().toString().trim();
        String longitudeStr = binding.editLongitude.getText().toString().trim();

        // 지도에서 위치를 선택했는지 확인
        if (TextUtils.isEmpty(latitudeStr) || TextUtils.isEmpty(longitudeStr)) {
            Toast.makeText(getContext(), "지도에서 위치를 선택해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            double latitude = Double.parseDouble(latitudeStr);
            double longitude = Double.parseDouble(longitudeStr);

            // 좌표가 0,0이 아닌지 확인 (실제 선택된 위치인지)
            if (latitude == 0.0 && longitude == 0.0) {
                Toast.makeText(getContext(), "지도에서 위치를 선택해주세요", Toast.LENGTH_SHORT).show();
                return false;
            }

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "위치 정보가 올바르지 않습니다. 다시 선택해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void addLocation() {
        String name = binding.editName.getText().toString().trim();

        // 장소 이름이 비어있으면 선택된 위치 이름 사용
        if (name.isEmpty()) {
            name = binding.textSelectedLocationName.getText().toString();
        }

        double latitude = Double.parseDouble(binding.editLatitude.getText().toString().trim());
        double longitude = Double.parseDouble(binding.editLongitude.getText().toString().trim());
        boolean isFrequent = binding.checkboxFrequent.isChecked();
        boolean notificationEnabled = binding.checkboxNotification.isChecked();

        // 새 위치 객체 생성
        Location location = new Location(0, name, latitude, longitude, isFrequent, notificationEnabled);

        // 리스너 콜백
        if (listener != null) {
            listener.onLocationAdded(location);
        }
    }
}