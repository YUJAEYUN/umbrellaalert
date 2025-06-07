package com.example.umbrellaalert.data.model;

/**
 * 버스정류장 정보 모델
 */
public class BusStop {
    private String nodeId;          // 정류소ID (국토교통부 TAGO 버스정류소번호)
    private String nodeName;        // 정류소명
    private double gpsLati;         // WGS84 위도 좌표
    private double gpsLong;         // WGS84 경도 좌표
    private int cityCode;           // 도시코드 (세종: 12, 대전: 25)
    private String nodeNo;          // 정류소번호
    private String routeType;       // 노선유형

    public BusStop() {}

    public BusStop(String nodeId, String nodeName, double gpsLati, double gpsLong, int cityCode) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.gpsLati = gpsLati;
        this.gpsLong = gpsLong;
        this.cityCode = cityCode;
    }

    // Getters and Setters
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

    public double getGpsLati() {
        return gpsLati;
    }

    public void setGpsLati(double gpsLati) {
        this.gpsLati = gpsLati;
    }

    public double getGpsLong() {
        return gpsLong;
    }

    public void setGpsLong(double gpsLong) {
        this.gpsLong = gpsLong;
    }

    public int getCityCode() {
        return cityCode;
    }

    public void setCityCode(int cityCode) {
        this.cityCode = cityCode;
    }

    public String getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(String nodeNo) {
        this.nodeNo = nodeNo;
    }

    public String getRouteType() {
        return routeType;
    }

    public void setRouteType(String routeType) {
        this.routeType = routeType;
    }

    @Override
    public String toString() {
        return "BusStop{" +
                "nodeId='" + nodeId + '\'' +
                ", nodeName='" + nodeName + '\'' +
                ", gpsLati=" + gpsLati +
                ", gpsLong=" + gpsLong +
                ", cityCode=" + cityCode +
                ", nodeNo='" + nodeNo + '\'' +
                ", routeType='" + routeType + '\'' +
                '}';
    }
}
