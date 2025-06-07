package com.example.umbrellaalert.data.model;

/**
 * 사용자가 등록한 버스 정보 모델
 */
public class RegisteredBus {
    private int id;                 // 로컬 DB ID
    private String nodeId;          // 정류소ID
    private String nodeName;        // 정류소명
    private String routeId;         // 노선ID
    private String routeNo;         // 노선번호
    private String routeType;       // 노선유형
    private String directionName;   // 방향
    private int cityCode;           // 도시코드
    private double latitude;        // 정류장 위도
    private double longitude;       // 정류장 경도
    private long createdAt;         // 등록 시간
    private boolean isActive;       // 활성화 상태
    private String alias;           // 사용자 지정 별명

    public RegisteredBus() {}

    public RegisteredBus(String nodeId, String nodeName, String routeId, String routeNo,
                        String directionName, int cityCode) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.routeId = routeId;
        this.routeNo = routeNo;
        this.directionName = directionName;
        this.cityCode = cityCode;
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;
    }

    public RegisteredBus(String nodeId, String nodeName, String routeId, String routeNo,
                        String directionName, int cityCode, double latitude, double longitude) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.routeId = routeId;
        this.routeNo = routeNo;
        this.directionName = directionName;
        this.cityCode = cityCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getRouteNo() {
        return routeNo;
    }

    public void setRouteNo(String routeNo) {
        this.routeNo = routeNo;
    }

    public String getRouteType() {
        return routeType;
    }

    public void setRouteType(String routeType) {
        this.routeType = routeType;
    }

    public String getDirectionName() {
        return directionName;
    }

    public void setDirectionName(String directionName) {
        this.directionName = directionName;
    }

    public int getCityCode() {
        return cityCode;
    }

    public void setCityCode(int cityCode) {
        this.cityCode = cityCode;
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * 표시용 이름 반환 (별명이 있으면 별명, 없으면 기본 정보)
     */
    public String getDisplayName() {
        if (alias != null && !alias.trim().isEmpty()) {
            return alias;
        }
        return routeNo + "번 (" + nodeName + ")";
    }

    /**
     * 도시명 반환
     */
    public String getCityName() {
        switch (cityCode) {
            case 12:
                return "세종";
            case 25:
                return "대전";
            default:
                return "알 수 없음";
        }
    }

    @Override
    public String toString() {
        return "RegisteredBus{" +
                "id=" + id +
                ", nodeId='" + nodeId + '\'' +
                ", nodeName='" + nodeName + '\'' +
                ", routeId='" + routeId + '\'' +
                ", routeNo='" + routeNo + '\'' +
                ", routeType='" + routeType + '\'' +
                ", directionName='" + directionName + '\'' +
                ", cityCode=" + cityCode +
                ", createdAt=" + createdAt +
                ", isActive=" + isActive +
                ", alias='" + alias + '\'' +
                '}';
    }
}
