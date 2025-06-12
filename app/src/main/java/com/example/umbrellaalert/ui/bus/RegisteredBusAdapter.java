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
import com.example.umbrellaalert.R;
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
     * 특정 버스 삭제 (애니메이션 포함)
     */
    public void removeBus(RegisteredBus busToRemove) {
        int position = -1;
        for (int i = 0; i < buses.size(); i++) {
            if (buses.get(i).getId() == busToRemove.getId()) {
                position = i;
                break;
            }
        }

        if (position != -1) {
            buses.remove(position);
            notifyItemRemoved(position);

            // 삭제된 아이템 이후의 아이템들 위치 업데이트
            if (position < buses.size()) {
                notifyItemRangeChanged(position, buses.size() - position);
            }
        }
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
            // 버스 번호 (간결하게)
            binding.tvRouteNo.setText(bus.getRouteNo() + "번");

            // 정류장명만 간결하게 표시
            String nodeName = bus.getNodeName();
            if (nodeName.length() > 12) {
                nodeName = nodeName.substring(0, 12) + "...";
            }
            binding.tvNodeName.setText(nodeName);
            // 초기 상태 설정
            binding.tvArrivalTime.setVisibility(View.GONE);
            binding.tvWalkingTime.setVisibility(View.GONE);
            binding.tvSmartStatus.setVisibility(View.GONE);
            binding.tvErrorState.setVisibility(View.GONE);
            binding.progressBarItem.setVisibility(View.VISIBLE);

            // 도착 정보 설정
            String key = bus.getNodeId() + "_" + bus.getRouteId();
            BusArrival arrival = null;
            if (arrivalInfoMap != null) {
                arrival = arrivalInfoMap.get(key);
            }

            if (arrival != null) {
                // 버스 정보가 있는 경우
                binding.progressBarItem.setVisibility(View.GONE);
                binding.tvErrorState.setVisibility(View.GONE);
                binding.tvArrivalTime.setVisibility(View.VISIBLE);

                // 도착 시간 간결하게 표시
                String arrivalText = arrival.getFormattedArrTime();
                // "3분 후" -> "3분"으로 간소화
                if (arrivalText.contains("분 후")) {
                    arrivalText = arrivalText.replace("분 후", "분");
                } else if (arrivalText.contains("곧 도착")) {
                    arrivalText = "곧";
                }
                binding.tvArrivalTime.setText(arrivalText);

                // 스마트 알림 표시
                showSmartNotification(bus, arrival);
            } else {
                // 도착 정보가 없는 경우
                binding.progressBarItem.setVisibility(View.GONE);
                binding.tvArrivalTime.setVisibility(View.GONE);
                binding.tvErrorState.setVisibility(View.VISIBLE);
            }

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
         * 로딩 상태 표시
         */
        public void showLoading() {
            binding.progressBarItem.setVisibility(View.VISIBLE);
            binding.tvArrivalTime.setVisibility(View.GONE);
            binding.tvWalkingTime.setVisibility(View.GONE);
            binding.tvSmartStatus.setVisibility(View.GONE);
            binding.tvErrorState.setVisibility(View.GONE);
        }

        /**
         * 오류 상태 표시
         */
        public void showError(String message) {
            binding.progressBarItem.setVisibility(View.GONE);
            binding.tvArrivalTime.setVisibility(View.GONE);
            binding.tvWalkingTime.setVisibility(View.GONE);
            binding.tvSmartStatus.setVisibility(View.GONE);
            binding.tvErrorState.setVisibility(View.VISIBLE);
        }

        /**
         * 스마트 알림 표시 - 새로운 로직 적용
         */
        private void showSmartNotification(RegisteredBus bus, BusArrival arrival) {
            // 정류장 위치 정보가 없으면 건너뛰기
            if (bus.getLatitude() == 0.0 && bus.getLongitude() == 0.0) {
                binding.tvWalkingTime.setVisibility(View.GONE);
                binding.tvSmartStatus.setVisibility(View.GONE);
                return;
            }

            // 현재 위치 가져오기
            Location currentLocation = locationService.getLastLocation();
            if (currentLocation == null) {
                binding.tvWalkingTime.setVisibility(View.GONE);
                binding.tvSmartStatus.setVisibility(View.GONE);
                return;
            }

            // 도보 시간 계산 (백그라운드에서)
            new Thread(() -> {
                try {
                    Future<Integer> walkingTimeFuture = walkingTimeCalculator.calculateWalkingTime(
                            currentLocation.getLatitude(), currentLocation.getLongitude(),
                            bus.getLatitude(), bus.getLongitude());

                    int walkingTimeMinutes = walkingTimeFuture.get();
                    int busArrivalMinutes = arrival.getArrTime();

                    // UI 스레드에서 업데이트
                    binding.getRoot().post(() -> {
                        // 도보 시간 표시
                        binding.tvWalkingTime.setVisibility(View.VISIBLE);
                        binding.tvWalkingTime.setText("🚶‍♂️ " + walkingTimeMinutes + "분");

                        // 스마트 상태 메시지 표시
                        showSmartStatus(walkingTimeMinutes, busArrivalMinutes);
                    });

                } catch (Exception e) {
                    // 오류 발생 시 숨김
                    binding.getRoot().post(() -> {
                        binding.tvWalkingTime.setVisibility(View.GONE);
                        binding.tvSmartStatus.setVisibility(View.GONE);
                    });
                }
            }).start();
        }

        /**
         * 스마트 상태 메시지 표시 - 새로운 로직
         */
        private void showSmartStatus(int walkingTime, int busArrival) {
            String statusMessage = "";
            int statusColor = R.color.text_secondary;

            // 새로운 로직: 버스 도착시간 < 도보시간 × 0.8 일 때
            if (busArrival < walkingTime * 0.8) {
                statusMessage = "😅 다음 버스 확인해보세요";
                statusColor = android.R.color.holo_orange_dark;
            } else if (busArrival <= walkingTime + 1) {
                statusMessage = "🏃‍♂️ 지금 출발!";
                statusColor = android.R.color.holo_red_dark;
            } else if (busArrival <= walkingTime + 3) {
                statusMessage = "⚡ 준비하세요";
                statusColor = android.R.color.holo_orange_dark;
            } else {
                statusMessage = "👍 여유있음";
                statusColor = android.R.color.holo_green_dark;
            }

            // 스마트 상태 표시
            binding.tvSmartStatus.setVisibility(View.VISIBLE);
            binding.tvSmartStatus.setText(statusMessage);
            binding.tvSmartStatus.setTextColor(context.getResources().getColor(statusColor));
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
