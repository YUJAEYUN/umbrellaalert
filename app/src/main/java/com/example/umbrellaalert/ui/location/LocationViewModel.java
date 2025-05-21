package com.example.umbrellaalert.ui.location;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.umbrellaalert.data.model.Location;
import com.example.umbrellaalert.data.repository.LocationRepository;

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
        locationRepository = new LocationRepository(application);
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
            int result = locationRepository.deleteLocation(location.getId());
            
            if (result > 0) {
                toastMessage.postValue("위치가 삭제되었습니다");
                loadLocations();
            } else {
                toastMessage.postValue("위치 삭제 실패");
            }
        });
    }
    
    /**
     * 위치 알림 설정 토글
     */
    public void toggleNotification(Location location) {
        // 알림 설정 토글
        location.setNotificationEnabled(!location.isNotificationEnabled());
        
        executorService.execute(() -> {
            int result = locationRepository.updateLocation(location);
            
            if (result > 0) {
                String message = location.isNotificationEnabled() ?
                        "알림이 활성화되었습니다" : "알림이 비활성화되었습니다";
                toastMessage.postValue(message);
            } else {
                toastMessage.postValue("설정 변경 실패");
            }
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
