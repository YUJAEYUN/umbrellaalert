package com.example.umbrellaalert.ui.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.umbrellaalert.R;
import com.example.umbrellaalert.data.manager.WeatherManager;
import com.example.umbrellaalert.data.model.Weather;

import java.util.Locale;
import com.example.umbrellaalert.ui.home.HomeActivity;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherWidget extends AppWidgetProvider {
    private static final String TAG = "WeatherWidget";

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // 모든 위젯 업데이트
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // 위젯 레이아웃 가져오기
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget);

        // 위젯 클릭 시 앱 실행 인텐트
        Intent intent = new Intent(context, com.example.umbrellaalert.ui.main.MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);

        // 초기 상태 설정
        views.setTextViewText(R.id.widget_temperature, "로딩 중...");
        views.setTextViewText(R.id.widget_condition, "");
        views.setTextViewText(R.id.widget_umbrella_text, "날씨 정보를 가져오는 중입니다");

        // 앱 위젯 업데이트
        appWidgetManager.updateAppWidget(appWidgetId, views);

        // 날씨 데이터 비동기 로드
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    WeatherManager weatherManager = WeatherManager.getInstance(context);

                    // 마지막 알려진 위치 또는 기본 위치(서울) 사용
                    weatherManager.getCurrentWeather(37.5665, 126.9780, new WeatherManager.WeatherCallback() {
                        @Override
                        public void onSuccess(Weather weather) {
                            if (weather != null) {
                                updateWidgetWithWeather(context, appWidgetManager, appWidgetId, weather);
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Failed to get weather for widget: " + error);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void updateWidgetWithWeather(Context context, AppWidgetManager appWidgetManager,
                                                int appWidgetId, Weather weather) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_weather);

        // 날씨 정보 업데이트
        views.setTextViewText(R.id.widget_temperature,
                String.format(Locale.getDefault(), "%.1f°C", weather.getTemperature()));

        // 온도 및 날씨 상태 업데이트
        views.setTextViewText(R.id.widget_temperature,
            String.format(Locale.getDefault(), "%.1f°C", weather.getTemperature()));
        views.setTextViewText(R.id.widget_condition, getWeatherConditionText(weather.getWeatherCondition()));

        // 우산 필요 여부에 따라 메시지와 아이콘 변경
        if (weather.isNeedUmbrella()) {
            views.setTextViewText(R.id.widget_umbrella_text, "우산이 필요해요!");
            views.setImageViewResource(R.id.widget_icon, R.drawable.ic_umbrella_small);
        } else {
            views.setTextViewText(R.id.widget_umbrella_text, "우산이 필요 없어요");
            views.setImageViewResource(R.id.widget_icon, R.drawable.ic_weather_sunny);
        }

        // 위젯 업데이트
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /**
     * 날씨 상태 텍스트 변환
     */
    private static String getWeatherConditionText(String condition) {
        if (condition == null) return "알 수 없음";

        switch (condition.toLowerCase()) {
            case "clear":
            case "sunny":
                return "맑음";
            case "partly_cloudy":
                return "구름 조금";
            case "cloudy":
                return "흐림";
            case "rain":
                return "비";
            case "snow":
                return "눈";
            case "thunderstorm":
                return "천둥번개";
            default:
                return condition;
        }
    }

    @Override
    public void onEnabled(Context context) {
        // 위젯이 처음 추가될 때 호출
    }

    @Override
    public void onDisabled(Context context) {
        // 마지막 위젯이 제거될 때 호출
    }
}