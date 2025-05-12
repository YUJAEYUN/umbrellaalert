package com.example.umbrellaalert.ui.location;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.umbrellaalert.data.model.Location;
import com.example.umbrellaalert.databinding.ItemLocationBinding;

import java.util.ArrayList;
import java.util.List;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {

    private List<Location> locations;
    private LocationListener listener;

    // 위치 관련 이벤트 리스너
    public interface LocationListener {
        void onLocationDelete(Location location);
        void onLocationToggleNotification(Location location);
    }

    public LocationAdapter(LocationListener listener) {
        this.locations = new ArrayList<>();
        this.listener = listener;
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLocationBinding binding = ItemLocationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new LocationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        Location location = locations.get(position);
        holder.bind(location);
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    class LocationViewHolder extends RecyclerView.ViewHolder {

        private final ItemLocationBinding binding;

        public LocationViewHolder(ItemLocationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final Location location) {
            // 위치 정보 표시
            binding.locationName.setText(location.getName());
            binding.locationCoordinates.setText(
                    String.format("위도: %.6f, 경도: %.6f", location.getLatitude(), location.getLongitude()));

            // 자주 가는 장소 표시
            binding.frequentIndicator.setVisibility(location.isFrequent() ? View.VISIBLE : View.GONE);

            // 알림 스위치 설정
            binding.switchNotification.setChecked(location.isNotificationEnabled());
            binding.switchNotification.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (listener != null) {
                        listener.onLocationToggleNotification(location);
                    }
                }
            });

            // 삭제 버튼
            binding.btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onLocationDelete(location);
                    }
                }
            });
        }
    }
}