package com.example.umbrellaalert.data.model;

/**
 * 기상청 예보 정보 모델 (초단기예보, 단기예보)
 */
public class KmaForecast {
    private String forecastDate; // 예보 일자
    private String forecastTime; // 예보 시각
    private float temperature; // 기온 (°C)
    private float precipitation; // 1시간 강수량 (mm)
    private int precipitationProbability; // 강수확률 (%)
    private int humidity; // 습도 (%)
    private float windSpeed; // 풍속 (m/s)
    private int precipitationType; // 강수형태 (0:없음, 1:비, 2:비/눈, 3:눈, 4:소나기)
    private String weatherCondition; // 날씨 상태 (Clear, Rain, Snow 등)
    private boolean needUmbrella; // 우산 필요 여부

    public KmaForecast() {
        // 기본 생성자
    }

    // Getters and Setters
    public String getForecastDate() {
        return forecastDate;
    }

    public void setForecastDate(String forecastDate) {
        this.forecastDate = forecastDate;
    }

    public String getForecastTime() {
        return forecastTime;
    }

    public void setForecastTime(String forecastTime) {
        this.forecastTime = forecastTime;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public float getPrecipitation() {
        return precipitation;
    }

    public void setPrecipitation(float precipitation) {
        this.precipitation = precipitation;
    }

    public int getPrecipitationProbability() {
        return precipitationProbability;
    }

    public void setPrecipitationProbability(int precipitationProbability) {
        this.precipitationProbability = precipitationProbability;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public float getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(float windSpeed) {
        this.windSpeed = windSpeed;
    }

    public int getPrecipitationType() {
        return precipitationType;
    }

    public void setPrecipitationType(int precipitationType) {
        this.precipitationType = precipitationType;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }

    public void setWeatherCondition(String weatherCondition) {
        this.weatherCondition = weatherCondition;
    }

    public boolean isNeedUmbrella() {
        return needUmbrella;
    }

    public void setNeedUmbrella(boolean needUmbrella) {
        this.needUmbrella = needUmbrella;
    }

    /**
     * 예보 시간을 가독성 있는 형식으로 변환 (예: "0900" -> "09:00")
     */
    public String getFormattedTime() {
        if (forecastTime == null || forecastTime.length() != 4) {
            return forecastTime;
        }
        return forecastTime.substring(0, 2) + ":" + forecastTime.substring(2);
    }

    /**
     * 예보 날짜를 가독성 있는 형식으로 변환 (예: "20230501" -> "05/01")
     */
    public String getFormattedDate() {
        if (forecastDate == null || forecastDate.length() != 8) {
            return forecastDate;
        }
        return forecastDate.substring(4, 6) + "/" + forecastDate.substring(6);
    }

    @Override
    public String toString() {
        return "KmaForecast{" +
                "forecastDate='" + forecastDate + '\'' +
                ", forecastTime='" + forecastTime + '\'' +
                ", temperature=" + temperature +
                ", precipitation=" + precipitation +
                ", precipitationProbability=" + precipitationProbability +
                ", humidity=" + humidity +
                ", windSpeed=" + windSpeed +
                ", precipitationType=" + precipitationType +
                ", weatherCondition='" + weatherCondition + '\'' +
                ", needUmbrella=" + needUmbrella +
                '}';
    }
}
