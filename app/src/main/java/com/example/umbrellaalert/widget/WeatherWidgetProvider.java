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
import com.example.umbrellaalert.service.LocationService;
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

        if (!isWidgetEnabled) {
            // 비활성화되어 있어도 기본 메시지는 표시
            for (int appWidgetId : appWidgetIds) {
                showDisabledWidget(context, appWidgetManager, appWidgetId);
            }
            return;
        }

        // 각 위젯 ID에 대해 업데이트 수행
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
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

                // 현재 위치로 날씨 데이터 가져오기
                WeatherManager weatherManager = WeatherManager.getInstance(context);
                final Location finalLocation = currentLocation;

                weatherManager.getCurrentWeather(finalLocation.getLatitude(), finalLocation.getLongitude(), new WeatherManager.WeatherCallback() {
                    @Override
                    public void onSuccess(Weather weather) {
                        Log.d(TAG, "날씨 데이터 성공적으로 가져옴");
                        if (weather != null) {
                            // 위젯 UI 업데이트
                            views.setTextViewText(R.id.widget_temperature,
                                    String.format(Locale.getDefault(), "%.1f°C", weather.getTemperature()));
                            views.setTextViewText(R.id.widget_condition, getWeatherConditionText(weather.getWeatherCondition()));

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

                        // 위젯 업데이트
                        appWidgetManager.updateAppWidget(appWidgetId, views);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "날씨 데이터 가져오기 실패: " + error);
                        // 오류 발생 시 위젯 업데이트
                        views.setTextViewText(R.id.widget_temperature, "--°C");
                        views.setTextViewText(R.id.widget_condition, "오류 발생");
                        views.setTextViewText(R.id.widget_umbrella_text, "날씨 정보를 가져오는 중 오류가 발생했습니다");
                        appWidgetManager.updateAppWidget(appWidgetId, views);
                    }
                });
            } else {
                Log.w(TAG, "위치 정보를 가져올 수 없습니다");
                // 위치 정보가 없는 경우 - 기본 위치(세종시) 사용
                WeatherManager weatherManager = WeatherManager.getInstance(context);
                weatherManager.getCurrentWeather(36.4800, 127.2890, new WeatherManager.WeatherCallback() {
                    @Override
                    public void onSuccess(Weather weather) {
                        if (weather != null) {
                            views.setTextViewText(R.id.widget_temperature,
                                    String.format(Locale.getDefault(), "%.1f°C", weather.getTemperature()));
                            views.setTextViewText(R.id.widget_condition, getWeatherConditionText(weather.getWeatherCondition()) + " (세종시)");

                            if (weather.isNeedUmbrella()) {
                                views.setImageViewResource(R.id.widget_icon, R.drawable.ic_umbrella_small);
                                views.setTextViewText(R.id.widget_umbrella_text, "우산이 필요해요!");
                            } else {
                                views.setImageViewResource(R.id.widget_icon, R.drawable.ic_weather_sunny);
                                views.setTextViewText(R.id.widget_umbrella_text, "우산이 필요 없어요");
                            }
                        } else {
                            views.setTextViewText(R.id.widget_temperature, "--°C");
                            views.setTextViewText(R.id.widget_condition, "날씨 정보 없음");
                            views.setTextViewText(R.id.widget_umbrella_text, "날씨 정보를 확인할 수 없습니다");
                        }
                        appWidgetManager.updateAppWidget(appWidgetId, views);
                    }

                    @Override
                    public void onError(String error) {
                        views.setTextViewText(R.id.widget_temperature, "--°C");
                        views.setTextViewText(R.id.widget_condition, "위치 정보 없음");
                        views.setTextViewText(R.id.widget_umbrella_text, "현재 위치를 확인할 수 없습니다");
                        appWidgetManager.updateAppWidget(appWidgetId, views);
                    }
                });
            }
        } else {
            // 위치 권한이 없는 경우
            views.setTextViewText(R.id.widget_temperature, "--°C");
            views.setTextViewText(R.id.widget_condition, "권한 필요");
            views.setTextViewText(R.id.widget_umbrella_text, "위치 권한이 필요합니다");
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
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
