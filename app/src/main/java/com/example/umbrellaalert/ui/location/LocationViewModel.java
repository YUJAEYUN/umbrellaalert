package com.example.umbrellaalert.ui.location;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.umbrellaalert.data.model.Location;
import com.example.umbrellaalert.domain.repository.LocationRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * 위치 정보 관리를 위한 ViewModel
 */
@HiltViewModel
public class LocationViewModel extends AndroidViewModel {

    private static final String TAG = "LocationViewModel";

    private final LocationRepository locationRepository;
    private final ExecutorService executorService;

    // LiveData
    private final MutableLiveData<List<Location>> locations = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> isEmpty = new MutableLiveData<>(true);
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    @Inject
    public LocationViewModel(@NonNull Application application, LocationRepository locationRepository) {
        super(application);
        this.locationRepository = locationRepository;
        executorService = Executors.newSingleThreadExecutor();

        // 위치 데이터 로드
        loadLocations();
    }

    /**
     * 모든 위치 정보 로드
     */
    public void loadLocations() {
        isLoading.setValue(true);

        executorService.execute(() -> {
            List<Location> locationList = locationRepository.getAllLocations();
            locations.postValue(locationList);
            isEmpty.postValue(locationList == null || locationList.isEmpty());
            isLoading.postValue(false);
        });
    }

    /**
     * 새 위치 추가
     */
    public void addLocation(Location location) {
        executorService.execute(() -> {
            long id = locationRepository.insertLocation(location);

            if (id > 0) {
                toastMessage.postValue("위치가 추가되었습니다");
                loadLocations();
            } else {
                toastMessage.postValue("위치 추가 실패");
            }
        });
    }



    /**
     * 위치 삭제
     */
    public void deleteLocation(Location location) {
        executorService.execute(() -> {
            locationRepository.deleteLocation(location.getId());

            toastMessage.postValue("위치가 삭제되었습니다");
            loadLocations();
        });
    }



    /**
     * 위치 알림 설정 토글
     */
    public void toggleNotification(Location location) {
        // 알림 설정 토글
        location.setNotificationEnabled(!location.isNotificationEnabled());

        executorService.execute(() -> {
            locationRepository.updateLocation(location);

            String message = location.isNotificationEnabled() ?
                    "알림이 활성화되었습니다" : "알림이 비활성화되었습니다";
            toastMessage.postValue(message);
            loadLocations();
        });
    }



    // LiveData Getters
    public LiveData<List<Location>> getLocations() {
        return locations;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getIsEmpty() {
        return isEmpty;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    /**
     * 모든 활성화된 위치에 대해 날씨를 체크하여 우산 필요 여부 확인
     */
    public void checkAllLocationsWeather(WeatherCheckCallback callback) {
        List<Location> currentLocations = locations.getValue();
        if (currentLocations == null || currentLocations.isEmpty()) {
            callback.onWeatherCheckCompleted(false);
            return;
        }

        // WeatherManager를 통해 날씨 체크 (실제 구현에서는 Hilt로 주입받아야 함)
        // 현재는 목업 데이터로 처리
        executorService.execute(() -> {
            boolean anyLocationNeedsUmbrella = false;

            for (Location location : currentLocations) {
                if (location.isNotificationEnabled()) {
                    // 실제 구현에서는 WeatherManager.checkAllLocationsWeather 사용
                    // 현재는 간단한 로직으로 처리 (위치 이름에 따라 임의 결정)
                    boolean needsUmbrella = location.getName().contains("학교") ||
                                          location.getName().contains("대학") ||
                                          Math.random() > 0.7; // 30% 확률로 우산 필요

                    if (needsUmbrella) {
                        anyLocationNeedsUmbrella = true;
                        break;
                    }
                }
            }

            final boolean finalResult = anyLocationNeedsUmbrella;
            // UI 스레드에서 콜백 호출
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                callback.onWeatherCheckCompleted(finalResult);
            });
        });
    }

    /**
     * 날씨 체크 완료 콜백 인터페이스
     */
    public interface WeatherCheckCallback {
        void onWeatherCheckCompleted(boolean anyLocationNeedsUmbrella);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
