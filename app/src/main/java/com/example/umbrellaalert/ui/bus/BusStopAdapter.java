package com.example.umbrellaalert.ui.bus;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.umbrellaalert.data.model.BusStop;
import com.example.umbrellaalert.databinding.ItemBusStopBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * 정류장 목록을 표시하는 RecyclerView 어댑터
 */
public class BusStopAdapter extends RecyclerView.Adapter<BusStopAdapter.BusStopViewHolder> {
    
    private List<BusStop> busStops = new ArrayList<>();
    private OnBusStopClickListener onBusStopClickListener;

    public interface OnBusStopClickListener {
        void onBusStopClick(BusStop busStop);
    }

    @NonNull
    @Override
    public BusStopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBusStopBinding binding = ItemBusStopBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new BusStopViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BusStopViewHolder holder, int position) {
        BusStop busStop = busStops.get(position);
        holder.bind(busStop);
    }

    @Override
    public int getItemCount() {
        return busStops.size();
    }

    /**
     * 정류장 목록 업데이트
     */
    public void updateBusStops(List<BusStop> newBusStops) {
        this.busStops.clear();
        if (newBusStops != null) {
            this.busStops.addAll(newBusStops);
        }
        notifyDataSetChanged();
    }

    /**
     * 클릭 리스너 설정
     */
    public void setOnBusStopClickListener(OnBusStopClickListener listener) {
        this.onBusStopClickListener = listener;
    }

    class BusStopViewHolder extends RecyclerView.ViewHolder {
        private final ItemBusStopBinding binding;

        public BusStopViewHolder(@NonNull ItemBusStopBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(BusStop busStop) {
            // 정류장 정보 설정
            binding.tvStopName.setText(busStop.getNodeName());
            
            // 정류장 번호가 있으면 표시
            if (busStop.getNodeNo() != null && !busStop.getNodeNo().isEmpty()) {
                binding.tvStopNumber.setText("정류장 번호: " + busStop.getNodeNo());
            } else {
                binding.tvStopNumber.setText("");
            }
            
            // 도시 정보 표시
            String cityName = getCityName(busStop.getCityCode());
            binding.tvCityName.setText(cityName);
            
            // 거리 계산 (임시로 좌표 표시)
            String coordinates = String.format("%.4f, %.4f", busStop.getGpsLati(), busStop.getGpsLong());
            binding.tvDistance.setText(coordinates);

            // 클릭 리스너
            binding.getRoot().setOnClickListener(v -> {
                if (onBusStopClickListener != null) {
                    onBusStopClickListener.onBusStopClick(busStop);
                }
            });
        }

        private String getCityName(int cityCode) {
            switch (cityCode) {
                case 12:
                    return "세종";
                case 25:
                    return "대전";
                default:
                    return "알 수 없음";
            }
        }
    }
}
