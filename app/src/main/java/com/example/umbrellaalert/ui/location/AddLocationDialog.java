package com.example.umbrellaalert.ui.location;

import android.app.Dialog;
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

    private boolean validateInput() {
        String name = binding.editName.getText().toString().trim();
        String latitudeStr = binding.editLatitude.getText().toString().trim();
        String longitudeStr = binding.editLongitude.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(getContext(), "장소 이름을 입력하세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(latitudeStr) || TextUtils.isEmpty(longitudeStr)) {
            Toast.makeText(getContext(), "위도와 경도를 입력하세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            double latitude = Double.parseDouble(latitudeStr);
            double longitude = Double.parseDouble(longitudeStr);

            // 유효한 위도/경도 범위 확인
            if (latitude < -90 || latitude > 90) {
                Toast.makeText(getContext(), "유효한 위도 범위는 -90에서 90 사이입니다", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (longitude < -180 || longitude > 180) {
                Toast.makeText(getContext(), "유효한 경도 범위는 -180에서 180 사이입니다", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "위도와 경도는 숫자여야 합니다", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void addLocation() {
        String name = binding.editName.getText().toString().trim();
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