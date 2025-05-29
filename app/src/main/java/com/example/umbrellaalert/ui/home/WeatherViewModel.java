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
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.domain.usecase.GetCurrentWeatherUseCase;
import com.example.umbrellaalert.domain.usecase.GetCatMessageUseCase;

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
    private final ExecutorService executorService;

    // LiveData
    private final MutableLiveData<Weather> weatherData = new MutableLiveData<>();
    private final MutableLiveData<String> locationName = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private final MutableLiveData<Integer> backgroundResource = new MutableLiveData<>();
    private final MutableLiveData<Integer> catImageResource = new MutableLiveData<>();
    private final MutableLiveData<String> catMessage = new MutableLiveData<>();
    private final MutableLiveData<String> umbrellaMessage = new MutableLiveData<>();
    private final MutableLiveData<List<Object>> forecastData = new MutableLiveData<>();
    private final MutableLiveData<String> temperatureMessage = new MutableLiveData<>();

    @Inject
    public WeatherViewModel(@NonNull Application application,
                           GetCurrentWeatherUseCase getCurrentWeatherUseCase,
                           GetCatMessageUseCase getCatMessageUseCase) {
        super(application);
        this.getCurrentWeatherUseCase = getCurrentWeatherUseCase;
        this.getCatMessageUseCase = getCatMessageUseCase;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    // 위치 기반 날씨 업데이트
    public void updateWeatherWithLocation(Location location) {
        isLoading.setValue(true);

        executorService.execute(() -> {
            try {
                // UseCase를 통해 현재 날씨 가져오기
                Weather weather = getCurrentWeatherUseCase.execute(
                        location.getLatitude(), location.getLongitude());

                if (weather != null) {
                    weatherData.postValue(weather);
                    updateWeatherUI(weather);
                } else {
                    // 날씨 정보를 가져올 수 없는 경우 기본값 사용
                    Weather defaultWeather = createDefaultWeather(location);
                    weatherData.postValue(defaultWeather);
                    updateWeatherUI(defaultWeather);
                }

                // 예보 데이터는 현재 사용하지 않음
                forecastData.postValue(new java.util.ArrayList<>());

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

    // 기본 위치(서울) 사용
    public void updateWeatherWithDefaultLocation() {
        // 기본 위치 대신 위치 권한 요청 유도
        isLoading.setValue(false);
        locationName.setValue("위치 권한이 필요합니다");

        // 기본 날씨 정보 생성
        android.location.Location defaultLocation = new android.location.Location("default");
        defaultLocation.setLatitude(DEFAULT_LATITUDE);
        defaultLocation.setLongitude(DEFAULT_LONGITUDE);

        Weather defaultWeather = createDefaultWeather(defaultLocation);
        weatherData.setValue(defaultWeather);
        updateWeatherUI(defaultWeather);
        forecastData.setValue(new java.util.ArrayList<>());
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

    // UI 업데이트
    private void updateWeatherUI(Weather weather) {
        // 배경 설정 (날씨에 따라)
        updateBackgroundAndCatImage(weather);

        // UseCase를 통해 고양이 메시지 생성
        String mainMessage = getCatMessageUseCase.execute(weather);
        catMessage.postValue(mainMessage);

        // 온도에 따른 추가 메시지
        String tempMessage = getCatMessageUseCase.getTemperatureMessage(weather.getTemperature());
        temperatureMessage.postValue(tempMessage);

        // 우산 필요 여부 메시지
        updateUmbrellaMessage(weather);
    }

    // 배경과 고양이 이미지 업데이트
    private void updateBackgroundAndCatImage(Weather weather) {
        if (weather.isNeedUmbrella()) {
            backgroundResource.postValue(R.drawable.ios_background_rainy);
            catImageResource.postValue(R.drawable.cat_rainy);
        } else {
            String condition = weather.getWeatherCondition();
            if (condition != null && condition.equalsIgnoreCase("Clear")) {
                backgroundResource.postValue(R.drawable.ios_background_sunny);
                catImageResource.postValue(R.drawable.cat_sunny);
            } else {
                backgroundResource.postValue(R.drawable.ios_background_cloudy);
                catImageResource.postValue(R.drawable.cat_cloudy);
            }
        }
    }

    // 우산 메시지 업데이트
    private void updateUmbrellaMessage(Weather weather) {
        if (weather.isNeedUmbrella()) {
            if (weather.getPrecipitation() > 5) {
                umbrellaMessage.postValue("비가 많이 올 예정이다냥! 우산을 꼭 챙겨라냥!");
            } else {
                umbrellaMessage.postValue("오늘은 우산이 필요하다냥!");
            }
        } else {
            umbrellaMessage.postValue("오늘은 우산이 필요 없을 것 같다냥~");
        }
    }

    // 기본 날씨 객체 생성
    private Weather createDefaultWeather(Location location) {
        return new Weather(
                0,
                20.0f,  // 기본 온도 20도
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

    public LiveData<List<Object>> getForecastData() {
        return forecastData;
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
