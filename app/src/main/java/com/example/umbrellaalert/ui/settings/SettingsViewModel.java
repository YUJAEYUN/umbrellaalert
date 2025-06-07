package com.example.umbrellaalert.ui.settings;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.umbrellaalert.receiver.AlarmReceiver;
import com.example.umbrellaalert.service.BusNotificationService;
import com.example.umbrellaalert.service.PersistentNotificationService;
import com.example.umbrellaalert.widget.WeatherWidgetProvider;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * 설정 관리를 위한 ViewModel
 */
public class SettingsViewModel extends AndroidViewModel {

    private static final String TAG = "SettingsViewModel";
    
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
    private static final String KEY_PERSISTENT_NOTIFICATION = "persistent_notification_enabled";
    private static final String KEY_BUS_NOTIFICATION = "bus_notification_enabled";
    
    private final SharedPreferences preferences;
    private final AlarmManager alarmManager;
    private final PendingIntent morningAlarmIntent;
    
    // LiveData
    private final MutableLiveData<Boolean> morningAlertEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> rainAlertEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> vibrationEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> soundEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> widgetEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> widgetAutoUpdateEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> persistentNotificationEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> busNotificationEnabled = new MutableLiveData<>();
    private final MutableLiveData<String> timeText = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    
    public SettingsViewModel(@NonNull Application application) {
        super(application);
        
        // SharedPreferences 초기화
        preferences = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // AlarmManager 초기화
        alarmManager = (AlarmManager) application.getSystemService(Context.ALARM_SERVICE);
        
        // 아침 알람 인텐트 생성
        Intent alarmIntent = new Intent(application, AlarmReceiver.class);
        morningAlarmIntent = PendingIntent.getBroadcast(
                application, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE);
        
        // 설정값 로드
        loadSettings();
    }
    
    /**
     * 설정값 로드
     */
    private void loadSettings() {
        morningAlertEnabled.setValue(preferences.getBoolean(KEY_MORNING_ALERT, true));
        rainAlertEnabled.setValue(preferences.getBoolean(KEY_RAIN_ALERT, true));
        vibrationEnabled.setValue(preferences.getBoolean(KEY_VIBRATION, true));
        soundEnabled.setValue(preferences.getBoolean(KEY_SOUND, true));
        widgetEnabled.setValue(preferences.getBoolean(KEY_WIDGET_ENABLED, true)); // 기본값을 true로 변경
        widgetAutoUpdateEnabled.setValue(preferences.getBoolean(KEY_WIDGET_AUTO_UPDATE, true));
        persistentNotificationEnabled.setValue(preferences.getBoolean(KEY_PERSISTENT_NOTIFICATION, false));
        busNotificationEnabled.setValue(preferences.getBoolean(KEY_BUS_NOTIFICATION, false));

        // 아침 알림 시간 로드 및 표시
        int hour = preferences.getInt(KEY_MORNING_HOUR, 7);
        int minute = preferences.getInt(KEY_MORNING_MINUTE, 0);
        updateTimeText(hour, minute);
    }
    
    /**
     * 아침 알림 설정 변경
     */
    public void setMorningAlertEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_MORNING_ALERT, enabled).apply();
        morningAlertEnabled.setValue(enabled);
        updateMorningAlarmSettings();
    }
    
    /**
     * 비소식 알림 설정 변경
     */
    public void setRainAlertEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_RAIN_ALERT, enabled).apply();
        rainAlertEnabled.setValue(enabled);
    }
    
    /**
     * 진동 설정 변경
     */
    public void setVibrationEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_VIBRATION, enabled).apply();
        vibrationEnabled.setValue(enabled);
    }
    
    /**
     * 소리 설정 변경
     */
    public void setSoundEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_SOUND, enabled).apply();
        soundEnabled.setValue(enabled);
    }
    
    /**
     * 위젯 활성화 설정 변경
     */
    public void setWidgetEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_WIDGET_ENABLED, enabled).apply();
        widgetEnabled.setValue(enabled);
        updateWidgetSettings(enabled);
    }
    
    /**
     * 위젯 자동 업데이트 설정 변경
     */
    public void setWidgetAutoUpdateEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_WIDGET_AUTO_UPDATE, enabled).apply();
        widgetAutoUpdateEnabled.setValue(enabled);
        updateWidgetUpdateSettings(enabled);
    }
    
    /**
     * 상태바 알림 설정 변경
     */
    public void setPersistentNotificationEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_PERSISTENT_NOTIFICATION, enabled).apply();
        persistentNotificationEnabled.setValue(enabled);
        PersistentNotificationService.setEnabled(getApplication(), enabled);
    }

    /**
     * 버스 알림 설정 변경
     */
    public void setBusNotificationEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_BUS_NOTIFICATION, enabled).apply();
        busNotificationEnabled.setValue(enabled);
        BusNotificationService.setEnabled(getApplication(), enabled);

        String message = enabled ? "버스 알림이 활성화되었습니다" : "버스 알림이 비활성화되었습니다";
        toastMessage.setValue(message);
    }
    
    /**
     * 아침 알림 시간 설정
     */
    public void setMorningAlarmTime(int hourOfDay, int minute) {
        // 시간 저장
        preferences.edit()
                .putInt(KEY_MORNING_HOUR, hourOfDay)
                .putInt(KEY_MORNING_MINUTE, minute)
                .apply();
        
        // 시간 표시 업데이트
        updateTimeText(hourOfDay, minute);
        
        // 알람 업데이트
        updateMorningAlarmSettings();
        
        toastMessage.setValue("아침 알림 시간이 설정되었습니다");
    }
    
    /**
     * 시간 텍스트 업데이트
     */
    private void updateTimeText(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        
        SimpleDateFormat format = new SimpleDateFormat("a h:mm", Locale.getDefault());
        timeText.setValue(format.format(calendar.getTime()));
    }
    
    /**
     * 아침 알람 설정 업데이트
     */
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
            
            toastMessage.setValue("아침 알림이 활성화되었습니다");
        } else {
            // 알람 취소
            alarmManager.cancel(morningAlarmIntent);
            toastMessage.setValue("아침 알림이 비활성화되었습니다");
        }
    }
    
    /**
     * 위젯 활성화/비활성화 설정
     */
    private void updateWidgetSettings(boolean isEnabled) {
        if (isEnabled) {
            // 위젯 활성화 로직 - 강제 업데이트 실행
            WeatherWidgetProvider.forceUpdateAllWidgets(getApplication());
            toastMessage.setValue("날씨 위젯이 활성화되었습니다. 기존 위젯이 업데이트됩니다.");
        } else {
            // 위젯 비활성화 로직 - 비활성화 메시지 표시
            WeatherWidgetProvider.forceUpdateAllWidgets(getApplication());
            toastMessage.setValue("날씨 위젯이 비활성화되었습니다");
        }
    }
    
    /**
     * 위젯 자동 업데이트 설정
     */
    private void updateWidgetUpdateSettings(boolean isEnabled) {
        if (isEnabled) {
            // 위젯 자동 업데이트 활성화
            toastMessage.setValue("위젯 자동 업데이트가 활성화되었습니다");
        } else {
            // 위젯 자동 업데이트 비활성화
            toastMessage.setValue("위젯 자동 업데이트가 비활성화되었습니다");
        }
    }
    
    // LiveData Getters
    public LiveData<Boolean> getMorningAlertEnabled() {
        return morningAlertEnabled;
    }
    
    public LiveData<Boolean> getRainAlertEnabled() {
        return rainAlertEnabled;
    }
    
    public LiveData<Boolean> getVibrationEnabled() {
        return vibrationEnabled;
    }
    
    public LiveData<Boolean> getSoundEnabled() {
        return soundEnabled;
    }
    
    public LiveData<Boolean> getWidgetEnabled() {
        return widgetEnabled;
    }
    
    public LiveData<Boolean> getWidgetAutoUpdateEnabled() {
        return widgetAutoUpdateEnabled;
    }
    
    public LiveData<Boolean> getPersistentNotificationEnabled() {
        return persistentNotificationEnabled;
    }

    public LiveData<Boolean> getBusNotificationEnabled() {
        return busNotificationEnabled;
    }

    public LiveData<String> getTimeText() {
        return timeText;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }
}
