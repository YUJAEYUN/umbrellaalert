package com.example.umbrellaalert.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.umbrellaalert.R;
import com.example.umbrellaalert.data.model.KmaForecast;

import java.util.ArrayList;
import java.util.List;

/**
 * 예보 목록을 표시하기 위한 RecyclerView 어댑터
 */
public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder> {

    private List<KmaForecast> forecasts = new ArrayList<>();

    @NonNull
    @Override
    public ForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_forecast, parent, false);
        return new ForecastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastViewHolder holder, int position) {
        KmaForecast forecast = forecasts.get(position);
        holder.bind(forecast);
    }

    @Override
    public int getItemCount() {
        return forecasts.size();
    }

    /**
     * 예보 목록 설정
     */
    public void setForecasts(List<KmaForecast> forecasts) {
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
        public void bind(KmaForecast forecast) {
            // 시간 표시 (예: "09:00")
            timeText.setText(forecast.getFormattedTime());

            // 날씨 상태에 따른 아이콘 설정
            if (forecast.isNeedUmbrella()) {
                if (forecast.getWeatherCondition().contains("Snow")) {
                    weatherIcon.setImageResource(R.drawable.ic_weather_snow);
                } else {
                    weatherIcon.setImageResource(R.drawable.ic_weather_rainy);
                }
            } else {
                if (forecast.getWeatherCondition().contains("Cloud")) {
                    weatherIcon.setImageResource(R.drawable.ic_weather_cloudy);
                } else {
                    weatherIcon.setImageResource(R.drawable.ic_weather_sunny);
                }
            }

            // 날씨 상태 텍스트
            String condition = getWeatherConditionText(forecast.getWeatherCondition());
            conditionText.setText(condition);

            // 온도
            tempText.setText(String.format("%.1f°C", forecast.getTemperature()));

            // 강수확률
            popText.setText(String.format("%d%%", forecast.getPrecipitationProbability()));
        }

        /**
         * 날씨 상태 텍스트 변환
         */
        private String getWeatherConditionText(String condition) {
            if (condition.equalsIgnoreCase("Clear")) {
                return "맑음";
            } else if (condition.equalsIgnoreCase("Clouds")) {
                return "구름";
            } else if (condition.equalsIgnoreCase("Partly Cloudy")) {
                return "구름조금";
            } else if (condition.equalsIgnoreCase("Rain")) {
                return "비";
            } else if (condition.equalsIgnoreCase("Rain/Snow")) {
                return "비/눈";
            } else if (condition.equalsIgnoreCase("Snow")) {
                return "눈";
            } else {
                return condition;
            }
        }
    }
}
