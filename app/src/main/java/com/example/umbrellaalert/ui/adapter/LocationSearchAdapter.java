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
public class LocationSearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_LOCATION = 0;
    private static final int TYPE_LOADING = 1;
    private static final int TYPE_NO_RESULTS = 2;

    private List<SearchLocation> searchResults = new ArrayList<>();
    private OnLocationSelectedListener listener;
    private boolean isLoading = false;
    private boolean showNoResults = false;

    public interface OnLocationSelectedListener {
        void onLocationSelected(SearchLocation location);
    }

    public LocationSearchAdapter(OnLocationSelectedListener listener) {
        this.listener = listener;
    }

    public void setSearchResults(List<SearchLocation> results) {
        this.searchResults = results != null ? results : new ArrayList<>();
        this.isLoading = false;
        this.showNoResults = false;
        notifyDataSetChanged();
    }

    public void setLoading(boolean loading) {
        this.isLoading = loading;
        this.showNoResults = false;
        if (loading) {
            this.searchResults.clear();
        }
        notifyDataSetChanged();
    }

    public void setNoResults(boolean noResults) {
        this.showNoResults = noResults;
        this.isLoading = false;
        if (noResults) {
            this.searchResults.clear();
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoading) {
            return TYPE_LOADING;
        } else if (showNoResults) {
            return TYPE_NO_RESULTS;
        } else {
            return TYPE_LOCATION;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_LOADING:
                View loadingView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_search_loading, parent, false);
                return new LoadingViewHolder(loadingView);
            case TYPE_NO_RESULTS:
                View noResultsView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_search_no_results, parent, false);
                return new NoResultsViewHolder(noResultsView);
            default:
                View locationView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_search_location, parent, false);
                return new LocationViewHolder(locationView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LocationViewHolder && !searchResults.isEmpty()) {
            SearchLocation location = searchResults.get(position);
            ((LocationViewHolder) holder).bind(location);
        }
        // LoadingViewHolder와 NoResultsViewHolder는 별도 바인딩이 필요 없음
    }

    @Override
    public int getItemCount() {
        if (isLoading || showNoResults) {
            return 1; // 로딩 또는 결과 없음 아이템 1개
        }
        return searchResults.size();
    }

    // 장소 검색 결과 ViewHolder
    class LocationViewHolder extends RecyclerView.ViewHolder {
        private TextView textPlaceName;
        private TextView textPlaceAddress;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            textPlaceName = itemView.findViewById(R.id.text_place_name);
            textPlaceAddress = itemView.findViewById(R.id.text_place_address);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null && !searchResults.isEmpty()) {
                    listener.onLocationSelected(searchResults.get(position));
                }
            });
        }

        public void bind(SearchLocation location) {
            textPlaceName.setText(location.getName());
            textPlaceAddress.setText(location.getAddress());
        }
    }

    // 로딩 상태 ViewHolder
    class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    // 검색 결과 없음 ViewHolder
    class NoResultsViewHolder extends RecyclerView.ViewHolder {
        public NoResultsViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
