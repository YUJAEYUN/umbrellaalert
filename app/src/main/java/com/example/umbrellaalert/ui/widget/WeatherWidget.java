package com.example.umbrellaalert.ui.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.widget.RemoteViews;

import com.example.umbrellaalert.R;
import com.example.umbrellaalert.data.manager.WeatherManager;
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.ui.home.HomeActivity;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherWidget extends AppWidgetProvider {

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
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_weather);

        // 위젯 클릭 시 앱 실행 인텐트
        Intent intent = new Intent(context, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);

        // 초기 상태 설정
        views.setTextViewText(R.id.widget_message, "날씨 정보를 가져오는 중이다냥~");

        // 앱 위젯 업데이트
        appWidgetManager.updateAppWidget(appWidgetId, views);

        // 날씨 데이터 비동기 로드
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    WeatherManager weatherManager = WeatherManager.getInstance(context);

                    // 마지막 알려진 위치 또는 기본 위치(서울) 사용
                    Weather weather = weatherManager.getCurrentWeather(37.5665, 126.9780); // 기본 서울 위치

                    if (weather != null) {
                        updateWidgetWithWeather(context, appWidgetManager, appWidgetId, weather);
                    }
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

        // 메시지 및 배경색 업데이트 (iOS 스타일)
        if (weather.isNeedUmbrella()) {
            views.setTextViewText(R.id.widget_message, "우산이 필요하다냥!");
            views.setInt(R.id.widget_container, "setBackgroundResource", R.drawable.widget_bg_rainy);
            views.setImageViewResource(R.id.widget_icon, R.drawable.ic_umbrella);
            // RemoteViews에서는 setShadowLayer 직접 호출 불가능
            // 그림자 효과는 XML에서 정의해야 함
        } else {
            views.setTextViewText(R.id.widget_message, "오늘은 맑은 하루다냥~");
            views.setInt(R.id.widget_container, "setBackgroundResource", R.drawable.widget_bg_sunny);
            views.setImageViewResource(R.id.widget_icon, R.drawable.ic_weather_sunny);
            // RemoteViews에서는 setShadowLayer 직접 호출 불가능
            // 그림자 효과는 XML에서 정의해야 함
        }

        // 위젯 업데이트
        appWidgetManager.updateAppWidget(appWidgetId, views);
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