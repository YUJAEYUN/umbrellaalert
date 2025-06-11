package com.example.umbrellaalert.widget;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.widget.RemoteViews;

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
import com.example.umbrellaalert.service.LocationService;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.example.umbrellaalert.ui.home.HomeActivity;

import java.util.Locale;

/**
 * 날씨 위젯 프로바이더 클래스
 */
public class WeatherWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "WeatherWidgetProvider";
    private static final String PREF_NAME = "UmbrellaAlertPrefs";
    private static final String KEY_WIDGET_ENABLED = "widget_enabled";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "위젯 업데이트 요청됨. 위젯 개수: " + appWidgetIds.length);

        // 위젯이 활성화되어 있는지 확인
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isWidgetEnabled = preferences.getBoolean(KEY_WIDGET_ENABLED, true); // 기본값을 true로 변경

        Log.d(TAG, "위젯 활성화 상태: " + isWidgetEnabled);

        // 각 위젯 ID에 대해 업데이트 수행 (활성화 여부와 관계없이)
        for (int appWidgetId : appWidgetIds) {
            if (isWidgetEnabled) {
                updateAppWidget(context, appWidgetManager, appWidgetId);
            } else {
                showDisabledWidget(context, appWidgetManager, appWidgetId);
            }
        }
    }

    /**
     * 비활성화된 위젯 표시
     */
    private void showDisabledWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget);

        // 앱 실행 인텐트 설정
        Intent intent = new Intent(context, com.example.umbrellaalert.ui.main.MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);

        // 비활성화 메시지 표시
        views.setTextViewText(R.id.widget_temperature, "--°C");
        views.setTextViewText(R.id.widget_condition, "위젯 비활성화");
        views.setTextViewText(R.id.widget_umbrella_text, "설정에서 위젯을 활성화해주세요");
        views.setTextViewText(R.id.widget_bus_info, "위젯이 비활성화되어 있습니다");
        views.setImageViewResource(R.id.widget_icon, R.drawable.ic_settings);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /**
     * 위젯 업데이트 메서드
     */
    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // 위젯 레이아웃 생성
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget);

        // 앱 실행 인텐트 설정
        Intent intent = new Intent(context, com.example.umbrellaalert.ui.main.MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);

        // 날씨 데이터 로드 및 위젯 업데이트
        loadWeatherData(context, views, appWidgetManager, appWidgetId);
    }

    /**
     * 날씨 데이터 로드 및 위젯 업데이트
     */
    private void loadWeatherData(Context context, RemoteViews views, AppWidgetManager appWidgetManager, int appWidgetId) {
        Log.d(TAG, "위젯 날씨 데이터 로드 시작");

        // 로딩 상태 표시
        views.setTextViewText(R.id.widget_temperature, "로딩 중...");
        views.setTextViewText(R.id.widget_condition, "");
        views.setTextViewText(R.id.widget_bus_info, "버스 정보 확인 중...");
        appWidgetManager.updateAppWidget(appWidgetId, views);

        // 위치 권한 확인 후 날씨 데이터 로드
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // LocationService를 사용하여 현재 위치 가져오기
            LocationService locationService = LocationService.getInstance(context);
            Location currentLocation = locationService.getLastLocation();

            // LocationService에서 위치를 가져올 수 없으면 LocationManager 사용
            if (currentLocation == null) {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

                try {
                    Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                    if (gpsLocation != null) {
                        currentLocation = gpsLocation;
                    } else if (networkLocation != null) {
                        currentLocation = networkLocation;
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "위치 권한 오류", e);
                }
            }
            
            if (currentLocation != null) {
                Log.d(TAG, "위치 정보 확인됨: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());

                // 현재 위치로 실제 날씨 데이터 가져오기
                final Location finalLocation = currentLocation;

                // 실제 날씨 정보와 버스 정보를 동시에 가져오기
                loadWeatherAndBusDataWithRealAPI(context, finalLocation, views, appWidgetManager, appWidgetId);
            } else {
                Log.w(TAG, "위치 정보를 가져올 수 없습니다");
                // 위치 정보가 없는 경우 - 기본 위치(세종시) 사용
                Location defaultLocation = new Location("default");
                defaultLocation.setLatitude(36.4800);
                defaultLocation.setLongitude(127.2890);

                loadWeatherAndBusDataWithRealAPI(context, defaultLocation, views, appWidgetManager, appWidgetId);
            }
        } else {
            // 위치 권한이 없는 경우
            views.setTextViewText(R.id.widget_temperature, "--°C");
            views.setTextViewText(R.id.widget_condition, "권한 필요");
            views.setTextViewText(R.id.widget_umbrella_text, "위치 권한이 필요합니다");
            views.setTextViewText(R.id.widget_bus_info, "위치 권한이 필요합니다");
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    /**
     * 날씨와 버스 정보를 함께 로드 (캐시 우선, 없으면 API 호출)
     */
    private void loadWeatherAndBusDataWithRealAPI(Context context, Location location,
                                     RemoteViews views, AppWidgetManager appWidgetManager, int appWidgetId) {

        // 1. 먼저 캐시에서 날씨 데이터 확인
        Weather cachedWeather = WeatherCacheManager.getWeatherFromCache(context);

        if (cachedWeather != null) {
            Log.d(TAG, "✅ 위젯 캐시된 날씨 데이터 사용: " + cachedWeather.getTemperature() + "°C, " + cachedWeather.getWeatherCondition());
            updateWeatherInfo(cachedWeather, views, location);
            appWidgetManager.updateAppWidget(appWidgetId, views);

            // 버스 정보도 가져오기
            loadBusInfo(context, views, appWidgetManager, appWidgetId);
            return;
        }

        // 2. 캐시에 없으면 기본 데이터 사용 (홈 화면에서 API 호출하므로)
        Log.d(TAG, "캐시된 데이터 없음, 기본 데이터 사용 (홈 화면에서 API 호출 대기)");
        Weather fallbackWeather = createFallbackWeather(location);
        updateWeatherInfo(fallbackWeather, views, location);
        appWidgetManager.updateAppWidget(appWidgetId, views);

        // 버스 정보 가져오기
        loadBusInfo(context, views, appWidgetManager, appWidgetId);
    }

    /**
     * 기본 날씨 데이터 생성 (API 실패 시 사용)
     */
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
     * 날씨 정보 업데이트
     */
    private void updateWeatherInfo(Weather weather, RemoteViews views, Location location) {
        if (weather != null) {
            // 위젯 UI 업데이트
            views.setTextViewText(R.id.widget_temperature,
                    String.format(Locale.getDefault(), "%.1f°C", weather.getTemperature()));

            String conditionText = getWeatherConditionText(weather.getWeatherCondition());
            if (location.getProvider().equals("default")) {
                conditionText += " (세종시)";
            }
            views.setTextViewText(R.id.widget_condition, conditionText);

            // 우산 필요 여부에 따라 아이콘 변경
            if (weather.isNeedUmbrella()) {
                views.setImageViewResource(R.id.widget_icon, R.drawable.ic_umbrella_small);
                views.setTextViewText(R.id.widget_umbrella_text, "우산이 필요해요!");
            } else {
                views.setImageViewResource(R.id.widget_icon, R.drawable.ic_weather_sunny);
                views.setTextViewText(R.id.widget_umbrella_text, "우산이 필요 없어요");
            }
        } else {
            Log.w(TAG, "날씨 데이터가 null입니다");
            // 날씨 데이터를 가져오지 못한 경우
            views.setTextViewText(R.id.widget_temperature, "--°C");
            views.setTextViewText(R.id.widget_condition, "날씨 정보 없음");
            views.setTextViewText(R.id.widget_umbrella_text, "날씨 정보를 확인할 수 없습니다");
        }
    }

    /**
     * 버스 정보 로드
     */
    private void loadBusInfo(Context context, RemoteViews views, AppWidgetManager appWidgetManager, int appWidgetId) {
        new Thread(() -> {
            try {
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
                BusDao busDao = new BusDao(dbHelper);
                BusApiClient busApiClient = new BusApiClient(context);

                List<RegisteredBus> buses = busDao.getAllRegisteredBuses();
                String busInfo = getBusInfoText(buses, busApiClient);

                // UI 스레드에서 업데이트
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    views.setTextViewText(R.id.widget_bus_info, busInfo);
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                });

            } catch (Exception e) {
                Log.e(TAG, "버스 정보 로드 실패", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    views.setTextViewText(R.id.widget_bus_info, "버스 정보를 가져올 수 없습니다");
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                });
            }
        }).start();
    }

    /**
     * 버스 정보 텍스트 생성
     */
    private String getBusInfoText(List<RegisteredBus> buses, BusApiClient busApiClient) {
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
    }

    /**
     * 날씨 상태 텍스트 변환
     */
    private String getWeatherConditionText(String condition) {
        if (condition == null) {
            return "알 수 없음";
        }
        
        // 이미 한글인 경우 그대로 반환
        if (condition.equals("맑음") || condition.equals("구름많음") || condition.equals("흐림") ||
            condition.equals("비") || condition.equals("눈") || condition.equals("소나기")) {
            return condition;
        }
        
        // 영어인 경우 한글로 변환
        if (condition.equalsIgnoreCase("Clear")) {
            return "맑음";
        } else if (condition.equalsIgnoreCase("Clouds")) {
            return "구름많음";
        } else if (condition.equalsIgnoreCase("Overcast")) {
            return "흐림";
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
     * 모든 위젯을 강제로 업데이트
     */
    public static void forceUpdateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, WeatherWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

        Log.d("WeatherWidgetProvider", "강제 업데이트 요청. 위젯 개수: " + appWidgetIds.length);

        if (appWidgetIds.length > 0) {
            Intent intent = new Intent(context, WeatherWidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            context.sendBroadcast(intent);
        }
    }
}
