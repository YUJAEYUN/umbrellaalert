package com.example.umbrellaalert.ui.location;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.umbrellaalert.data.model.Location;
import com.example.umbrellaalert.databinding.DialogAddLocationBinding;

public class AddLocationDialog extends DialogFragment {

    private static final int MAP_PICKER_REQUEST_CODE = 1001;

    private DialogAddLocationBinding binding;
    private LocationAddedListener listener;

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