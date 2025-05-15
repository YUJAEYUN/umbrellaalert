package com.example.umbrellaalert.ui.settings;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.umbrellaalert.widget.WeatherWidgetProvider;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.umbrellaalert.R;
import com.example.umbrellaalert.databinding.ActivitySettingsBinding;
import com.example.umbrellaalert.receiver.AlarmReceiver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SharedPreferences preferences;
    private AlarmManager alarmManager;
    private PendingIntent morningAlarmIntent;

    // 상수
    private static final String PREF_NAME = "UmbrellaAlertPrefs";
    private static final String KEY_MORNING_ALERT = "morning_alert_enabled";
    private static final String KEY_RAIN_ALERT = "rain_alert_enabled";
    private static final String KEY_VIBRATION = "vibration_enabled";
    private static final String KEY_SOUND = "sound_enabled";
    private static final String KEY_MORNING_HOUR = "morning_hour";
    private static final String KEY_MORNING_MINUTE = "morning_minute";
    private static final String KEY_WIDGET_ENABLED = "widget_enabled";
    private static final String KEY_WIDGET_AUTO_UPDATE = "widget_auto_update";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // SharedPreferences 초기화
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // AlarmManager 초기화
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // 아침 알람 인텐트 생성
        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        morningAlarmIntent = PendingIntent.getBroadcast(
                this, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE);

        // UI 설정
        setupUI();

        // 설정값 로드
        loadSettings();
    }

    private void setupUI() {
        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 아침 알림 스위치
        binding.switchMorningAlert.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(KEY_MORNING_ALERT, isChecked).apply();
                updateMorningAlarmSettings();
            }
        });

        // 비소식 알림 스위치
        binding.switchRainAlert.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(KEY_RAIN_ALERT, isChecked).apply();
            }
        });

        // 진동 스위치
        binding.switchVibration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(KEY_VIBRATION, isChecked).apply();
            }
        });

        // 소리 스위치
        binding.switchSound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(KEY_SOUND, isChecked).apply();
            }
        });

        // 위젯 활성화 스위치
        binding.switchWidget.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(KEY_WIDGET_ENABLED, isChecked).apply();
                updateWidgetSettings(isChecked);
            }
        });

        // 위젯 자동 업데이트 스위치
        binding.switchWidgetAutoUpdate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(KEY_WIDGET_AUTO_UPDATE, isChecked).apply();
                updateWidgetUpdateSettings(isChecked);
            }
        });

        // 아침 알림 시간 설정
        binding.timePickerContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        });
    }

    private void loadSettings() {
        // 스위치 상태 로드
        binding.switchMorningAlert.setChecked(preferences.getBoolean(KEY_MORNING_ALERT, true));
        binding.switchRainAlert.setChecked(preferences.getBoolean(KEY_RAIN_ALERT, true));
        binding.switchVibration.setChecked(preferences.getBoolean(KEY_VIBRATION, true));
        binding.switchSound.setChecked(preferences.getBoolean(KEY_SOUND, true));
        binding.switchWidget.setChecked(preferences.getBoolean(KEY_WIDGET_ENABLED, false));
        binding.switchWidgetAutoUpdate.setChecked(preferences.getBoolean(KEY_WIDGET_AUTO_UPDATE, true));

        // 아침 알림 시간 로드 및 표시
        int hour = preferences.getInt(KEY_MORNING_HOUR, 7);
        int minute = preferences.getInt(KEY_MORNING_MINUTE, 0);
        updateTimeText(hour, minute);
    }

    private void showTimePickerDialog() {
        int hour = preferences.getInt(KEY_MORNING_HOUR, 7);
        int minute = preferences.getInt(KEY_MORNING_MINUTE, 0);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        // 시간 저장
                        preferences.edit()
                                .putInt(KEY_MORNING_HOUR, hourOfDay)
                                .putInt(KEY_MORNING_MINUTE, minute)
                                .apply();

                        // 시간 표시 업데이트
                        updateTimeText(hourOfDay, minute);

                        // 알람 업데이트
                        updateMorningAlarmSettings();

                        Toast.makeText(SettingsActivity.this,
                                "아침 알림 시간이 설정되었습니다", Toast.LENGTH_SHORT).show();
                    }
                }, hour, minute, false);

        timePickerDialog.show();
    }

    private void updateTimeText(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        SimpleDateFormat format = new SimpleDateFormat("a h:mm", Locale.getDefault());
        binding.timeText.setText(format.format(calendar.getTime()));
    }

    private void updateMorningAlarmSettings() {
        boolean isEnabled = preferences.getBoolean(KEY_MORNING_ALERT, true);

        if (isEnabled) {
            // 알람 설정
            int hour = preferences.getInt(KEY_MORNING_HOUR, 7);
            int minute = preferences.getInt(KEY_MORNING_MINUTE, 0);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            // 이미 지난 시간이면 다음 날로 설정
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            // 알람 설정 (매일 반복)
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    morningAlarmIntent);

            Toast.makeText(this, "아침 알림이 활성화되었습니다", Toast.LENGTH_SHORT).show();
        } else {
            // 알람 취소
            alarmManager.cancel(morningAlarmIntent);
            Toast.makeText(this, "아침 알림이 비활성화되었습니다", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 위젯 활성화/비활성화 설정
     */
    private void updateWidgetSettings(boolean isEnabled) {
        if (isEnabled) {
            // 위젯 활성화 로직
            Intent intent = new Intent(this, WeatherWidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            sendBroadcast(intent);
            Toast.makeText(this, "날씨 위젯이 활성화되었습니다", Toast.LENGTH_SHORT).show();
        } else {
            // 위젯 비활성화 로직
            Toast.makeText(this, "날씨 위젯이 비활성화되었습니다", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 위젯 자동 업데이트 설정
     */
    private void updateWidgetUpdateSettings(boolean isEnabled) {
        if (isEnabled) {
            // 위젯 자동 업데이트 활성화
            Toast.makeText(this, "위젯 자동 업데이트가 활성화되었습니다", Toast.LENGTH_SHORT).show();
        } else {
            // 위젯 자동 업데이트 비활성화
            Toast.makeText(this, "위젯 자동 업데이트가 비활성화되었습니다", Toast.LENGTH_SHORT).show();
        }
    }
}