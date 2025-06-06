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
import com.example.umbrellaalert.data.model.BusStop;
import com.example.umbrellaalert.data.model.RegisteredBus;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 버스 설정 화면의 ViewModel
 */
public class BusSettingsViewModel extends AndroidViewModel {
    
    private static final String TAG = "BusSettingsViewModel";
    
    private final BusApiClient busApiClient;
    private final BusDao busDao;
    private final ExecutorService executorService;
    
    // LiveData
    private final MutableLiveData<List<BusStop>> nearbyBusStops = new MutableLiveData<>();
    private final MutableLiveData<List<BusArrival>> busArrivals = new MutableLiveData<>();
    private final MutableLiveData<BusStop> selectedBusStop = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public BusSettingsViewModel(@NonNull Application application) {
        super(application);
        this.busApiClient = new BusApiClient(application);
        this.busDao = new BusDao(DatabaseHelper.getInstance(application));
        this.executorService = Executors.newCachedThreadPool();
    }

    // Getters for LiveData
    public LiveData<List<BusStop>> getNearbyBusStops() {
        return nearbyBusStops;
    }

    public LiveData<List<BusArrival>> getBusArrivals() {
        return busArrivals;
    }

    public LiveData<BusStop> getSelectedBusStop() {
        return selectedBusStop;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * 근처 정류장 검색
     */
    public void loadNearbyBusStops(double latitude, double longitude) {
        isLoading.setValue(true);
        
        executorService.execute(() -> {
            try {
                Future<List<BusStop>> future = busApiClient.getNearbyBusStops(latitude, longitude);
                List<BusStop> stops = future.get();
                
                nearbyBusStops.postValue(stops);
                Log.d(TAG, "근처 정류장 로드 완료: " + stops.size() + "개");
                
            } catch (Exception e) {
                Log.e(TAG, "근처 정류장 로드 실패", e);
                errorMessage.postValue("주변 정류장을 불러올 수 없습니다.");
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    /**
     * 정류장 선택
     */
    public void selectBusStop(BusStop busStop) {
        selectedBusStop.setValue(busStop);
    }

    /**
     * 정류장 선택 해제
     */
    public void clearSelectedBusStop() {
        selectedBusStop.setValue(null);
        busArrivals.setValue(null);
    }

    /**
     * 선택된 정류장의 버스 도착 정보 로드
     */
    public void loadBusArrivals(BusStop busStop) {
        if (busStop == null) return;
        
        isLoading.setValue(true);
        
        executorService.execute(() -> {
            try {
                Future<List<BusArrival>> future = busApiClient.getBusArrivalInfo(
                    busStop.getNodeId(), busStop.getCityCode());
                
                List<BusArrival> arrivals = future.get();
                busArrivals.postValue(arrivals);
                
                Log.d(TAG, "버스 도착 정보 로드 완료: " + arrivals.size() + "개");
                
            } catch (Exception e) {
                Log.e(TAG, "버스 도착 정보 로드 실패", e);
                errorMessage.postValue("버스 도착 정보를 불러올 수 없습니다.");
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    /**
     * 버스 등록
     */
    public void registerBus(BusStop busStop, BusArrival busArrival) {
        if (busStop == null || busArrival == null) return;

        executorService.execute(() -> {
            try {
                // 중복 확인
                if (busDao.isDuplicateBus(busStop.getNodeId(), busArrival.getRouteId())) {
                    errorMessage.postValue("이미 등록된 버스입니다.");
                    return;
                }

                // RegisteredBus 객체 생성 (위치 정보 포함)
                RegisteredBus registeredBus = new RegisteredBus(
                    busStop.getNodeId(),
                    busStop.getNodeName(),
                    busArrival.getRouteId(),
                    busArrival.getRouteNo(),
                    busArrival.getDirectionName(),
                    busStop.getCityCode(),
                    busStop.getGpsLati(),
                    busStop.getGpsLong()
                );

                long id = busDao.insertRegisteredBus(registeredBus);
                if (id > 0) {
                    Log.d(TAG, "버스 등록 완료: " + busArrival.getRouteNo());
                    // 성공 메시지는 Activity에서 처리
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
     * 버스 번호로 직접 등록
     */
    public void registerBusByNumber(BusStop busStop, String busNumber) {
        if (busStop == null || busNumber == null || busNumber.trim().isEmpty()) {
            errorMessage.postValue("버스 번호를 입력해주세요.");
            return;
        }

        executorService.execute(() -> {
            try {
                // 먼저 해당 정류장의 버스 도착 정보를 가져와서 해당 번호의 버스가 있는지 확인
                Future<List<BusArrival>> future = busApiClient.getBusArrivalInfo(
                    busStop.getNodeId(), busStop.getCityCode());

                List<BusArrival> arrivals = future.get();

                // 입력한 번호와 일치하는 버스 찾기
                BusArrival targetBus = null;
                for (BusArrival arrival : arrivals) {
                    if (busNumber.trim().equals(arrival.getRouteNo())) {
                        targetBus = arrival;
                        break;
                    }
                }

                if (targetBus != null) {
                    // 해당 버스를 찾았으면 일반 등록 프로세스 진행
                    registerBus(busStop, targetBus);
                } else {
                    // 해당 번호의 버스가 현재 이 정류장에 없으면 기본 정보로 등록
                    String routeId = "MANUAL_" + busNumber + "_" + busStop.getNodeId();

                    // 중복 확인
                    if (busDao.isDuplicateBus(busStop.getNodeId(), routeId)) {
                        errorMessage.postValue("이미 등록된 버스입니다.");
                        return;
                    }

                    RegisteredBus registeredBus = new RegisteredBus(
                        busStop.getNodeId(),
                        busStop.getNodeName(),
                        routeId,
                        busNumber,
                        "수동 등록", // 방향 정보
                        busStop.getCityCode(),
                        busStop.getGpsLati(),
                        busStop.getGpsLong()
                    );

                    long id = busDao.insertRegisteredBus(registeredBus);
                    if (id > 0) {
                        Log.d(TAG, "버스 번호로 등록 완료: " + busNumber);
                        // 성공 메시지는 Activity에서 처리
                    } else {
                        errorMessage.postValue("버스 등록에 실패했습니다.");
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "버스 번호 등록 실패", e);
                errorMessage.postValue("버스 등록 중 오류가 발생했습니다.");
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
