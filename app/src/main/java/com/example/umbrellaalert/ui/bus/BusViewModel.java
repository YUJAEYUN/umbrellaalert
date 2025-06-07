package com.example.umbrellaalert.ui.bus;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.umbrellaalert.data.api.BusApiClient;
import com.example.umbrellaalert.data.database.BusDao;
import com.example.umbrellaalert.data.database.DatabaseHelper;
import com.example.umbrellaalert.data.model.BusArrival;
import com.example.umbrellaalert.data.model.RegisteredBus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 버스 관련 데이터를 관리하는 ViewModel
 */
public class BusViewModel extends AndroidViewModel {
    
    private static final String TAG = "BusViewModel";
    
    private final BusApiClient busApiClient;
    private final BusDao busDao;
    private final ExecutorService executorService;
    
    // LiveData
    private final MutableLiveData<List<RegisteredBus>> registeredBuses = new MutableLiveData<>();
    private final MutableLiveData<Map<String, BusArrival>> arrivalInfoMap = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public BusViewModel(@NonNull Application application) {
        super(application);
        this.busApiClient = new BusApiClient(application);
        this.busDao = new BusDao(DatabaseHelper.getInstance(application));
        this.executorService = Executors.newCachedThreadPool();
        this.arrivalInfoMap.setValue(new HashMap<>());
    }

    // Getters for LiveData
    public LiveData<List<RegisteredBus>> getRegisteredBuses() {
        return registeredBuses;
    }

    public LiveData<Map<String, BusArrival>> getArrivalInfoMap() {
        return arrivalInfoMap;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * 등록된 버스 목록 로드
     */
    public void loadRegisteredBuses() {
        isLoading.setValue(true);
        
        executorService.execute(() -> {
            try {
                List<RegisteredBus> buses = busDao.getAllRegisteredBuses();
                registeredBuses.postValue(buses);
                
                // 각 버스의 도착 정보도 함께 로드
                if (!buses.isEmpty()) {
                    loadArrivalInfoForBuses(buses);
                }
                
                Log.d(TAG, "등록된 버스 로드 완료: " + buses.size() + "개");
                
            } catch (Exception e) {
                Log.e(TAG, "등록된 버스 로드 실패", e);
                errorMessage.postValue("버스 정보를 불러올 수 없습니다.");
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    /**
     * 버스 등록
     */
    public void registerBus(RegisteredBus bus) {
        executorService.execute(() -> {
            try {
                // 중복 확인
                if (busDao.isDuplicateBus(bus.getNodeId(), bus.getRouteId())) {
                    errorMessage.postValue("이미 등록된 버스입니다.");
                    return;
                }
                
                long id = busDao.insertRegisteredBus(bus);
                if (id > 0) {
                    bus.setId((int) id);
                    Log.d(TAG, "버스 등록 완료: " + bus.getRouteNo());
                    loadRegisteredBuses(); // 목록 새로고침
                } else {
                    errorMessage.postValue("버스 등록에 실패했습니다.");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "버스 등록 실패", e);
                errorMessage.postValue("버스 등록 중 오류가 발생했습니다.");
            }
        });
    }

    /**
     * 버스 삭제
     */
    public void deleteBus(int busId) {
        executorService.execute(() -> {
            try {
                int result = busDao.deleteRegisteredBus(busId);
                if (result > 0) {
                    Log.d(TAG, "버스 삭제 완료: ID=" + busId);
                    loadRegisteredBuses(); // 목록 새로고침
                } else {
                    errorMessage.postValue("버스 삭제에 실패했습니다.");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "버스 삭제 실패", e);
                errorMessage.postValue("버스 삭제 중 오류가 발생했습니다.");
            }
        });
    }

    /**
     * 버스들의 도착 정보 로드
     */
    private void loadArrivalInfoForBuses(List<RegisteredBus> buses) {
        Map<String, BusArrival> newArrivalMap = new HashMap<>();
        Map<String, String> busStatusMap = new HashMap<>(); // 버스 상태 추가

        for (RegisteredBus bus : buses) {
            try {
                Future<List<BusArrival>> future = busApiClient.getBusArrivalInfo(
                    bus.getNodeId(), bus.getCityCode());

                List<BusArrival> arrivals = future.get();
                String key = bus.getNodeId() + "_" + bus.getRouteId();

                // 등록한 버스 번호와 일치하는 도착 정보 찾기
                boolean found = false;
                for (BusArrival arrival : arrivals) {
                    // 버스 번호로 매칭 (더 정확한 매칭)
                    if (bus.getRouteNo().equals(arrival.getRouteNo())) {
                        newArrivalMap.put(key, arrival);
                        busStatusMap.put(key, "운행중");
                        found = true;

                        Log.d(TAG, String.format("✅ 등록된 버스 매칭 성공: %s번 → %s에 %d분 후 도착",
                            bus.getRouteNo(), bus.getNodeName(), arrival.getArrTime()));
                        break;
                    }
                }

                // 해당 노선을 찾지 못한 경우
                if (!found) {
                    // 전체 도착 정보가 비어있으면 정류장 자체에 문제
                    if (arrivals.isEmpty()) {
                        busStatusMap.put(key, "정류장 정보 없음");
                    } else {
                        // 다른 버스는 있지만 해당 노선이 없으면 운행 종료 또는 지나감
                        busStatusMap.put(key, "운행 종료");
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "도착 정보 로드 실패: " + bus.getRouteNo(), e);
                String key = bus.getNodeId() + "_" + bus.getRouteId();
                busStatusMap.put(key, "정보 로드 실패");
            }
        }

        arrivalInfoMap.postValue(newArrivalMap);
        // 상태 정보도 함께 전달 (추후 필요시 별도 LiveData로 분리 가능)
    }

    /**
     * 특정 버스의 도착 정보 새로고침
     */
    public void refreshArrivalInfo(RegisteredBus bus) {
        executorService.execute(() -> {
            try {
                Future<List<BusArrival>> future = busApiClient.getBusArrivalInfo(
                    bus.getNodeId(), bus.getCityCode());
                
                List<BusArrival> arrivals = future.get();
                
                Map<String, BusArrival> currentMap = arrivalInfoMap.getValue();
                if (currentMap == null) {
                    currentMap = new HashMap<>();
                }
                
                // 해당 노선의 도착 정보 업데이트
                for (BusArrival arrival : arrivals) {
                    if (bus.getRouteId().equals(arrival.getRouteId()) || 
                        bus.getRouteNo().equals(arrival.getRouteNo())) {
                        String key = bus.getNodeId() + "_" + bus.getRouteId();
                        currentMap.put(key, arrival);
                        break;
                    }
                }
                
                arrivalInfoMap.postValue(currentMap);
                
            } catch (Exception e) {
                Log.e(TAG, "도착 정보 새로고침 실패: " + bus.getRouteNo(), e);
            }
        });
    }

    /**
     * 모든 버스의 도착 정보 새로고침
     */
    public void refreshAllArrivalInfo() {
        List<RegisteredBus> buses = registeredBuses.getValue();
        if (buses != null && !buses.isEmpty()) {
            loadArrivalInfoForBuses(buses);
        }
    }

    /**
     * 특정 버스의 도착 정보 가져오기
     */
    public BusArrival getArrivalInfo(RegisteredBus bus) {
        Map<String, BusArrival> map = arrivalInfoMap.getValue();
        if (map != null) {
            String key = bus.getNodeId() + "_" + bus.getRouteId();
            return map.get(key);
        }
        return null;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
