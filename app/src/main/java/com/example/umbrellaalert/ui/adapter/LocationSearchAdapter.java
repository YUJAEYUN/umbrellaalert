package com.example.umbrellaalert.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.umbrellaalert.R;
import com.example.umbrellaalert.data.model.SearchLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 장소 검색 결과를 표시하는 어댑터
 */
public class LocationSearchAdapter extends RecyclerView.Adapter<LocationSearchAdapter.ViewHolder> {

    private List<SearchLocation> searchResults = new ArrayList<>();
    private OnLocationSelectedListener listener;

    public interface OnLocationSelectedListener {
        void onLocationSelected(SearchLocation location);
    }

    public LocationSearchAdapter(OnLocationSelectedListener listener) {
        this.listener = listener;
    }

    public void setSearchResults(List<SearchLocation> results) {
        this.searchResults = results != null ? results : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_location, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchLocation location = searchResults.get(position);
        holder.bind(location);
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textPlaceName;
        private TextView textPlaceAddress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textPlaceName = itemView.findViewById(R.id.text_place_name);
            textPlaceAddress = itemView.findViewById(R.id.text_place_address);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onLocationSelected(searchResults.get(position));
                }
            });
        }

        public void bind(SearchLocation location) {
            textPlaceName.setText(location.getName());
            textPlaceAddress.setText(location.getAddress());
        }
    }
}
