package com.example.umbrellaalert.data.model;

/**
 * 기상청 초단기실황 날씨 정보 모델
 */
public class KmaWeather {
    private String baseDate; // 발표 일자
    private String baseTime; // 발표 시각
    private float temperature; // 기온 (°C)
    private float precipitation; // 1시간 강수량 (mm)
    private int humidity; // 습도 (%)
    private float windSpeed; // 풍속 (m/s)
    private int precipitationType; // 강수형태 (0:없음, 1:비, 2:비/눈, 3:눈, 4:소나기)
    private String weatherCondition; // 날씨 상태 (Clear, Rain, Snow 등)
    private boolean needUmbrella; // 우산 필요 여부

    public KmaWeather() {
        // 기본 생성자
    }

    // Getters and Setters
    public String getBaseDate() {
        return baseDate;
    }

    public void setBaseDate(String baseDate) {
        this.baseDate = baseDate;
    }

    public String getBaseTime() {
        return baseTime;
    }

    public void setBaseTime(String baseTime) {
        this.baseTime = baseTime;
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

    @Override
    public String toString() {
        return "KmaWeather{" +
                "baseDate='" + baseDate + '\'' +
                ", baseTime='" + baseTime + '\'' +
                ", temperature=" + temperature +
                ", precipitation=" + precipitation +
                ", humidity=" + humidity +
                ", windSpeed=" + windSpeed +
                ", precipitationType=" + precipitationType +
                ", weatherCondition='" + weatherCondition + '\'' +
                ", needUmbrella=" + needUmbrella +
                '}';
    }
}
