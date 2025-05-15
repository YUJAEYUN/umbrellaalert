package com.example.umbrellaalert.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.umbrellaalert.R;
import com.example.umbrellaalert.data.manager.WeatherManager;
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.ui.home.HomeActivity;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 상태바에 지속적인 날씨 알림을 표시하는 서비스
 */
public class PersistentNotificationService extends Service {

    private static final String TAG = "PersistentNotifService";
    private static final String CHANNEL_ID = "weather_persistent_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long UPDATE_INTERVAL = TimeUnit.MINUTES.toMillis(30); // 30분마다 업데이트

    private static final String PREF_NAME = "UmbrellaAlertPrefs";
    private static final String KEY_PERSISTENT_NOTIFICATION = "persistent_notification_enabled";

    private WeatherManager weatherManager;
    private ExecutorService executorService;
    private Handler handler;
    private Runnable updateRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        weatherManager = WeatherManager.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());

        // 업데이트 Runnable 정의
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateWeatherNotification();
                // 다음 업데이트 예약
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        };

        // 알림 채널 생성
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 서비스가 시작될 때 알림 표시
        updateWeatherNotification();

        // 주기적 업데이트 시작
        handler.postDelayed(updateRunnable, UPDATE_INTERVAL);

        // 서비스가 종료되면 자동으로 재시작
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 업데이트 중지
        handler.removeCallbacks(updateRunnable);
        executorService.shutdown();
    }

    /**
     * 알림 채널 생성 (Android 8.0 이상)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "날씨 알림",
                    NotificationManager.IMPORTANCE_LOW); // 중요도 낮게 설정
            channel.setDescription("현재 날씨 정보를 표시합니다");
            channel.setShowBadge(false); // 배지 표시 안함

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * 날씨 알림 업데이트
     */
    private void updateWeatherNotification() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // 서울 좌표 (위도, 경도) 기본값 사용
                    Weather weather = weatherManager.getCurrentWeather(37.5665, 126.9780);

                    if (weather != null) {
                        // 메인 스레드에서 알림 업데이트
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                showWeatherNotification(weather);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "날씨 정보 업데이트 중 오류 발생", e);
                }
            }
        });
    }

    /**
     * 날씨 알림 표시
     */
    private void showWeatherNotification(Weather weather) {
        // 앱 실행 인텐트
        Intent intent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // 알림 내용 구성
        String title = String.format(Locale.getDefault(), "현재 온도: %.1f°C", weather.getTemperature());
        String content = getWeatherConditionText(weather.getWeatherCondition());
        
        if (weather.isNeedUmbrella()) {
            content += " - 우산이 필요합니다!";
        } else {
            content += " - 우산이 필요하지 않습니다";
        }

        // 알림 생성
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(weather.isNeedUmbrella() ? R.drawable.ic_umbrella : R.drawable.ic_weather_sunny)
                .setContentTitle(title)
                .setContentText(content)
                .setOngoing(true) // 사용자가 스와이프로 제거할 수 없음
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        // 포그라운드 서비스로 실행
        startForeground(NOTIFICATION_ID, notification);
    }

    /**
     * 날씨 상태 텍스트 변환
     */
    private String getWeatherConditionText(String condition) {
        if (condition.equalsIgnoreCase("Clear")) {
            return "맑음";
        } else if (condition.equalsIgnoreCase("Clouds")) {
            return "구름";
        } else if (condition.equalsIgnoreCase("Rain")) {
            return "비";
        } else if (condition.equalsIgnoreCase("Drizzle")) {
            return "이슬비";
        } else if (condition.equalsIgnoreCase("Thunderstorm")) {
            return "뇌우";
        } else if (condition.equalsIgnoreCase("Snow")) {
            return "눈";
        } else if (condition.equalsIgnoreCase("Atmosphere")) {
            return "안개";
        } else {
            return condition;
        }
    }

    /**
     * 서비스 활성화 여부 확인
     */
    public static boolean isEnabled(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_PERSISTENT_NOTIFICATION, false);
    }

    /**
     * 서비스 활성화/비활성화 설정
     */
    public static void setEnabled(Context context, boolean enabled) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_PERSISTENT_NOTIFICATION, enabled).apply();

        if (enabled) {
            // 서비스 시작
            Intent intent = new Intent(context, PersistentNotificationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } else {
            // 서비스 중지
            context.stopService(new Intent(context, PersistentNotificationService.class));
        }
    }
}
