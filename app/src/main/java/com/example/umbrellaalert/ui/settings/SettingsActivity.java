package com.example.umbrellaalert.ui.settings;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.umbrellaalert.databinding.ActivitySettingsBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SettingsViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ViewModel 초기화
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        // UI 설정
        setupUI();

        // LiveData 관찰
        observeViewModel();
    }

    private void observeViewModel() {
        // 스위치 상태 관찰
        viewModel.getMorningAlertEnabled().observe(this, enabled ->
            binding.switchMorningAlert.setChecked(enabled));

        viewModel.getRainAlertEnabled().observe(this, enabled ->
            binding.switchRainAlert.setChecked(enabled));

        viewModel.getVibrationEnabled().observe(this, enabled ->
            binding.switchVibration.setChecked(enabled));

        viewModel.getSoundEnabled().observe(this, enabled ->
            binding.switchSound.setChecked(enabled));

        viewModel.getWidgetEnabled().observe(this, enabled ->
            binding.switchWidget.setChecked(enabled));

        viewModel.getWidgetAutoUpdateEnabled().observe(this, enabled ->
            binding.switchWidgetAutoUpdate.setChecked(enabled));

        viewModel.getPersistentNotificationEnabled().observe(this, enabled ->
            binding.switchPersistentNotification.setChecked(enabled));

        viewModel.getBusNotificationEnabled().observe(this, enabled ->
            binding.switchBusNotification.setChecked(enabled));

        // 시간 텍스트 관찰
        viewModel.getTimeText().observe(this, text ->
            binding.timeText.setText(text));

        // 토스트 메시지 관찰
        viewModel.getToastMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupUI() {
        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener(v -> finish());

        // 아침 알림 스위치
        binding.switchMorningAlert.setOnCheckedChangeListener((buttonView, isChecked) ->
            viewModel.setMorningAlertEnabled(isChecked));

        // 비소식 알림 스위치
        binding.switchRainAlert.setOnCheckedChangeListener((buttonView, isChecked) ->
            viewModel.setRainAlertEnabled(isChecked));

        // 진동 스위치
        binding.switchVibration.setOnCheckedChangeListener((buttonView, isChecked) ->
            viewModel.setVibrationEnabled(isChecked));

        // 소리 스위치
        binding.switchSound.setOnCheckedChangeListener((buttonView, isChecked) ->
            viewModel.setSoundEnabled(isChecked));

        // 위젯 활성화 스위치
        binding.switchWidget.setOnCheckedChangeListener((buttonView, isChecked) ->
            viewModel.setWidgetEnabled(isChecked));

        // 위젯 자동 업데이트 스위치
        binding.switchWidgetAutoUpdate.setOnCheckedChangeListener((buttonView, isChecked) ->
            viewModel.setWidgetAutoUpdateEnabled(isChecked));

        // 상태바 알림 스위치
        binding.switchPersistentNotification.setOnCheckedChangeListener((buttonView, isChecked) ->
            viewModel.setPersistentNotificationEnabled(isChecked));

        // 버스 알림 스위치
        binding.switchBusNotification.setOnCheckedChangeListener((buttonView, isChecked) ->
            viewModel.setBusNotificationEnabled(isChecked));

        // 아침 알림 시간 설정
        binding.timePickerContainer.setOnClickListener(v -> showTimePickerDialog());
    }

    private void showTimePickerDialog() {
        // ViewModel에서 현재 설정된 시간 가져오기
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    // ViewModel에 시간 설정
                    viewModel.setMorningAlarmTime(hourOfDay, minute);
                },
                7, 0, false); // 기본값은 ViewModel에서 처리

        timePickerDialog.show();
    }
}