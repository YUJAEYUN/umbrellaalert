package com.example.umbrellaalert.data.model;

public class WeatherForecast {
    private String baseDate;      // 발표일자 (YYYYMMDD)
    private String baseTime;      // 발표시각 (HHMM)
    private String category;      // 자료구분문자
    private String fcstDate;      // 예보일자 (YYYYMMDD)
    private String fcstTime;      // 예보시각 (HHMM)
    private String fcstValue;     // 예보값
    private int nx;               // 예보지점 X 좌표
    private int ny;               // 예보지점 Y 좌표

    // 기본 생성자
    public WeatherForecast() {}

    // 전체 생성자
    public WeatherForecast(String baseDate, String baseTime, String category, 
                          String fcstDate, String fcstTime, String fcstValue, 
                          int nx, int ny) {
        this.baseDate = baseDate;
        this.baseTime = baseTime;
        this.category = category;
        this.fcstDate = fcstDate;
        this.fcstTime = fcstTime;
        this.fcstValue = fcstValue;
        this.nx = nx;
        this.ny = ny;
    }

    // Getter와 Setter 메서드들
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getFcstDate() {
        return fcstDate;
    }

    public void setFcstDate(String fcstDate) {
        this.fcstDate = fcstDate;
    }

    public String getFcstTime() {
        return fcstTime;
    }

    public void setFcstTime(String fcstTime) {
        this.fcstTime = fcstTime;
    }

    public String getFcstValue() {
        return fcstValue;
    }

    public void setFcstValue(String fcstValue) {
        this.fcstValue = fcstValue;
    }

    public int getNx() {
        return nx;
    }

    public void setNx(int nx) {
        this.nx = nx;
    }

    public int getNy() {
        return ny;
    }

    public void setNy(int ny) {
        this.ny = ny;
    }

    @Override
    public String toString() {
        return "WeatherForecast{" +
                "baseDate='" + baseDate + '\'' +
                ", baseTime='" + baseTime + '\'' +
                ", category='" + category + '\'' +
                ", fcstDate='" + fcstDate + '\'' +
                ", fcstTime='" + fcstTime + '\'' +
                ", fcstValue='" + fcstValue + '\'' +
                ", nx=" + nx +
                ", ny=" + ny +
                '}';
    }
}