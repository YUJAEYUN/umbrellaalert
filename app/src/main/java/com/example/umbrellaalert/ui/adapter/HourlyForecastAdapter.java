package com.example.umbrellaalert.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.umbrellaalert.R;
import com.example.umbrellaalert.data.model.HourlyForecast;

import java.util.ArrayList;
import java.util.List;

/**
 * 시간별 예보 어댑터 (12시간 예보용)
 * 1시간 단위로 날씨 정보를 표시
 */
public class HourlyForecastAdapter extends RecyclerView.Adapter<HourlyForecastAdapter.ViewHolder> {

    private List<HourlyForecast> forecasts = new ArrayList<>();

    public void setForecasts(List<HourlyForecast> forecasts) {
        this.forecasts = forecasts != null ? forecasts : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hourly_forecast, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HourlyForecast forecast = forecasts.get(position);
        holder.bind(forecast);
    }

    @Override
    public int getItemCount() {
        return forecasts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView timeText;
        private final ImageView weatherIcon;
        private final TextView precipitationProbability;
        private final ImageView rainIcon;
        private final TextView temperatureText;
        private final ImageView umbrellaIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            timeText = itemView.findViewById(R.id.time_text);
            weatherIcon = itemView.findViewById(R.id.weather_icon);
            precipitationProbability = itemView.findViewById(R.id.precipitation_probability);
            rainIcon = itemView.findViewById(R.id.rain_icon);
            temperatureText = itemView.findViewById(R.id.temperature_text);
            umbrellaIndicator = itemView.findViewById(R.id.umbrella_indicator);
        }

        public void bind(HourlyForecast forecast) {
            // 시간 표시 (현재 시간이면 "지금" 표시)
            if (forecast.isCurrentHour()) {
                timeText.setText("지금");
                timeText.setTextColor(itemView.getContext().getColor(R.color.ios_blue));
            } else {
                timeText.setText(forecast.getFormattedTime());
                timeText.setTextColor(itemView.getContext().getColor(R.color.text_primary));
            }

            // 날씨 아이콘 설정
            weatherIcon.setImageResource(getWeatherIcon(forecast.getWeatherCondition()));

            // 강수확률 표시
            int precipProb = forecast.getPrecipitationProbability();
            if (precipProb > 0) {
                precipitationProbability.setText(precipProb + "%");
                precipitationProbability.setVisibility(View.VISIBLE);
                
                // 강수확률이 30% 이상이면 빗방울 아이콘 표시
                if (precipProb >= 30) {
                    rainIcon.setVisibility(View.VISIBLE);
                } else {
                    rainIcon.setVisibility(View.GONE);
                }
            } else {
                precipitationProbability.setVisibility(View.INVISIBLE);
                rainIcon.setVisibility(View.GONE);
            }

            // 온도 표시
            temperatureText.setText(Math.round(forecast.getTemperature()) + "°");

            // 우산 필요 표시
            if (forecast.isNeedUmbrella()) {
                umbrellaIndicator.setVisibility(View.VISIBLE);
            } else {
                umbrellaIndicator.setVisibility(View.GONE);
            }

            // 현재 시간이면 배경 강조
            if (forecast.isCurrentHour()) {
                itemView.setBackgroundColor(itemView.getContext().getColor(R.color.ios_fill));
            } else {
                itemView.setBackground(itemView.getContext().getDrawable(android.R.drawable.list_selector_background));
            }
        }

        /**
         * 날씨 상태에 따른 아이콘 리소스 반환 (3가지 날씨)
         */
        private int getWeatherIcon(String weatherCondition) {
            if (weatherCondition == null) {
                return R.drawable.ic_sunny;
            }

            // 3가지 날씨 상황에 맞춘 아이콘
            if (weatherCondition.contains("비")) {
                return R.drawable.ic_rainy;
            } else if (weatherCondition.contains("흐림")) {
                return R.drawable.ic_cloudy;
            } else {
                // 맑음 (기본값)
                return R.drawable.ic_sunny;
            }
        }
    }
}
