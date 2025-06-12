package com.example.umbrellaalert;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.example.umbrellaalert.service.LocationSearchService;
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

        // 네이버 클라우드 플랫폼 Geocoding API 초기화
        LocationSearchService.initialize(this);

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
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            // 기본 우산 알림 채널
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "우산 알림",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("날씨 상태에 따른 우산 필요 여부 알림");
            notificationManager.createNotificationChannel(channel);

            // 상태바 지속 알림 채널
            NotificationChannel persistentChannel = new NotificationChannel(
                    "weather_persistent_channel",
                    "날씨 알림",
                    NotificationManager.IMPORTANCE_LOW
            );
            persistentChannel.setDescription("현재 날씨 정보를 표시합니다");
            persistentChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(persistentChannel);

            // 버스 알림 채널
            NotificationChannel busChannel = new NotificationChannel(
                    "bus_notification_channel",
                    "버스 알림",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            busChannel.setDescription("등록된 버스 도착 알림");
            notificationManager.createNotificationChannel(busChannel);
        }
    }
}