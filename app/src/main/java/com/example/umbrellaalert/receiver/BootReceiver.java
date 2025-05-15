package com.example.umbrellaalert.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.umbrellaalert.service.PersistentNotificationService;
import com.example.umbrellaalert.service.WeatherUpdateService;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed, starting weather service");

            // 날씨 업데이트 서비스 시작
            WeatherUpdateService.startService(context);

            // 상태바 알림 서비스 시작 (설정에 따라)
            if (PersistentNotificationService.isEnabled(context)) {
                PersistentNotificationService.setEnabled(context, true);
            }
        }
    }
}