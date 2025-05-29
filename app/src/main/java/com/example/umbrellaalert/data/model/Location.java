package com.example.umbrellaalert.data.model;

public class Location {
    private int id;
    private String name;
    private double latitude;
    private double longitude;
    private boolean frequent;
    private boolean notificationEnabled;
    private boolean isDefault;

    // 생성자
    public Location(int id, String name, double latitude, double longitude,
                    boolean frequent, boolean notificationEnabled) {
        this(id, name, latitude, longitude, frequent, notificationEnabled, false);
    }

    // 전체 생성자
    public Location(int id, String name, double latitude, double longitude,
                    boolean frequent, boolean notificationEnabled, boolean isDefault) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.frequent = frequent;
        this.notificationEnabled = notificationEnabled;
        this.isDefault = isDefault;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isFrequent() {
        return frequent;
    }

    public void setFrequent(boolean frequent) {
        this.frequent = frequent;
    }

    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    public void setNotificationEnabled(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}