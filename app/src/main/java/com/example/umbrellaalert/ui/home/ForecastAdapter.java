package com.example.umbrellaalert.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.umbrellaalert.R;
import java.util.ArrayList;
import java.util.List;

/**
 * 예보 목록을 표시하기 위한 RecyclerView 어댑터
 */
public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder> {

    private List<Object> forecasts = new ArrayList<>();

    @NonNull
    @Override
    public ForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_forecast, parent, false);
        return new ForecastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastViewHolder holder, int position) {
        // 현재는 예보 데이터를 사용하지 않음
        holder.bind(null);
    }

    @Override
    public int getItemCount() {
        return forecasts.size();
    }

    /**
     * 예보 목록 설정
     */
    public void setForecasts(List<Object> forecasts) {
        this.forecasts = forecasts;
        notifyDataSetChanged();
    }

    /**
     * 예보 항목 ViewHolder
     */
    static class ForecastViewHolder extends RecyclerView.ViewHolder {
        private final TextView timeText;
        private final ImageView weatherIcon;
        private final TextView conditionText;
        private final TextView tempText;
        private final TextView popText;

        public ForecastViewHolder(@NonNull View itemView) {
            super(itemView);
            timeText = itemView.findViewById(R.id.forecast_time);
            weatherIcon = itemView.findViewById(R.id.forecast_icon);
            conditionText = itemView.findViewById(R.id.forecast_condition);
            tempText = itemView.findViewById(R.id.forecast_temp);
            popText = itemView.findViewById(R.id.forecast_pop);
        }

        /**
         * 예보 데이터 바인딩
         */
        public void bind(Object forecast) {
            // 현재는 예보 데이터를 사용하지 않으므로 기본값 표시
            timeText.setText("--:--");
            weatherIcon.setImageResource(R.drawable.ic_weather_sunny);
            conditionText.setText("예보 준비중");
            tempText.setText("--°");
            popText.setText("--%");
        }
    }
}
