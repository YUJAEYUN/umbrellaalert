package com.example.umbrellaalert.data.model;

/**
 * 장소 검색 결과를 나타내는 모델 클래스
 */
public class SearchLocation {
    private String name;        // 장소 이름
    private String address;     // 주소
    private double latitude;    // 위도
    private double longitude;   // 경도
    private String category;    // 카테고리 (예: 관공서, 마트, 학교 등)

    public SearchLocation() {}

    public SearchLocation(String name, String address, double latitude, double longitude) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public SearchLocation(String name, String address, double latitude, double longitude, String category) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "SearchLocation{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", category='" + category + '\'' +
                '}';
    }
}
