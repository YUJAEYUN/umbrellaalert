package com.example.umbrellaalert.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.RequiresApi;

import androidx.core.app.NotificationCompat;

import com.example.umbrellaalert.R;
import com.example.umbrellaalert.UmbrellaApplication;
import com.example.umbrellaalert.data.manager.WeatherManager;
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.ui.home.HomeActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WeatherUpdateService extends Service {

    private static final String TAG = "WeatherUpdateService";
    private static final int NOTIFICATION_ID = 1001;
    private static final long UPDATE_INTERVAL = TimeUnit.HOURS.toMillis(1); // 1시간 간격 업데이트

    private ExecutorService executorService;
    private Handler handler;
    private LocationService locationService;
    private WeatherManager weatherManager;
    private boolean isRunning;

    // 서비스 시작 (정적 메소드)
    public static void startService(Context context) {
        Intent intent = new Intent(context, WeatherUpdateService.class);

        // API 레벨 26 이상에서는 startForegroundService 사용
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        executorService = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
        locationService = LocationService.getInstance(this);
        weatherManager = WeatherManager.getInstance(this);
        isRunning = false;

        // 즉시 포그라운드 서비스로 시작
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForegroundQ(NOTIFICATION_ID, createNotification("날씨 서비스 시작 중..."));
        } else {
            startForeground(NOTIFICATION_ID, createNotification("날씨 서비스 시작 중..."));
        }

        Log.d(TAG, "Weather update service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");

        if (!isRunning) {
            isRunning = true;
            startWeatherUpdates();
        }

        return START_STICKY;
    }

    // 날씨 업데이트 시작
    private void startWeatherUpdates() {
        // 위치 업데이트 시작
        locationService.startLocationUpdates(new LocationService.LocationCallback() {
            @Override
            public void onLocationUpdate(Location location) {
                // 위치가 업데이트되면 날씨 정보 가져오기
                updateWeatherForLocation(location);
            }
        });

        // 주기적 업데이트 스케줄링
        scheduleNextUpdate();
    }

    // 위치에 따른 날씨 업데이트
    private void updateWeatherForLocation(Location location) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // 날씨 정보 가져오기
                    weatherManager.getCurrentWeather(
                            location.getLatitude(), location.getLongitude(), new WeatherManager.WeatherCallback() {
                                @Override
                                public void onSuccess(Weather weather) {
                                    if (weather != null) {
                                        // UI 쓰레드에서 알림 업데이트
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                updateNotification(weather);

                                                // 우산이 필요하면 특별 알림 생성
                                                if (weather.isNeedUmbrella()) {
                                                    sendUmbrellaNotification(weather);
                                                }
                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e(TAG, "Failed to get weather: " + error);
                                }
                            });
                } catch (Exception e) {
                    Log.e(TAG, "Error updating weather", e);
                }
            }
        });
    }

    // 포그라운드 서비스 알림 생성
    private Notification createNotification(String content) {
        Intent notificationIntent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, UmbrellaApplication.CHANNEL_ID)
                .setContentTitle("아 맞다 우산!")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_umbrella_small)
                .setContentIntent(pendingIntent)
                .build();
    }

    // 알림 업데이트
    private void updateNotification(Weather weather) {
        String message = weatherManager.getCatMessage(weather);
        Notification notification = createNotification(message);

        // 알림 업데이트
        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    // 우산 필요 알림 전송
    private void sendUmbrellaNotification(Weather weather) {
        Intent notificationIntent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        String message = "비가 올 예정이다냥! 우산을 챙겨라냥~";

        Notification notification = new NotificationCompat.Builder(this, UmbrellaApplication.CHANNEL_ID)
                .setContentTitle("아 맞다 우산!")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setSmallIcon(R.drawable.ic_umbrella_small)
                .setContentIntent(pendingIntent)
                .setColor(getResources().getColor(R.color.alert_color, null))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();

        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID + 1, notification);
    }

    // 다음 업데이트 스케줄링
    private void scheduleNextUpdate() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Location lastLocation = locationService.getLastLocation();
                if (lastLocation != null) {
                    updateWeatherForLocation(lastLocation);
                }

                // 다음 업데이트 스케줄링
                scheduleNextUpdate();
            }
        }, UPDATE_INTERVAL);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // 서비스 정리
        isRunning = false;
        locationService.stopLocationUpdates();
        executorService.shutdown();
        handler.removeCallbacksAndMessages(null);

        Log.d(TAG, "Weather update service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // 바인딩 불필요
    }

    /**
     * Android 10(API 29) 이상에서 Foreground Service 타입을 지정하는 메서드
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void startForegroundQ(int id, Notification notification) {
        startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
    }
}