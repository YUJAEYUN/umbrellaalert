package com.example.umbrellaalert;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.example.umbrellaalert.service.PersistentNotificationService;
import com.example.umbrellaalert.service.WeatherUpdateService;
import com.example.umbrellaalert.ui.settings.ThemeActivity;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class UmbrellaApplication extends Application {

    public static final String CHANNEL_ID = "umbrella_alert_channel";

    @Override
    public void onCreate() {
        super.onCreate();

        // 저장된 테마 적용
        ThemeActivity.applyTheme(this);

        // 알림 채널 생성
        createNotificationChannel();

        // 서비스 시작
        WeatherUpdateService.startService(this);

        // 상태바 알림 서비스 시작 (설정에 따라)
        if (PersistentNotificationService.isEnabled(this)) {
            PersistentNotificationService.setEnabled(this, true);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "우산 알림",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("날씨 상태에 따른 우산 필요 여부 알림");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}