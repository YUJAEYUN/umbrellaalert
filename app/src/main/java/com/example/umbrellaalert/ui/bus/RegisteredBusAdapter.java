package com.example.umbrellaalert.ui.bus;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.umbrellaalert.data.model.BusArrival;
import com.example.umbrellaalert.data.model.RegisteredBus;
import com.example.umbrellaalert.databinding.ItemRegisteredBusBinding;
import com.example.umbrellaalert.service.LocationService;
import com.example.umbrellaalert.util.WalkingTimeCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * 등록된 버스 목록을 표시하는 RecyclerView 어댑터
 */
public class RegisteredBusAdapter extends RecyclerView.Adapter<RegisteredBusAdapter.BusViewHolder> {

    private List<RegisteredBus> buses = new ArrayList<>();
    private Map<String, BusArrival> arrivalInfoMap;
    private OnBusClickListener onBusClickListener;
    private OnBusDeleteListener onBusDeleteListener;
    private Context context;
    private WalkingTimeCalculator walkingTimeCalculator;
    private LocationService locationService;

    public interface OnBusClickListener {
        void onBusClick(RegisteredBus bus);
    }

    public interface OnBusDeleteListener {
        void onBusDelete(RegisteredBus bus);
    }

    public RegisteredBusAdapter(Context context) {
        this.context = context;
        this.walkingTimeCalculator = new WalkingTimeCalculator(context);
        this.locationService = LocationService.getInstance(context);
    }

    @NonNull
    @Override
    public BusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRegisteredBusBinding binding = ItemRegisteredBusBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new BusViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BusViewHolder holder, int position) {
        RegisteredBus bus = buses.get(position);
        holder.bind(bus);
    }

    @Override
    public int getItemCount() {
        return buses.size();
    }

    /**
     * 버스 목록 업데이트
     */
    public void updateBuses(List<RegisteredBus> newBuses) {
        this.buses.clear();
        if (newBuses != null) {
            this.buses.addAll(newBuses);
        }
        notifyDataSetChanged();
    }

    /**
     * 도착 정보 업데이트
     */
    public void updateArrivalInfo(Map<String, BusArrival> arrivalInfoMap) {
        this.arrivalInfoMap = arrivalInfoMap;
        notifyDataSetChanged();
    }

    /**
     * 클릭 리스너 설정
     */
    public void setOnBusClickListener(OnBusClickListener listener) {
        this.onBusClickListener = listener;
    }

    /**
     * 삭제 리스너 설정
     */
    public void setOnBusDeleteListener(OnBusDeleteListener listener) {
        this.onBusDeleteListener = listener;
    }

    class BusViewHolder extends RecyclerView.ViewHolder {
        private final ItemRegisteredBusBinding binding;

        public BusViewHolder(@NonNull ItemRegisteredBusBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(RegisteredBus bus) {
            // 기본 정보 설정
            binding.tvRouteNo.setText(bus.getRouteNo() + "번");
            binding.tvNodeName.setText(bus.getNodeName());
            
            // 방향 정보 설정
            if (bus.getDirectionName() != null && !bus.getDirectionName().isEmpty()) {
                binding.tvDirection.setText("→ " + bus.getDirectionName());
                binding.tvDirection.setVisibility(View.VISIBLE);
            } else {
                binding.tvDirection.setVisibility(View.GONE);
            }

            // 도착 정보 설정
            String key = bus.getNodeId() + "_" + bus.getRouteId();
            BusArrival arrival = null;
            if (arrivalInfoMap != null) {
                arrival = arrivalInfoMap.get(key);
            }

            if (arrival != null) {
                // 도착 정보가 있는 경우
                binding.progressBarItem.setVisibility(View.GONE);
                binding.tvErrorState.setVisibility(View.GONE);
                binding.tvArrivalTime.setVisibility(View.VISIBLE);
                binding.tvStationCount.setVisibility(View.VISIBLE);

                binding.tvArrivalTime.setText(arrival.getFormattedArrTime());
                binding.tvStationCount.setText(arrival.getFormattedStationCount());
            } else {
                // 도착 정보가 없는 경우
                binding.tvArrivalTime.setVisibility(View.GONE);
                binding.tvStationCount.setVisibility(View.GONE);
                binding.progressBarItem.setVisibility(View.GONE);
                binding.tvErrorState.setVisibility(View.VISIBLE);
                binding.tvErrorState.setText("정보 없음");
            }

            // 도보 시간 계산 및 표시
            calculateAndDisplayWalkingTime(bus);

            // 클릭 리스너
            binding.getRoot().setOnClickListener(v -> {
                if (onBusClickListener != null) {
                    onBusClickListener.onBusClick(bus);
                }
            });

            // 삭제 버튼 클릭 리스너
            binding.btnDelete.setOnClickListener(v -> {
                if (onBusDeleteListener != null) {
                    onBusDeleteListener.onBusDelete(bus);
                }
            });

            // 길게 누르기로 삭제 버튼 표시/숨김
            binding.getRoot().setOnLongClickListener(v -> {
                toggleDeleteButton();
                return true;
            });
        }

        private void toggleDeleteButton() {
            if (binding.btnDelete.getVisibility() == View.VISIBLE) {
                binding.btnDelete.setVisibility(View.GONE);
            } else {
                binding.btnDelete.setVisibility(View.VISIBLE);
            }
        }

        /**
         * 도보 시간 계산 및 표시
         */
        private void calculateAndDisplayWalkingTime(RegisteredBus bus) {
            // 정류장 위치 정보가 있는지 확인
            if (bus.getLatitude() == 0.0 && bus.getLongitude() == 0.0) {
                binding.walkingTimeContainer.setVisibility(View.GONE);
                return;
            }

            // 현재 위치 가져오기
            Location currentLocation = locationService.getLastLocation();
            if (currentLocation == null) {
                binding.walkingTimeContainer.setVisibility(View.GONE);
                return;
            }

            // 도보 시간 계산 (백그라운드에서)
            new Thread(() -> {
                try {
                    Future<Integer> walkingTimeFuture = walkingTimeCalculator.calculateWalkingTime(
                            currentLocation.getLatitude(), currentLocation.getLongitude(),
                            bus.getLatitude(), bus.getLongitude());

                    int walkingTimeMinutes = walkingTimeFuture.get();

                    // UI 스레드에서 업데이트
                    binding.getRoot().post(() -> {
                        binding.walkingTimeContainer.setVisibility(View.VISIBLE);
                        binding.tvWalkingTime.setText("도보 " + walkingTimeMinutes + "분");
                    });

                } catch (Exception e) {
                    // 오류 발생 시 숨김
                    binding.getRoot().post(() -> {
                        binding.walkingTimeContainer.setVisibility(View.GONE);
                    });
                }
            }).start();
        }

        /**
         * 로딩 상태 표시
         */
        public void showLoading() {
            binding.progressBarItem.setVisibility(View.VISIBLE);
            binding.tvArrivalTime.setVisibility(View.GONE);
            binding.tvStationCount.setVisibility(View.GONE);
            binding.tvErrorState.setVisibility(View.GONE);
        }

        /**
         * 오류 상태 표시
         */
        public void showError(String message) {
            binding.progressBarItem.setVisibility(View.GONE);
            binding.tvArrivalTime.setVisibility(View.GONE);
            binding.tvStationCount.setVisibility(View.GONE);
            binding.tvErrorState.setVisibility(View.VISIBLE);
            binding.tvErrorState.setText(message);
        }
    }

    /**
     * 특정 위치의 아이템 로딩 상태 표시
     */
    public void showLoadingForItem(int position) {
        if (position >= 0 && position < buses.size()) {
            notifyItemChanged(position);
        }
    }

    /**
     * 특정 위치의 아이템 오류 상태 표시
     */
    public void showErrorForItem(int position, String message) {
        if (position >= 0 && position < buses.size()) {
            notifyItemChanged(position);
        }
    }
}
