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
            // 등록한 버스+정류장 조합을 명확하게 표시
            binding.tvRouteNo.setText(bus.getRouteNo() + "번");

            // 정류장명을 메인으로 표시
            StringBuilder mainInfo = new StringBuilder();
            mainInfo.append("📍 ").append(bus.getNodeName());

            // 방향 정보가 있으면 추가
            if (bus.getDirectionName() != null && !bus.getDirectionName().isEmpty()
                && !bus.getDirectionName().equals("수동 등록")) {
                mainInfo.append("\n🚌 ").append(bus.getDirectionName()).append(" 방면");
            }

            binding.tvNodeName.setText(mainInfo.toString());
            binding.tvDirection.setVisibility(View.GONE); // 정류장명에 포함시켰으므로 숨김

            // 도착 정보 설정
            String key = bus.getNodeId() + "_" + bus.getRouteId();
            BusArrival arrival = null;
            if (arrivalInfoMap != null) {
                arrival = arrivalInfoMap.get(key);
            }

            if (arrival != null) {
                // 등록한 버스가 해당 정류장에 도착하는 정보 표시
                binding.progressBarItem.setVisibility(View.GONE);
                binding.tvErrorState.setVisibility(View.GONE);
                binding.tvArrivalTime.setVisibility(View.VISIBLE);
                binding.tvStationCount.setVisibility(View.VISIBLE);

                // 메인 도착 시간 표시 - 더 강조
                String arrivalText = arrival.getFormattedArrTime();
                if (arrival.getArrTime() <= 3) {
                    // 3분 이하면 빨간색으로 강조
                    binding.tvArrivalTime.setText("🚨 " + arrivalText + " 후 도착!");
                    binding.tvArrivalTime.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
                } else if (arrival.getArrTime() <= 5) {
                    // 5분 이하면 주황색
                    binding.tvArrivalTime.setText("⚡ " + arrivalText + " 후 도착");
                    binding.tvArrivalTime.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
                } else {
                    // 일반적인 경우
                    binding.tvArrivalTime.setText("⏰ " + arrivalText + " 후 도착");
                    binding.tvArrivalTime.setTextColor(context.getResources().getColor(R.color.ios_blue));
                }

                // 정류장 수 정보
                String stationText = arrival.getFormattedStationCount();
                if (stationText != null && !stationText.isEmpty()) {
                    binding.tvStationCount.setText("📍 " + stationText + " 전");
                } else {
                    binding.tvStationCount.setText("📍 정류장 정보 없음");
                }

                // 도보 시간과 버스 도착 시간을 비교하여 스마트 알림 표시
                showSmartNotification(bus, arrival);
            } else {
                // 도착 정보가 없는 경우 - 더 구체적인 상태 표시
                binding.tvArrivalTime.setVisibility(View.GONE);
                binding.tvStationCount.setVisibility(View.GONE);
                binding.progressBarItem.setVisibility(View.GONE);
                binding.tvErrorState.setVisibility(View.VISIBLE);

                // 현재 시간을 기준으로 상태 메시지 결정
                java.util.Calendar cal = java.util.Calendar.getInstance();
                int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);

                if (hour >= 23 || hour < 5) {
                    // 심야 시간대
                    binding.tvErrorState.setText("🌙 심야 운행 종료");
                } else if (hour >= 5 && hour < 7) {
                    // 이른 아침
                    binding.tvErrorState.setText("🌅 운행 준비 중");
                } else {
                    // 일반 운행 시간대
                    binding.tvErrorState.setText("🚌 다음 버스 대기 중");
                }
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

        /**
         * 스마트 알림 표시 - 도보 시간과 버스 도착 시간을 비교
         */
        private void showSmartNotification(RegisteredBus bus, BusArrival arrival) {
            // 현재 위치 가져오기
            Location currentLocation = locationService.getLastLocation();
            if (currentLocation == null) return;

            // 도보 시간 계산 (백그라운드에서)
            new Thread(() -> {
                try {
                    Future<Integer> walkingTimeFuture = walkingTimeCalculator.calculateWalkingTime(
                            currentLocation.getLatitude(), currentLocation.getLongitude(),
                            bus.getLatitude(), bus.getLongitude());

                    int walkingTimeMinutes = walkingTimeFuture.get();
                    int busArrivalMinutes = arrival.getArrTime();

                    // UI 스레드에서 알림 표시
                    binding.getRoot().post(() -> {
                        showTimingAdvice(walkingTimeMinutes, busArrivalMinutes);
                    });

                } catch (Exception e) {
                    // 오류 발생 시 무시
                }
            }).start();
        }

        /**
         * 타이밍 조언 표시
         */
        private void showTimingAdvice(int walkingTime, int busArrival) {
            String advice = "";
            int timeDiff = busArrival - walkingTime;

            if (timeDiff <= 1) {
                advice = "🏃‍♂️ 지금 출발하세요!";
                binding.tvStationCount.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
            } else if (timeDiff <= 3) {
                advice = "⚡ 곧 출발 준비하세요";
                binding.tvStationCount.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
            } else if (timeDiff <= 5) {
                advice = "👍 여유있게 준비하세요";
                binding.tvStationCount.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
            } else {
                advice = "😌 아직 시간 여유가 있어요";
                binding.tvStationCount.setTextColor(context.getResources().getColor(R.color.text_secondary));
            }

            if (!advice.isEmpty()) {
                binding.tvStationCount.setText(binding.tvStationCount.getText() + " • " + advice);
            }
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
