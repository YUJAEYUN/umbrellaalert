package com.example.umbrellaalert.ui.bus;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.umbrellaalert.data.model.BusStop;
import com.example.umbrellaalert.databinding.ItemBusStopBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
     * 정류장 목록 업데이트 (같은 이름의 정류장들을 방향별로 정렬)
     */
    public void updateBusStops(List<BusStop> newBusStops) {
        this.busStops.clear();
        if (newBusStops != null) {
            // 같은 이름의 정류장들을 그룹화하고 방향별로 정렬
            List<BusStop> sortedStops = sortBusStopsByNameAndDirection(newBusStops);
            this.busStops.addAll(sortedStops);
        }
        notifyDataSetChanged();
    }

    /**
     * 정류장을 이름과 방향별로 정렬
     */
    private List<BusStop> sortBusStopsByNameAndDirection(List<BusStop> stops) {
        List<BusStop> sortedStops = new ArrayList<>(stops);

        // 정류장명으로 그룹화한 후 방향별로 정렬
        Collections.sort(sortedStops, new Comparator<BusStop>() {
            @Override
            public int compare(BusStop stop1, BusStop stop2) {
                // 먼저 정류장명으로 정렬
                int nameCompare = stop1.getNodeName().compareTo(stop2.getNodeName());
                if (nameCompare != 0) {
                    return nameCompare;
                }

                // 같은 이름이면 방향으로 정렬 (좌표 기반)
                String direction1 = getDirectionFromCoordinatesStatic(stop1.getGpsLati(), stop1.getGpsLong());
                String direction2 = getDirectionFromCoordinatesStatic(stop2.getGpsLati(), stop2.getGpsLong());
                return direction1.compareTo(direction2);
            }
        });

        return sortedStops;
    }

    /**
     * 좌표를 기반으로 대략적인 방향 정보 생성 (static 메서드)
     */
    private static String getDirectionFromCoordinatesStatic(double lat, double lng) {
        // 세종시와 대전시의 주요 도로 방향을 고려한 방향 정보
        double sejongCenterLat = 36.4800;
        double sejongCenterLng = 127.2890;
        double daejeonCenterLat = 36.3504;
        double daejeonCenterLng = 127.3845;

        // 가장 가까운 도심과의 상대적 위치로 방향 결정
        double sejongDistance = Math.sqrt(Math.pow(lat - sejongCenterLat, 2) + Math.pow(lng - sejongCenterLng, 2));
        double daejeonDistance = Math.sqrt(Math.pow(lat - daejeonCenterLat, 2) + Math.pow(lng - daejeonCenterLng, 2));

        double centerLat, centerLng;
        if (sejongDistance < daejeonDistance) {
            centerLat = sejongCenterLat;
            centerLng = sejongCenterLng;
        } else {
            centerLat = daejeonCenterLat;
            centerLng = daejeonCenterLng;
        }

        // 중심점 대비 상대적 위치로 방향 결정
        double latDiff = lat - centerLat;
        double lngDiff = lng - centerLng;

        // 8방향으로 구분
        double angle = Math.atan2(latDiff, lngDiff) * 180 / Math.PI;
        if (angle < 0) angle += 360;

        if (angle >= 337.5 || angle < 22.5) {
            return "동쪽";
        } else if (angle >= 22.5 && angle < 67.5) {
            return "북동쪽";
        } else if (angle >= 67.5 && angle < 112.5) {
            return "북쪽";
        } else if (angle >= 112.5 && angle < 157.5) {
            return "북서쪽";
        } else if (angle >= 157.5 && angle < 202.5) {
            return "서쪽";
        } else if (angle >= 202.5 && angle < 247.5) {
            return "남서쪽";
        } else if (angle >= 247.5 && angle < 292.5) {
            return "남쪽";
        } else {
            return "남동쪽";
        }
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

            // 정류장 번호와 방향 정보를 함께 표시
            StringBuilder infoBuilder = new StringBuilder();

            if (busStop.getNodeNo() != null && !busStop.getNodeNo().isEmpty()) {
                infoBuilder.append("정류장 번호: ").append(busStop.getNodeNo());
            }

            // 방향 정보 추가 (좌표 기반으로 대략적인 방향 표시)
            String direction = getDirectionFromCoordinatesStatic(busStop.getGpsLati(), busStop.getGpsLong());
            if (!direction.isEmpty()) {
                if (infoBuilder.length() > 0) {
                    infoBuilder.append(" • ");
                }
                infoBuilder.append(direction).append(" 방향");
            }

            binding.tvStopNumber.setText(infoBuilder.toString());

            // 도시 정보 표시
            String cityName = getCityName(busStop.getCityCode());
            binding.tvCityName.setText(cityName);

            // 거리 정보 표시 (좌표 대신 실제 거리로 개선 가능)
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
