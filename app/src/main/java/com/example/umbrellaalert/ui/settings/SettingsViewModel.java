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
import com.example.umbrellaalert.receiver.NotificationDismissReceiver;
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
    private static final String KEY_AUTO_STOP_ENABLED = "auto_stop_enabled";
    private static final String KEY_RAIN_ALERT = "rain_alert_enabled";
    private static final String KEY_VIBRATION = "vibration_enabled";
    private static final String KEY_SOUND = "sound_enabled";
    private static final String KEY_STOP_HOUR = "stop_hour";
    private static final String KEY_STOP_MINUTE = "stop_minute";
    private static final String KEY_WIDGET_ENABLED = "widget_enabled";
    private static final String KEY_WIDGET_AUTO_UPDATE = "widget_auto_update";
    private static final String KEY_PERSISTENT_NOTIFICATION = "persistent_notification_enabled";
    private static final String KEY_BUS_NOTIFICATION = "bus_notification_enabled";
    
    private final SharedPreferences preferences;
    private final AlarmManager alarmManager;
    private final PendingIntent morningAlarmIntent;
    
    // LiveData
    private final MutableLiveData<Boolean> autoStopEnabled = new MutableLiveData<>();
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
        autoStopEnabled.setValue(preferences.getBoolean(KEY_AUTO_STOP_ENABLED, true));
        rainAlertEnabled.setValue(preferences.getBoolean(KEY_RAIN_ALERT, true));
        vibrationEnabled.setValue(preferences.getBoolean(KEY_VIBRATION, true));
        soundEnabled.setValue(preferences.getBoolean(KEY_SOUND, true));
        widgetEnabled.setValue(preferences.getBoolean(KEY_WIDGET_ENABLED, true)); // 기본값을 true로 변경
        widgetAutoUpdateEnabled.setValue(preferences.getBoolean(KEY_WIDGET_AUTO_UPDATE, true));
        persistentNotificationEnabled.setValue(preferences.getBoolean(KEY_PERSISTENT_NOTIFICATION, false));
        busNotificationEnabled.setValue(preferences.getBoolean(KEY_BUS_NOTIFICATION, false));

        // 알림 종료 시간 로드 및 표시
        int hour = preferences.getInt(KEY_STOP_HOUR, 10);
        int minute = preferences.getInt(KEY_STOP_MINUTE, 0);
        updateTimeText(hour, minute);
    }
    
    /**
     * 알림 자동 종료 설정 변경
     */
    public void setAutoStopEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_AUTO_STOP_ENABLED, enabled).apply();
        autoStopEnabled.setValue(enabled);
        // 설정이 변경되면 현재 시간 체크
        checkAndStopNotificationsIfNeeded();
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

        if (enabled) {
            // 알림을 다시 활성화할 때 dismiss 상태 초기화
            NotificationDismissReceiver.resetPersistentDismiss(getApplication());
        }

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
     * 알림 종료 시간 설정
     */
    public void setStopTime(int hourOfDay, int minute) {
        // 시간 저장
        preferences.edit()
                .putInt(KEY_STOP_HOUR, hourOfDay)
                .putInt(KEY_STOP_MINUTE, minute)
                .apply();

        // 시간 표시 업데이트
        updateTimeText(hourOfDay, minute);

        // 현재 시간 체크
        checkAndStopNotificationsIfNeeded();

        toastMessage.setValue("알림 종료 시간이 설정되었습니다");
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
     * 현재 시간이 종료 시간을 지났는지 확인하고 알림 중단
     */
    private void checkAndStopNotificationsIfNeeded() {
        boolean isAutoStopEnabled = preferences.getBoolean(KEY_AUTO_STOP_ENABLED, true);

        if (!isAutoStopEnabled) {
            return; // 자동 종료가 비활성화되어 있으면 아무것도 하지 않음
        }

        int stopHour = preferences.getInt(KEY_STOP_HOUR, 10);
        int stopMinute = preferences.getInt(KEY_STOP_MINUTE, 0);

        Calendar now = Calendar.getInstance();
        Calendar stopTime = Calendar.getInstance();
        stopTime.set(Calendar.HOUR_OF_DAY, stopHour);
        stopTime.set(Calendar.MINUTE, stopMinute);
        stopTime.set(Calendar.SECOND, 0);

        // 현재 시간이 종료 시간을 지났으면 알림 중단
        if (now.after(stopTime)) {
            // 상태바 알림 중단
            if (preferences.getBoolean(KEY_PERSISTENT_NOTIFICATION, false)) {
                PersistentNotificationService.setEnabled(getApplication(), false);
                preferences.edit().putBoolean(KEY_PERSISTENT_NOTIFICATION, false).apply();
                persistentNotificationEnabled.setValue(false);
            }

            // 버스 알림 중단
            if (preferences.getBoolean(KEY_BUS_NOTIFICATION, false)) {
                BusNotificationService.setEnabled(getApplication(), false);
                preferences.edit().putBoolean(KEY_BUS_NOTIFICATION, false).apply();
                busNotificationEnabled.setValue(false);
            }

            toastMessage.setValue("설정된 시간이 지나 알림이 자동으로 중단되었습니다");
        }
    }

    /**
     * 시간이 지났는지 확인하는 공개 메서드 (다른 클래스에서 호출 가능)
     */
    public static boolean shouldStopNotifications(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isAutoStopEnabled = preferences.getBoolean(KEY_AUTO_STOP_ENABLED, true);

        if (!isAutoStopEnabled) {
            return false;
        }

        int stopHour = preferences.getInt(KEY_STOP_HOUR, 10);
        int stopMinute = preferences.getInt(KEY_STOP_MINUTE, 0);

        Calendar now = Calendar.getInstance();
        Calendar stopTime = Calendar.getInstance();
        stopTime.set(Calendar.HOUR_OF_DAY, stopHour);
        stopTime.set(Calendar.MINUTE, stopMinute);
        stopTime.set(Calendar.SECOND, 0);

        return now.after(stopTime);
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
    public LiveData<Boolean> getAutoStopEnabled() {
        return autoStopEnabled;
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
