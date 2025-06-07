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
            // 버스 번호
            binding.tvRouteNo.setText(busArrival.getRouteNo() + "번");
            
            // 방향 정보
            if (busArrival.getDirectionName() != null && !busArrival.getDirectionName().isEmpty()) {
                binding.tvDirection.setText("→ " + busArrival.getDirectionName());
            } else {
                binding.tvDirection.setText("방향 정보 없음");
            }
            
            // 도착 시간
            binding.tvArrivalTime.setText(busArrival.getFormattedArrTime());
            
            // 정류장 수
            binding.tvStationCount.setText(busArrival.getFormattedStationCount());
            
            // 노선 유형
            if (busArrival.getRouteTypeName() != null && !busArrival.getRouteTypeName().isEmpty()) {
                binding.tvRouteType.setText(busArrival.getRouteTypeName());
            } else {
                binding.tvRouteType.setText("일반");
            }

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
