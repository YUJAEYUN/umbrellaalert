package com.example.umbrellaalert.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.umbrellaalert.R;
import com.example.umbrellaalert.UmbrellaApplication;
import com.example.umbrellaalert.data.api.BusApiClient;
import com.example.umbrellaalert.data.database.AppDatabase;
import com.example.umbrellaalert.data.database.BusDao;
import com.example.umbrellaalert.data.model.BusArrival;
import com.example.umbrellaalert.data.model.RegisteredBus;
import com.example.umbrellaalert.service.LocationService;
import com.example.umbrellaalert.ui.home.HomeActivity;
import com.example.umbrellaalert.ui.settings.SettingsViewModel;
import com.example.umbrellaalert.util.WalkingTimeCalculator;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 버스 알림 서비스
 * 등록된 버스의 도착 시간을 모니터링하고 도보 시간을 고려한 알림 발송
 */
public class BusNotificationService extends Service {
    
    private static final String TAG = "BusNotificationService";
    private static final String CHANNEL_ID = "bus_notification_channel";
    private static final int NOTIFICATION_ID = 2000;
    private static final long CHECK_INTERVAL = 30 * 1000; // 30초마다 체크
    
    private static final String PREF_NAME = "bus_notification_prefs";
    private static final String KEY_ENABLED = "bus_notification_enabled";
    
    private Handler handler;
    private Runnable checkRunnable;
    private ExecutorService executorService;
    private LocationService locationService;
    private BusApiClient busApiClient;
    private WalkingTimeCalculator walkingTimeCalculator;
    private BusDao busDao;
    private Location currentLocation;
    
    public static void setEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
        
        Intent intent = new Intent(context, BusNotificationService.class);
        if (enabled) {
            context.startForegroundService(intent);
        } else {
            context.stopService(intent);
        }
    }
    
    public static boolean isEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ENABLED, false);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        handler = new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();
        locationService = LocationService.getInstance(this);
        busApiClient = new BusApiClient(this);
        walkingTimeCalculator = new WalkingTimeCalculator(this);
        busDao = AppDatabase.getInstance(this).busDao();
        
        // 알림 채널 생성
        createNotificationChannel();
        
        // 위치 업데이트 시작
        locationService.startLocationUpdates(new LocationService.LocationCallback() {
            @Override
            public void onLocationUpdate(Location location) {
                currentLocation = location;
            }
        });
        
        // 버스 체크 시작
        startBusChecking();
        
        Log.d(TAG, "버스 알림 서비스 시작됨");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 포그라운드 서비스로 실행
        Notification notification = createForegroundNotification();
        startForeground(NOTIFICATION_ID, notification);
        
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "버스 알림",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("버스 도착 알림");
        
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
    
    private Notification createForegroundNotification() {
        Intent intent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("버스 알림 서비스")
                .setContentText("등록된 버스를 모니터링 중입니다")
                .setSmallIcon(R.drawable.ic_bus)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }
    
    private void startBusChecking() {
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                checkRegisteredBuses();
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
        
        handler.post(checkRunnable);
    }
    
    private void checkRegisteredBuses() {
        // 설정된 시간이 지났는지 확인
        if (SettingsViewModel.shouldStopNotifications(this)) {
            Log.d(TAG, "설정된 종료 시간이 지나 버스 알림 서비스 중단");
            stopSelf(); // 서비스 종료
            return;
        }

        if (currentLocation == null) {
            Log.w(TAG, "현재 위치를 알 수 없어 버스 체크를 건너뜁니다");
            return;
        }
        
        executorService.execute(() -> {
            try {
                List<RegisteredBus> registeredBuses = busDao.getAllRegisteredBuses();
                
                for (RegisteredBus bus : registeredBuses) {
                    checkBusArrival(bus);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "등록된 버스 체크 실패", e);
            }
        });
    }
    
    private void checkBusArrival(RegisteredBus bus) {
        try {
            // 1. 버스 도착 정보 가져오기
            Future<List<BusArrival>> future = busApiClient.getBusArrivalInfo(
                    bus.getNodeId(), bus.getCityCode());
            List<BusArrival> arrivals = future.get();
            
            // 2. 해당 노선 찾기
            BusArrival targetArrival = null;
            for (BusArrival arrival : arrivals) {
                if (bus.getRouteId().equals(arrival.getRouteId()) || 
                    bus.getRouteNo().equals(arrival.getRouteNo())) {
                    targetArrival = arrival;
                    break;
                }
            }
            
            if (targetArrival == null) {
                Log.d(TAG, "버스 도착 정보를 찾을 수 없음: " + bus.getRouteNo());
                return;
            }
            
            // 3. 정류장 위치 정보 사용
            double busStopLat = bus.getLatitude();
            double busStopLng = bus.getLongitude();

            // 위치 정보가 없는 경우 (기존 데이터) 건너뛰기
            if (busStopLat == 0.0 && busStopLng == 0.0) {
                Log.w(TAG, "정류장 위치 정보가 없어 알림을 건너뜁니다: " + bus.getRouteNo());
                return;
            }
            
            // 4. 도보 시간 계산
            Future<Integer> walkingTimeFuture = walkingTimeCalculator.calculateWalkingTime(
                    currentLocation.getLatitude(), currentLocation.getLongitude(),
                    busStopLat, busStopLng);
            int walkingTimeMinutes = walkingTimeFuture.get();
            
            // 5. 버스 도착 시간 (이미 분 단위)
            int busArrivalMinutes = targetArrival.getArrTime();
            
            // 6. 알림 조건 체크 (버스 도착 시간 - 도보 시간 <= 2분)
            int timeDifference = busArrivalMinutes - walkingTimeMinutes;
            
            Log.d(TAG, String.format("버스 %s: 도착 %d분, 도보 %d분, 차이 %d분", 
                    bus.getRouteNo(), busArrivalMinutes, walkingTimeMinutes, timeDifference));
            
            if (timeDifference <= 2 && timeDifference >= 0) {
                sendBusNotification(bus, busArrivalMinutes, walkingTimeMinutes);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "버스 도착 체크 실패: " + bus.getRouteNo(), e);
        }
    }
    
    private void sendBusNotification(RegisteredBus bus, int arrivalMinutes, int walkingMinutes) {
        Intent intent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        String title = "🏃‍♂️ 지금 출발!";
        String message = String.format("%s번 %d분 후 도착 (도보 %d분)",
                bus.getRouteNo(), arrivalMinutes, walkingMinutes);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setSmallIcon(R.drawable.ic_bus)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(NOTIFICATION_ID + bus.getRouteNo().hashCode(), notification);

        Log.d(TAG, "버스 알림 발송: " + message);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        if (handler != null && checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
        }
        
        if (locationService != null) {
            locationService.stopLocationUpdates();
        }
        
        if (walkingTimeCalculator != null) {
            walkingTimeCalculator.shutdown();
        }
        
        if (executorService != null) {
            executorService.shutdown();
        }
        
        Log.d(TAG, "버스 알림 서비스 종료됨");
    }
}
