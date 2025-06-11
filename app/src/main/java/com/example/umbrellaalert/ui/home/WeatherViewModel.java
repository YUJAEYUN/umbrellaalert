package com.example.umbrellaalert.ui.home;

import android.app.Application;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.umbrellaalert.R;
import com.example.umbrellaalert.data.model.HourlyForecast;
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.domain.usecase.Get12HourForecastUseCase;
import com.example.umbrellaalert.domain.usecase.GetCurrentWeatherUseCase;
import com.example.umbrellaalert.domain.usecase.GetCatMessageUseCase;
import com.example.umbrellaalert.util.WeatherCacheManager;
import com.example.umbrellaalert.service.CatWeatherAnalystService;
import com.example.umbrellaalert.service.MockWeatherForecastService;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * 날씨 정보를 관리하는 ViewModel (MVVM + Clean Architecture)
 * UseCase를 통해 비즈니스 로직을 처리하고 UI 상태를 관리
 */
@HiltViewModel
public class WeatherViewModel extends AndroidViewModel {

    private static final String TAG = "WeatherViewModel";

    // 서울 기본 좌표
    private static final double DEFAULT_LATITUDE = 37.5665;
    private static final double DEFAULT_LONGITUDE = 126.9780;

    // UseCase 의존성
    private final GetCurrentWeatherUseCase getCurrentWeatherUseCase;
    private final GetCatMessageUseCase getCatMessageUseCase;
    private final Get12HourForecastUseCase get12HourForecastUseCase;
    private final ExecutorService executorService;

    // LiveData
    private final MutableLiveData<Weather> weatherData = new MutableLiveData<>();
    private final MutableLiveData<String> locationName = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private final MutableLiveData<Integer> backgroundResource = new MutableLiveData<>();
    private final MutableLiveData<Integer> catImageResource = new MutableLiveData<>();
    private final MutableLiveData<String> catMessage = new MutableLiveData<>();
    private final MutableLiveData<String> umbrellaMessage = new MutableLiveData<>();
    private final MutableLiveData<List<HourlyForecast>> hourlyForecastData = new MutableLiveData<>();
    private final MutableLiveData<String> forecastUpdateTime = new MutableLiveData<>();
    private final MutableLiveData<String> temperatureMessage = new MutableLiveData<>();

    @Inject
    public WeatherViewModel(@NonNull Application application,
                           GetCurrentWeatherUseCase getCurrentWeatherUseCase,
                           GetCatMessageUseCase getCatMessageUseCase,
                           Get12HourForecastUseCase get12HourForecastUseCase) {
        super(application);
        this.getCurrentWeatherUseCase = getCurrentWeatherUseCase;
        this.getCatMessageUseCase = getCatMessageUseCase;
        this.get12HourForecastUseCase = get12HourForecastUseCase;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    // 위치 기반 날씨 업데이트 (0.5초 로딩 텀 추가)
    public void updateWeatherWithLocation(Location location) {
        isLoading.setValue(true);

        executorService.execute(() -> {
            try {
                // 로딩 텀 제거 - 바로 데이터 생성

                // UseCase를 통해 현재 날씨 가져오기 (캐싱된 데이터 우선 사용)
                Weather weather = getCurrentWeatherUseCase.execute(
                        location.getLatitude(), location.getLongitude());

                if (weather != null && weather.getTemperature() > -50 && weather.getTemperature() < 60) {
                    // 유효한 온도 범위의 실제 데이터인 경우
                    Log.d(TAG, "🌡️ WeatherViewModel에서 받은 유효한 날씨 데이터: " + weather.getTemperature() + "°C, 상태: " + weather.getWeatherCondition());

                    // 캐시에 저장 (위젯과 알림에서 재사용)
                    WeatherCacheManager.saveWeatherToCache(getApplication(), weather);

                    weatherData.postValue(weather);
                    updateWeatherUI(weather);

                    // 실제 API 데이터를 받았으므로 예보 데이터도 가져오기
                    List<HourlyForecast> hourlyForecasts = get12HourForecastUseCase.execute(
                            location.getLatitude(), location.getLongitude());

                    // 6시간만 표시하도록 제한
                    if (hourlyForecasts != null && hourlyForecasts.size() > 6) {
                        hourlyForecasts = hourlyForecasts.subList(0, 6);
                    }

                    if (hourlyForecasts != null && !hourlyForecasts.isEmpty()) {
                        Log.d(TAG, "📊 WeatherViewModel에서 받은 6시간 예보 데이터:");
                        for (int i = 0; i < Math.min(3, hourlyForecasts.size()); i++) {
                            HourlyForecast forecast = hourlyForecasts.get(i);
                            Log.d(TAG, "  " + (i + 1) + "시간 후: " + forecast.getTemperature() + "°C, 시간: " + forecast.getForecastTime());
                        }

                        hourlyForecastData.postValue(hourlyForecasts);

                        // 예보 업데이트 시간 설정
                        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.KOREA);
                        String updateTime = "업데이트: " + timeFormat.format(new java.util.Date());
                        forecastUpdateTime.postValue(updateTime);
                    }

                } else {
                    Log.w(TAG, "⚠️ 날씨 정보를 가져올 수 없어서 기본값 사용");
                    // 날씨 정보를 가져올 수 없는 경우 기본값 사용
                    Weather defaultWeather = createDefaultWeather(location);
                    weatherData.postValue(defaultWeather);
                    updateWeatherUI(defaultWeather);
                }



            } catch (Exception e) {
                Log.e(TAG, "날씨 정보 업데이트 실패", e);
                // 오류 발생 시 기본 날씨 정보 사용
                Weather defaultWeather = createDefaultWeather(location);
                weatherData.postValue(defaultWeather);
                updateWeatherUI(defaultWeather);
            } finally {
                isLoading.postValue(false);
            }
        });

        // 위치명 업데이트
        updateLocationName(location);
    }

    // 기본 위치(서울) 사용 (0.5초 로딩 텀 추가)
    public void updateWeatherWithDefaultLocation() {
        isLoading.setValue(true);

        executorService.execute(() -> {
            try {
                // 로딩 텀 제거 - 바로 데이터 생성

                // 기본 위치 대신 위치 권한 요청 유도
                locationName.postValue("위치 권한이 필요합니다");

                // 기본 날씨 정보 생성
                android.location.Location defaultLocation = new android.location.Location("default");
                defaultLocation.setLatitude(DEFAULT_LATITUDE);
                defaultLocation.setLongitude(DEFAULT_LONGITUDE);

                Weather defaultWeather = createDefaultWeather(defaultLocation);
                weatherData.postValue(defaultWeather);
                updateWeatherUI(defaultWeather);

                // 기본 6시간 예보 데이터 생성
                List<HourlyForecast> defaultForecasts = get12HourForecastUseCase.execute(
                        DEFAULT_LATITUDE, DEFAULT_LONGITUDE);

                // 6시간만 표시하도록 제한
                if (defaultForecasts != null && defaultForecasts.size() > 6) {
                    defaultForecasts = defaultForecasts.subList(0, 6);
                }
                hourlyForecastData.postValue(defaultForecasts);

                // 기본 업데이트 시간 설정
                java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.KOREA);
                String updateTime = "업데이트: " + timeFormat.format(new java.util.Date());
                forecastUpdateTime.postValue(updateTime);

            } catch (Exception e) {
                Log.e(TAG, "날씨 데이터 로딩 실패", e);
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    // 위치명 업데이트 (지오코딩)
    private void updateLocationName(Location location) {
        executorService.execute(() -> {
            String name = getLocationName(location);
            locationName.postValue(name);
        });
    }

    // 위도/경도로부터 위치명 가져오기
    private String getLocationName(Location location) {
        Geocoder geocoder = new Geocoder(getApplication(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // 시/도 및 구/군 정보 가져오기
                String city = address.getAdminArea(); // 시/도
                String district = address.getLocality(); // 구/군

                if (city != null && district != null) {
                    return city + " " + district;
                } else if (city != null) {
                    return city;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "지오코딩 오류", e);
        }

        return "알 수 없는 위치";
    }

    // UI 업데이트 (개선된 고양이 메시지 시스템)
    private void updateWeatherUI(Weather weather) {
        // UseCase를 통해 고양이 메시지 객체 생성
        com.example.umbrellaalert.data.model.CatMessage catMessageObj =
            getCatMessageUseCase.getCatMessageObject(weather);

        // 고양이 이미지 업데이트 (메시지 객체에서 가져옴)
        catImageResource.postValue(catMessageObj.getCatImageResource());

        // 배경 설정 (날씨에 따라)
        updateBackgroundAndCatImage(weather);

        // 메인 고양이 메시지 (이모지 포함)
        String mainMessage = getCatMessageUseCase.execute(weather);
        catMessage.postValue(mainMessage);

        // 온도에 따른 추가 메시지 (개선된 버전)
        String tempMessage = getCatMessageUseCase.getTemperatureMessage(weather.getTemperature());
        temperatureMessage.postValue(tempMessage);

        // 우산 필요 여부 메시지 (개선된 버전)
        updateUmbrellaMessage(weather, catMessageObj);

        // 특별 상황 메시지 추가
        updateSpecialMessages();
    }

    // 배경과 고양이 이미지 업데이트 (3가지 날씨)
    private void updateBackgroundAndCatImage(Weather weather) {
        String condition = weather.getWeatherCondition();

        if (condition != null && condition.contains("비")) {
            // 비오는 날 - 파란색 계열
            backgroundResource.postValue(R.drawable.ios_background_rainy);
            catImageResource.postValue(R.drawable.cat_rainy);
        } else if (condition != null && condition.contains("흐림")) {
            // 흐린 날 - 회색 계열
            backgroundResource.postValue(R.drawable.ios_background_cloudy);
            catImageResource.postValue(R.drawable.cat_cloudy);
        } else {
            // 맑은 날 - 노란색/주황색 계열
            backgroundResource.postValue(R.drawable.ios_background_sunny);
            catImageResource.postValue(R.drawable.cat_sunny);
        }
    }

    // 우산 메시지 업데이트 (6월 9일 세종 맑은 날씨 기준, 간결하게)
    private void updateUmbrellaMessage(Weather weather, com.example.umbrellaalert.data.model.CatMessage catMessageObj) {
        String umbrellaMsg;

        // 토스 스타일로 간결한 메시지
        String[] sunnyDayMessages = {
            "우산 필요 없다냥! ☀️",
            "완전 맑은 날이다냥! 😸",
            "비 걱정 제로다냥! 🌤️",
            "나들이 완벽한 날씨냥! ☀️"
        };

        int randomIndex = (int) (Math.random() * sunnyDayMessages.length);
        umbrellaMsg = sunnyDayMessages[randomIndex];

        umbrellaMessage.postValue(umbrellaMsg);
    }

    // 특별 상황 메시지 업데이트
    private void updateSpecialMessages() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        int dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK);

        String specialMsg = null;

        // 아침 러시아워 (평일 7-9시)
        if ((dayOfWeek >= java.util.Calendar.MONDAY && dayOfWeek <= java.util.Calendar.FRIDAY)
            && (hour >= 7 && hour <= 9)) {
            specialMsg = getCatMessageUseCase.getSpecialMessage("morning_rush");
        }
        // 주말
        else if (dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY) {
            specialMsg = getCatMessageUseCase.getSpecialMessage("weekend");
        }
        // 늦은 시간
        else if (hour >= 22 || hour <= 5) {
            specialMsg = getCatMessageUseCase.getSpecialMessage("late_night");
        }

        // 특별 메시지가 있으면 온도 메시지 대신 사용
        if (specialMsg != null) {
            temperatureMessage.postValue(specialMsg);
        }
    }

    // HourlyForecast를 Weather 객체로 변환
    private Weather convertHourlyForecastToWeather(HourlyForecast forecast, Location location) {
        return new Weather(
                0,
                forecast.getTemperature(), // 예보 온도 사용
                forecast.getWeatherCondition(), // 예보 날씨 상태 사용
                forecast.getPrecipitation(), // 예보 강수량 사용
                forecast.getHumidity(), // 예보 습도 사용
                forecast.getWindSpeed(), // 예보 풍속 사용
                location.getLatitude() + "," + location.getLongitude(),
                System.currentTimeMillis(),
                forecast.isNeedUmbrella() // 예보 우산 필요 여부 사용
        );
    }

    // 기본 날씨 객체 생성
    private Weather createDefaultWeather(Location location) {
        return new Weather(
                0,
                29.2f,  // 기본 온도 29.2도
                "Clear", // 기본 날씨 상태
                0.0f,   // 강수량 없음
                50,     // 습도 50%
                2.0f,   // 풍속 2m/s
                location.getLatitude() + "," + location.getLongitude(),
                System.currentTimeMillis(),
                false   // 우산 필요 없음
        );
    }

    // 날씨 상태 텍스트 변환
    public String getWeatherConditionText(String condition) {
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

    // LiveData Getters
    public LiveData<Weather> getWeatherData() {
        return weatherData;
    }

    public LiveData<String> getLocationName() {
        return locationName;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Integer> getBackgroundResource() {
        return backgroundResource;
    }

    public LiveData<Integer> getCatImageResource() {
        return catImageResource;
    }

    public LiveData<String> getCatMessage() {
        return catMessage;
    }

    public LiveData<String> getUmbrellaMessage() {
        return umbrellaMessage;
    }

    public LiveData<List<HourlyForecast>> getHourlyForecastData() {
        return hourlyForecastData;
    }

    public LiveData<String> getForecastUpdateTime() {
        return forecastUpdateTime;
    }

    public LiveData<String> getTemperatureMessage() {
        return temperatureMessage;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
