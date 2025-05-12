package com.example.umbrellaalert.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.umbrellaalert.service.WeatherUpdateService;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received, updating weather");

        // 날씨 업데이트 서비스 시작 (이미 실행 중이면 업데이트만 트리거)
        WeatherUpdateService.startService(context);
    }
}