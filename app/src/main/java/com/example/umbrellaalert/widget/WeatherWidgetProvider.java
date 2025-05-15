package com.example.umbrellaalert.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import com.example.umbrellaalert.R;
import com.example.umbrellaalert.data.manager.WeatherManager;
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.ui.home.HomeActivity;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 날씨 위젯 프로바이더 클래스
 */
public class WeatherWidgetProvider extends AppWidgetProvider {

    private static final String PREF_NAME = "UmbrellaAlertPrefs";
    private static final String KEY_WIDGET_ENABLED = "widget_enabled";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // 위젯이 활성화되어 있는지 확인
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isWidgetEnabled = preferences.getBoolean(KEY_WIDGET_ENABLED, false);

        if (!isWidgetEnabled) {
            return; // 위젯이 비활성화되어 있으면 업데이트하지 않음
        }

        // 각 위젯 ID에 대해 업데이트 수행
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    /**
     * 위젯 업데이트 메서드
     */
    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // 위젯 레이아웃 생성
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget);

        // 앱 실행 인텐트 설정
        Intent intent = new Intent(context, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);

        // 날씨 데이터 로드 및 위젯 업데이트
        loadWeatherData(context, views, appWidgetManager, appWidgetId);
    }

    /**
     * 날씨 데이터 로드 및 위젯 업데이트
     */
    private void loadWeatherData(Context context, RemoteViews views, AppWidgetManager appWidgetManager, int appWidgetId) {
        // 로딩 상태 표시
        views.setTextViewText(R.id.widget_temperature, "로딩 중...");
        views.setTextViewText(R.id.widget_condition, "");
        appWidgetManager.updateAppWidget(appWidgetId, views);

        // 날씨 데이터 비동기 로드
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                // 날씨 데이터 가져오기
                WeatherManager weatherManager = WeatherManager.getInstance(context);
                // 서울 좌표 (위도, 경도) 기본값 사용
                Weather weather = weatherManager.getCurrentWeather(37.5665, 126.9780);

                if (weather != null) {
                    // 위젯 UI 업데이트
                    views.setTextViewText(R.id.widget_temperature,
                            String.format(Locale.getDefault(), "%.1f°C", weather.getTemperature()));
                    views.setTextViewText(R.id.widget_condition, getWeatherConditionText(weather.getWeatherCondition()));

                    // 우산 필요 여부에 따라 아이콘 변경
                    if (weather.isNeedUmbrella()) {
                        views.setImageViewResource(R.id.widget_icon, R.drawable.ic_umbrella);
                        views.setTextViewText(R.id.widget_umbrella_text, "우산이 필요해요!");
                    } else {
                        views.setImageViewResource(R.id.widget_icon, R.drawable.ic_weather_sunny);
                        views.setTextViewText(R.id.widget_umbrella_text, "우산이 필요 없어요");
                    }
                } else {
                    // 날씨 데이터를 가져오지 못한 경우
                    views.setTextViewText(R.id.widget_temperature, "--°C");
                    views.setTextViewText(R.id.widget_condition, "날씨 정보 없음");
                    views.setTextViewText(R.id.widget_umbrella_text, "날씨 정보를 확인할 수 없습니다");
                }

                // 위젯 업데이트
                appWidgetManager.updateAppWidget(appWidgetId, views);
            } catch (Exception e) {
                e.printStackTrace();
                // 오류 발생 시 위젯 업데이트
                views.setTextViewText(R.id.widget_temperature, "--°C");
                views.setTextViewText(R.id.widget_condition, "오류 발생");
                views.setTextViewText(R.id.widget_umbrella_text, "날씨 정보를 가져오는 중 오류가 발생했습니다");
                appWidgetManager.updateAppWidget(appWidgetId, views);
            } finally {
                executorService.shutdown();
            }
        });
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
}
