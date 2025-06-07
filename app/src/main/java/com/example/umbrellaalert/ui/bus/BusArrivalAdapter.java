package com.example.umbrellaalert.ui.bus;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.umbrellaalert.data.model.BusArrival;
import com.example.umbrellaalert.databinding.ItemBusArrivalBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * 버스 도착 정보를 표시하는 RecyclerView 어댑터
 */
public class BusArrivalAdapter extends RecyclerView.Adapter<BusArrivalAdapter.BusArrivalViewHolder> {
    
    private List<BusArrival> busArrivals = new ArrayList<>();
    private OnBusArrivalClickListener onBusArrivalClickListener;

    public interface OnBusArrivalClickListener {
        void onBusArrivalClick(BusArrival busArrival);
    }

    @NonNull
    @Override
    public BusArrivalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBusArrivalBinding binding = ItemBusArrivalBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new BusArrivalViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BusArrivalViewHolder holder, int position) {
        BusArrival busArrival = busArrivals.get(position);
        holder.bind(busArrival);
    }

    @Override
    public int getItemCount() {
        return busArrivals.size();
    }

    /**
     * 버스 도착 정보 목록 업데이트
     */
    public void updateBusArrivals(List<BusArrival> newBusArrivals) {
        this.busArrivals.clear();
        if (newBusArrivals != null) {
            this.busArrivals.addAll(newBusArrivals);
        }
        notifyDataSetChanged();
    }

    /**
     * 클릭 리스너 설정
     */
    public void setOnBusArrivalClickListener(OnBusArrivalClickListener listener) {
        this.onBusArrivalClickListener = listener;
    }

    class BusArrivalViewHolder extends RecyclerView.ViewHolder {
        private final ItemBusArrivalBinding binding;

        public BusArrivalViewHolder(@NonNull ItemBusArrivalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(BusArrival busArrival) {
            // 버스 번호와 노선 유형을 함께 표시
            String routeInfo = busArrival.getRouteNo() + "번";
            if (busArrival.getRouteTypeName() != null && !busArrival.getRouteTypeName().isEmpty()) {
                routeInfo += " (" + busArrival.getRouteTypeName() + ")";
            }
            binding.tvRouteNo.setText(routeInfo);

            // 방향 정보를 더 명확하게 표시
            if (busArrival.getDirectionName() != null && !busArrival.getDirectionName().isEmpty()) {
                // 방향 정보에 화살표와 함께 더 명확한 표시
                String directionText = "🚌 " + busArrival.getDirectionName() + " 방면";
                binding.tvDirection.setText(directionText);
            } else {
                binding.tvDirection.setText("🚌 방향 정보 확인 중");
            }

            // 도착 시간을 더 직관적으로 표시
            String arrivalText = busArrival.getFormattedArrTime();
            if (arrivalText.contains("분")) {
                binding.tvArrivalTime.setText("⏰ " + arrivalText);
            } else {
                binding.tvArrivalTime.setText("⏰ " + arrivalText);
            }

            // 정류장 수를 더 명확하게 표시
            String stationText = busArrival.getFormattedStationCount();
            if (stationText != null && !stationText.isEmpty()) {
                binding.tvStationCount.setText("📍 " + stationText);
            } else {
                binding.tvStationCount.setText("📍 정류장 정보 없음");
            }

            // 노선 유형은 이미 버스 번호와 함께 표시했으므로 제거하거나 다른 정보로 활용
            binding.tvRouteType.setText(""); // 또는 다른 유용한 정보로 대체

            // 클릭 리스너
            binding.getRoot().setOnClickListener(v -> {
                if (onBusArrivalClickListener != null) {
                    onBusArrivalClickListener.onBusArrivalClick(busArrival);
                }
            });

            // 등록 버튼 클릭 리스너
            binding.btnRegister.setOnClickListener(v -> {
                if (onBusArrivalClickListener != null) {
                    onBusArrivalClickListener.onBusArrivalClick(busArrival);
                }
            });
        }
    }
}
