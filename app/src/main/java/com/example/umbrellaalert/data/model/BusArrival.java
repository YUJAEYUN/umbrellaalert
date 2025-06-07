package com.example.umbrellaalert.data.model;

/**
 * 버스 도착 예정 정보 모델
 */
public class BusArrival {
    private String nodeId;          // 정류소ID
    private String routeId;         // 노선ID
    private String routeNo;         // 노선번호
    private String routeType;       // 노선유형
    private int arrPrevStationCnt;  // 도착예정정류장수
    private int arrTime;            // 도착예정시간(분)
    private String vehicleNo;       // 차량번호
    private String directionName;   // 방향
    private String routeTypeName;   // 노선유형명
    private String routeTypeCode;   // 노선유형코드

    public BusArrival() {}

    public BusArrival(String nodeId, String routeId, String routeNo, int arrTime) {
        this.nodeId = nodeId;
        this.routeId = routeId;
        this.routeNo = routeNo;
        this.arrTime = arrTime;
    }

    // Getters and Setters
    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
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

    public int getArrPrevStationCnt() {
        return arrPrevStationCnt;
    }

    public void setArrPrevStationCnt(int arrPrevStationCnt) {
        this.arrPrevStationCnt = arrPrevStationCnt;
    }

    public int getArrTime() {
        return arrTime;
    }

    public void setArrTime(int arrTime) {
        this.arrTime = arrTime;
    }

    public String getVehicleNo() {
        return vehicleNo;
    }

    public void setVehicleNo(String vehicleNo) {
        this.vehicleNo = vehicleNo;
    }

    public String getDirectionName() {
        return directionName;
    }

    public void setDirectionName(String directionName) {
        this.directionName = directionName;
    }

    public String getRouteTypeName() {
        return routeTypeName;
    }

    public void setRouteTypeName(String routeTypeName) {
        this.routeTypeName = routeTypeName;
    }

    public String getRouteTypeCode() {
        return routeTypeCode;
    }

    public void setRouteTypeCode(String routeTypeCode) {
        this.routeTypeCode = routeTypeCode;
    }

    /**
     * 도착 시간을 분 단위로 포맷팅
     */
    public String getFormattedArrTime() {
        if (arrTime <= 0) {
            return "곧 도착";
        } else if (arrTime == 1) {
            return "1분 후";
        } else {
            return arrTime + "분 후";
        }
    }

    /**
     * 정류장 수 정보 포맷팅
     */
    public String getFormattedStationCount() {
        if (arrPrevStationCnt <= 0) {
            return "도착 중";
        } else if (arrPrevStationCnt == 1) {
            return "1정거장 전";
        } else {
            return arrPrevStationCnt + "정거장 전";
        }
    }

    @Override
    public String toString() {
        return "BusArrival{" +
                "nodeId='" + nodeId + '\'' +
                ", routeId='" + routeId + '\'' +
                ", routeNo='" + routeNo + '\'' +
                ", routeType='" + routeType + '\'' +
                ", arrPrevStationCnt=" + arrPrevStationCnt +
                ", arrTime=" + arrTime +
                ", vehicleNo='" + vehicleNo + '\'' +
                ", directionName='" + directionName + '\'' +
                ", routeTypeName='" + routeTypeName + '\'' +
                ", routeTypeCode='" + routeTypeCode + '\'' +
                '}';
    }
}
