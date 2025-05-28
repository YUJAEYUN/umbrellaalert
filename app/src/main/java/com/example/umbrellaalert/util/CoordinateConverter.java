package com.example.umbrellaalert.util;

/**
 * 기상청 격자 좌표계 변환 유틸리티 클래스
 * 위도/경도를 기상청 격자 좌표(nx, ny)로 변환
 */
public class CoordinateConverter {
    
    // 기상청 격자 좌표계 상수
    private static final double RE = 6371.00877; // 지구 반지름(km)
    private static final double GRID = 5.0; // 격자 간격(km)
    private static final double SLAT1 = 30.0; // 투영 위도1(degree)
    private static final double SLAT2 = 60.0; // 투영 위도2(degree)
    private static final double OLON = 126.0; // 기준점 경도(degree)
    private static final double OLAT = 38.0; // 기준점 위도(degree)
    private static final double XO = 43; // 기준점 X좌표(GRID)
    private static final double YO = 136; // 기준점 Y좌표(GRID)
    
    public static class GridCoordinate {
        public int nx;
        public int ny;
        
        public GridCoordinate(int nx, int ny) {
            this.nx = nx;
            this.ny = ny;
        }
    }
    
    /**
     * 위도/경도를 기상청 격자 좌표로 변환
     * @param lat 위도
     * @param lon 경도
     * @return 격자 좌표 (nx, ny)
     */
    public static GridCoordinate convertToGrid(double lat, double lon) {
        double DEGRAD = Math.PI / 180.0;
        double RADDEG = 180.0 / Math.PI;
        
        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;
        
        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);
        
        double ra = Math.tan(Math.PI * 0.25 + (lat) * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);
        double theta = lon * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;
        
        int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);
        
        return new GridCoordinate(nx, ny);
    }
    
    /**
     * 주요 도시의 격자 좌표 반환
     */
    public static GridCoordinate getGridByCity(String cityName) {
        switch (cityName) {
            case "서울":
                return new GridCoordinate(60, 127);
            case "부산":
                return new GridCoordinate(98, 76);
            case "대구":
                return new GridCoordinate(89, 90);
            case "인천":
                return new GridCoordinate(55, 124);
            case "광주":
                return new GridCoordinate(58, 74);
            case "대전":
                return new GridCoordinate(67, 100);
            case "울산":
                return new GridCoordinate(102, 84);
            case "세종":
                return new GridCoordinate(66, 103);
            case "강릉":
                return new GridCoordinate(92, 131);
            case "춘천":
                return new GridCoordinate(73, 134);
            case "청주":
                return new GridCoordinate(69, 106);
            case "전주":
                return new GridCoordinate(63, 89);
            case "창원":
                return new GridCoordinate(90, 77);
            case "제주":
                return new GridCoordinate(52, 38);
            default:
                // 기본값: 서울
                return new GridCoordinate(60, 127);
        }
    }
}