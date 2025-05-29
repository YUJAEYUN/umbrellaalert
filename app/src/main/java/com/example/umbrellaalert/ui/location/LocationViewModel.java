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

/**
 * 위치 정보 관리를 위한 ViewModel
 */
public class LocationViewModel extends AndroidViewModel {

    private static final String TAG = "LocationViewModel";

    private final LocationRepository locationRepository;
    private final ExecutorService executorService;

    // LiveData
    private final MutableLiveData<List<Location>> locations = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> isEmpty = new MutableLiveData<>(true);
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public LocationViewModel(@NonNull Application application) {
        super(application);
        // TODO: Hilt 의존성 주입으로 변경 필요
        // 임시로 null 초기화 (실제 사용 시 Hilt로 주입 필요)
        this.locationRepository = null;
        executorService = Executors.newSingleThreadExecutor();

        // 위치 데이터 로드 (Repository가 null이므로 주석 처리)
        // loadLocations();
    }

    /**
     * 모든 위치 정보 로드
     */
    public void loadLocations() {
        if (locationRepository == null) {
            // Repository가 null인 경우 빈 리스트 반환
            this.locations.setValue(new java.util.ArrayList<>());
            isEmpty.setValue(true);
            isLoading.setValue(false);
            return;
        }

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
        if (locationRepository == null) {
            toastMessage.setValue("위치 서비스를 사용할 수 없습니다");
            return;
        }

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
        if (locationRepository == null) {
            toastMessage.setValue("위치 서비스를 사용할 수 없습니다");
            return;
        }

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
        if (locationRepository == null) {
            toastMessage.setValue("위치 서비스를 사용할 수 없습니다");
            return;
        }

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

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
