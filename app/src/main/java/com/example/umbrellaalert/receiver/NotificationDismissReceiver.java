package com.example.umbrellaalert.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.umbrellaalert.service.PersistentNotificationService;

/**
 * 알림 지우기 액션을 처리하는 리시버
 */
public class NotificationDismissReceiver extends BroadcastReceiver {
    
    private static final String TAG = "NotificationDismiss";
    public static final String ACTION_DISMISS_PERSISTENT = "com.example.umbrellaalert.DISMISS_PERSISTENT";
    public static final String ACTION_DISMISS_WEATHER = "com.example.umbrellaalert.DISMISS_WEATHER";
    public static final String ACTION_DISMISS_BUS = "com.example.umbrellaalert.DISMISS_BUS";
    
    private static final String PREF_NAME = "UmbrellaAlertPrefs";
    private static final String KEY_PERSISTENT_DISMISSED = "persistent_notification_dismissed";
    private static final String KEY_WEATHER_DISMISSED = "weather_notification_dismissed";
    private static final String KEY_BUS_DISMISSED = "bus_notification_dismissed";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "알림 지우기 액션 수신: " + action);
        
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        if (ACTION_DISMISS_PERSISTENT.equals(action)) {
            // 지속적 알림 비활성화
            editor.putBoolean(KEY_PERSISTENT_DISMISSED, true);
            PersistentNotificationService.setEnabled(context, false);
            Log.d(TAG, "지속적 알림 비활성화됨");
            
        } else if (ACTION_DISMISS_WEATHER.equals(action)) {
            // 날씨 알림 일시 중지 (1시간)
            long dismissTime = System.currentTimeMillis();
            editor.putLong(KEY_WEATHER_DISMISSED, dismissTime);
            Log.d(TAG, "날씨 알림 1시간 중지됨");
            
        } else if (ACTION_DISMISS_BUS.equals(action)) {
            // 버스 알림 일시 중지 (30분)
            long dismissTime = System.currentTimeMillis();
            editor.putLong(KEY_BUS_DISMISSED, dismissTime);
            Log.d(TAG, "버스 알림 30분 중지됨");
        }
        
        editor.apply();
    }
    
    /**
     * 지속적 알림이 사용자에 의해 비활성화되었는지 확인
     */
    public static boolean isPersistentNotificationDismissed(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_PERSISTENT_DISMISSED, false);
    }
    
    /**
     * 날씨 알림이 일시 중지되었는지 확인 (1시간)
     */
    public static boolean isWeatherNotificationDismissed(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long dismissTime = prefs.getLong(KEY_WEATHER_DISMISSED, 0);
        long currentTime = System.currentTimeMillis();
        long oneHour = 60 * 60 * 1000; // 1시간
        
        return (currentTime - dismissTime) < oneHour;
    }
    
    /**
     * 버스 알림이 일시 중지되었는지 확인 (30분)
     */
    public static boolean isBusNotificationDismissed(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long dismissTime = prefs.getLong(KEY_BUS_DISMISSED, 0);
        long currentTime = System.currentTimeMillis();
        long thirtyMinutes = 30 * 60 * 1000; // 30분
        
        return (currentTime - dismissTime) < thirtyMinutes;
    }
    
    /**
     * 지속적 알림 비활성화 상태 초기화 (설정에서 다시 활성화할 때 사용)
     */
    public static void resetPersistentDismiss(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_PERSISTENT_DISMISSED, false).apply();
    }
}
