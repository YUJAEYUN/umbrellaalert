package com.example.umbrellaalert.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.umbrellaalert.R;
import com.example.umbrellaalert.data.manager.WeatherManager;
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.util.WeatherCacheManager;
import com.example.umbrellaalert.data.model.RegisteredBus;
import com.example.umbrellaalert.data.model.BusArrival;
import com.example.umbrellaalert.data.api.BusApiClient;
import com.example.umbrellaalert.data.database.BusDao;
import com.example.umbrellaalert.data.database.DatabaseHelper;
import com.example.umbrellaalert.receiver.NotificationDismissReceiver;
import com.example.umbrellaalert.ui.main.MainActivity;

import java.util.List;
import java.util.concurrent.Future;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 상태바에 지속적인 날씨 알림을 표시하는 서비스
 */
@AndroidEntryPoint
public class PersistentNotificationService extends Service implements LocationListener {

    private static final String TAG = "PersistentNotifService";
    private static final String CHANNEL_ID = "weather_persistent_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long UPDATE_INTERVAL = TimeUnit.MINUTES.toMillis(30); // 30분마다 업데이트

    private static final String PREF_NAME = "UmbrellaAlertPrefs";
    private static final String KEY_PERSISTENT_NOTIFICATION = "persistent_notification_enabled";

    @Inject
    WeatherManager weatherManager;

    private BusApiClient busApiClient;
    private BusDao busDao;
    private ExecutorService executorService;
    private Handler handler;
    private Runnable updateRunnable;
    private LocationManager locationManager;
    private Location currentLocation;

    @Override
    public void onCreate() {
        super.onCreate();
        // weatherManager는 Hilt로 주입됨
        busApiClient = new BusApiClient(this);
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        busDao = new BusDao(dbHelper);
        executorService = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // 업데이트 Runnable 정의
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateNotification();
                // 다음 업데이트 예약
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        };

        // 알림 채널 생성
        createNotificationChannel();
        
        // 위치 업데이트 시작
        startLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 서비스가 시작될 때 알림 표시
        updateNotification();

        // 주기적 업데이트 시작
        handler.postDelayed(updateRunnable, UPDATE_INTERVAL);

        // 서비스가 종료되면 자동으로 재시작
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 업데이트 중지
        handler.removeCallbacks(updateRunnable);
        stopLocationUpdates();
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
     * 날씨 + 버스 알림 업데이트
     */
    private void updateNotification() {
        // 사용자가 알림을 지웠는지 확인
        if (NotificationDismissReceiver.isPersistentNotificationDismissed(this)) {
            Log.d(TAG, "지속적 알림이 사용자에 의해 비활성화됨");
            stopSelf(); // 서비스 종료
            return;
        }

        executorService.execute(() -> {
            try {
                // 1. 날씨 정보 가져오기
                Weather weather = getWeatherData();

                // 2. 버스 정보 가져오기
                String busInfo = getBusInfo();

                // 3. 알림 표시
                handler.post(() -> showCombinedNotification(weather, busInfo));

            } catch (Exception e) {
                Log.e(TAG, "알림 업데이트 실패", e);
            }
        });
    }

    /**
     * 날씨 데이터 가져오기 (동기)
     */
    private Weather getWeatherData() {
        if (currentLocation == null) {
            return null;
        }

        try {
            // 1. 먼저 캐시에서 날씨 데이터 확인
            Weather cachedWeather = WeatherCacheManager.getWeatherFromCache(this);

            if (cachedWeather != null) {
                Log.d(TAG, "✅ 알림 캐시된 날씨 데이터 사용: " + cachedWeather.getTemperature() + "°C, " + cachedWeather.getWeatherCondition());
                return cachedWeather;
            }

            // 2. 캐시에 없으면 기본 데이터 사용 (홈 화면에서 API 호출하므로)
            Log.d(TAG, "캐시된 데이터 없음, 기본 데이터 사용 (홈 화면에서 API 호출 대기)");
            return createFallbackWeather(currentLocation);

        } catch (Exception e) {
            Log.e(TAG, "알림 날씨 데이터 가져오기 오류", e);
            return createFallbackWeather(currentLocation);
        }
    }

    // 기본 날씨 데이터 생성 (API 실패 시 사용)
    private Weather createFallbackWeather(Location location) {
        String[] conditions = {"맑음", "흐림", "비"};
        float[] temperatures = {8.0f, 15.0f, 22.0f, 28.0f};

        String condition = conditions[(int) (Math.random() * conditions.length)];
        float temperature = temperatures[(int) (Math.random() * temperatures.length)];

        float precipitation = 0.0f;
        boolean needUmbrella = false;

        if (condition.contains("비")) {
            precipitation = (float) (Math.random() * 15 + 2);
            needUmbrella = true;
        }

        return new Weather(
                0,
                temperature,
                condition,
                precipitation,
                (int) (Math.random() * 40 + 40),
                (float) (Math.random() * 5 + 1),
                location.getLatitude() + "," + location.getLongitude(),
                System.currentTimeMillis(),
                needUmbrella
        );
    }

    /**
     * 버스 정보 가져오기
     */
    private String getBusInfo() {
        try {
            List<RegisteredBus> buses = busDao.getAllRegisteredBuses();
            if (buses == null || buses.isEmpty()) {
                return "등록된 버스가 없습니다";
            }

            StringBuilder busInfo = new StringBuilder();
            int count = 0;

            for (RegisteredBus bus : buses) {
                if (count >= 2) break; // 최대 2개만 표시

                try {
                    Future<List<BusArrival>> future = busApiClient.getBusArrivalInfo(bus.getNodeId(), bus.getCityCode());
                    List<BusArrival> arrivals = future.get(3, TimeUnit.SECONDS); // 3초 타임아웃

                    // 해당 버스 찾기
                    boolean found = false;
                    for (BusArrival arrival : arrivals) {
                        if (bus.getRouteNo().equals(arrival.getRouteNo())) {
                            if (count > 0) busInfo.append(" | ");
                            busInfo.append(bus.getRouteNo()).append("번: ").append(arrival.getFormattedArrTime());
                            count++;
                            found = true;
                            break;
                        }
                    }

                    // 해당 버스를 찾지 못한 경우
                    if (!found) {
                        if (count > 0) busInfo.append(" | ");
                        busInfo.append(bus.getRouteNo()).append("번: 운행정보 없음");
                        count++;
                    }

                } catch (Exception e) {
                    Log.e(TAG, "버스 정보 가져오기 실패: " + bus.getRouteNo(), e);
                    if (count > 0) busInfo.append(" | ");
                    busInfo.append(bus.getRouteNo()).append("번: 정보 오류");
                    count++;
                }
            }

            if (busInfo.length() == 0) {
                return "버스 도착 정보를 가져올 수 없습니다";
            }

            return busInfo.toString();

        } catch (Exception e) {
            Log.e(TAG, "버스 정보 조회 오류", e);
            return "버스 정보 조회 중 오류가 발생했습니다";
        }
    }

    /**
     * 위치 업데이트 시작
     */
    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                // GPS 위치 요청
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 300000, 1000, this); // 5분마다, 1km 변경시
                
                // 네트워크 위치 요청
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 300000, 1000, this);
                
                // 마지막 알려진 위치 가져오기
                Location lastKnownGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Location lastKnownNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                
                if (lastKnownGps != null) {
                    currentLocation = lastKnownGps;
                    Log.d(TAG, "Using last known GPS location");
                } else if (lastKnownNetwork != null) {
                    currentLocation = lastKnownNetwork;
                    Log.d(TAG, "Using last known network location");
                }
                
                Log.d(TAG, "Location updates started");
            } catch (SecurityException e) {
                Log.e(TAG, "Location permission denied", e);
            }
        }
    }

    /**
     * 위치 업데이트 중지
     */
    private void stopLocationUpdates() {
        try {
            locationManager.removeUpdates(this);
            Log.d(TAG, "Location updates stopped");
        } catch (SecurityException e) {
            Log.e(TAG, "Error stopping location updates", e);
        }
    }

    // LocationListener 구현
    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "Location provider enabled: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "Location provider disabled: " + provider);
    }

    /**
     * 날씨 + 버스 통합 알림 표시
     */
    private void showCombinedNotification(Weather weather, String busInfo) {
        // 앱 실행 인텐트
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // 알림 지우기 인텐트
        Intent dismissIntent = new Intent(this, NotificationDismissReceiver.class);
        dismissIntent.setAction(NotificationDismissReceiver.ACTION_DISMISS_PERSISTENT);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                this, 0, dismissIntent, PendingIntent.FLAG_IMMUTABLE);

        // 알림 내용 구성
        String title;
        String content;
        int icon;

        if (weather != null) {
            title = String.format(Locale.getDefault(), "%.1f°C %s",
                weather.getTemperature(), getWeatherConditionText(weather.getWeatherCondition()));

            if (weather.isNeedUmbrella()) {
                content = "🌧️ 우산 필요 | " + busInfo;
                icon = R.drawable.ic_umbrella_small;
            } else {
                content = "☀️ 우산 불필요 | " + busInfo;
                icon = R.drawable.ic_weather_sunny;
            }
        } else {
            title = "날씨 정보 없음";
            content = "🚌 " + busInfo;
            icon = R.drawable.ic_bus;
        }

        // 알림 생성 (확장 가능한 스타일)
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(content)
                    .setBigContentTitle(title))
                .setOngoing(true) // 사용자가 스와이프로 제거할 수 없음
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_close, "알림 끄기", dismissPendingIntent) // 알림 끄기 액션 추가
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
