package com.example.umbrellaalert.data.model;

public class Weather {
    private int id;
    private float temperature;
    private String weatherCondition;
    private float precipitation;
    private int humidity;
    private float windSpeed;
    private String location;
    private long timestamp;
    private boolean needUmbrella;

    // 생성자
    public Weather(int id, float temperature, String weatherCondition, float precipitation,
                   int humidity, float windSpeed, String location, long timestamp, boolean needUmbrella) {
        this.id = id;
        this.temperature = temperature;
        this.weatherCondition = weatherCondition;
        this.precipitation = precipitation;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.location = location;
        this.timestamp = timestamp;
        this.needUmbrella = needUmbrella;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }

    public void setWeatherCondition(String weatherCondition) {
        this.weatherCondition = weatherCondition;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isNeedUmbrella() {
        return needUmbrella;
    }

    public void setNeedUmbrella(boolean needUmbrella) {
        this.needUmbrella = needUmbrella;
    }
}