package com.example.umbrellaalert.service;

import com.example.umbrellaalert.data.model.SearchLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 장소 검색 서비스 (목업 데이터 제공)
 */
public class LocationSearchService {

    /**
     * 세종시 및 대전시 주요 장소 목업 데이터
     */
    private static final List<SearchLocation> ALL_LOCATIONS = new ArrayList<>();

    static {
        // === 세종시 ===
        // 관공서
        ALL_LOCATIONS.add(new SearchLocation("세종시청", "세종특별자치시 한누리대로 2130", 36.4800, 127.2890, "관공서"));
        ALL_LOCATIONS.add(new SearchLocation("세종시의회", "세종특별자치시 한누리대로 2130", 36.4805, 127.2885, "관공서"));
        ALL_LOCATIONS.add(new SearchLocation("세종경찰서", "세종특별자치시 한누리대로 2104", 36.4790, 127.2870, "관공서"));

        // 교육기관
        ALL_LOCATIONS.add(new SearchLocation("세종대학교", "세종특별자치시 연기면 세종로 567", 36.4950, 127.2800, "대학교"));
        ALL_LOCATIONS.add(new SearchLocation("고운중학교", "세종특별자치시 고운로 42", 36.4820, 127.2950, "중학교"));
        ALL_LOCATIONS.add(new SearchLocation("새롬고등학교", "세종특별자치시 새롬로 20", 36.4780, 127.2920, "고등학교"));

        // 쇼핑/마트
        ALL_LOCATIONS.add(new SearchLocation("롯데마트 세종점", "세종특별자치시 한누리대로 2169", 36.4810, 127.2900, "마트"));
        ALL_LOCATIONS.add(new SearchLocation("이마트 세종점", "세종특별자치시 새롬로 81", 36.4770, 127.2930, "마트"));
        ALL_LOCATIONS.add(new SearchLocation("홈플러스 세종점", "세종특별자치시 도움7로 12", 36.4850, 127.2980, "마트"));

        // 의료기관
        ALL_LOCATIONS.add(new SearchLocation("세종충남대학교병원", "세종특별자치시 20(금남면)", 36.5100, 127.2600, "병원"));
        ALL_LOCATIONS.add(new SearchLocation("세종병원", "세종특별자치시 새롬로 20", 36.4780, 127.2920, "병원"));

        // 문화/여가
        ALL_LOCATIONS.add(new SearchLocation("세종문화예술회관", "세종특별자치시 한누리대로 2110", 36.4795, 127.2875, "문화시설"));
        ALL_LOCATIONS.add(new SearchLocation("세종호수공원", "세종특별자치시 연기면 세종로 567", 36.4900, 127.2850, "공원"));
        ALL_LOCATIONS.add(new SearchLocation("국립세종도서관", "세종특별자치시 한누리대로 2121", 36.4800, 127.2880, "도서관"));

        // 교통
        ALL_LOCATIONS.add(new SearchLocation("세종고속버스터미널", "세종특별자치시 조치원읍 터미널로 11", 36.6050, 127.2900, "터미널"));
        ALL_LOCATIONS.add(new SearchLocation("조치원역", "세종특별자치시 조치원읍 역전로 1", 36.6080, 127.2920, "기차역"));

        // 종교시설
        ALL_LOCATIONS.add(new SearchLocation("세종중앙교회", "세종특별자치시 한누리대로 2000", 36.4750, 127.2850, "교회"));
        ALL_LOCATIONS.add(new SearchLocation("세종성당", "세종특별자치시 새롬로 15", 36.4770, 127.2900, "성당"));

        // === 대전시 ===
        // 교육기관
        ALL_LOCATIONS.add(new SearchLocation("한밭대학교", "대전광역시 유성구 동서대로 125", 36.3504, 127.2992, "대학교"));
        ALL_LOCATIONS.add(new SearchLocation("충남대학교", "대전광역시 유성구 대학로 99", 36.3665, 127.3444, "대학교"));
        ALL_LOCATIONS.add(new SearchLocation("KAIST", "대전광역시 유성구 대학로 291", 36.3736, 127.3616, "대학교"));
        ALL_LOCATIONS.add(new SearchLocation("대전대학교", "대전광역시 동구 대학로 62", 36.3270, 127.4354, "대학교"));

        // 관공서
        ALL_LOCATIONS.add(new SearchLocation("대전시청", "대전광역시 서구 둔산로 100", 36.3504, 127.3845, "관공서"));
        ALL_LOCATIONS.add(new SearchLocation("대전시의회", "대전광역시 서구 둔산로 100", 36.3500, 127.3840, "관공서"));

        // 쇼핑/마트
        ALL_LOCATIONS.add(new SearchLocation("롯데마트 대전점", "대전광역시 서구 계룡로 598", 36.3550, 127.3780, "마트"));
        ALL_LOCATIONS.add(new SearchLocation("이마트 대전터미널점", "대전광역시 동구 정동 1-1", 36.3270, 127.4280, "마트"));
        ALL_LOCATIONS.add(new SearchLocation("홈플러스 대전유성점", "대전광역시 유성구 온천로 45", 36.3620, 127.3440, "마트"));

        // 교통
        ALL_LOCATIONS.add(new SearchLocation("대전역", "대전광역시 동구 중앙로 215", 36.3315, 127.4350, "기차역"));
        ALL_LOCATIONS.add(new SearchLocation("서대전역", "대전광역시 서구 배재로 200", 36.3515, 127.3790, "기차역"));
        ALL_LOCATIONS.add(new SearchLocation("대전복합터미널", "대전광역시 동구 동서대로 1700", 36.3350, 127.4280, "터미널"));

        // 문화/여가
        ALL_LOCATIONS.add(new SearchLocation("엑스포과학공원", "대전광역시 유성구 대덕대로 480", 36.3720, 127.3840, "공원"));
        ALL_LOCATIONS.add(new SearchLocation("대전예술의전당", "대전광역시 서구 둔산대로 135", 36.3520, 127.3890, "문화시설"));
    }

    /**
     * 장소 이름으로 검색
     */
    public static List<SearchLocation> searchByName(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<SearchLocation> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        for (SearchLocation location : ALL_LOCATIONS) {
            if (location.getName().toLowerCase().contains(lowerQuery) ||
                location.getAddress().toLowerCase().contains(lowerQuery) ||
                (location.getCategory() != null && location.getCategory().toLowerCase().contains(lowerQuery))) {
                results.add(location);
            }
        }

        // 최대 10개 결과만 반환
        return results.size() > 10 ? results.subList(0, 10) : results;
    }

    /**
     * 좌표를 주소로 변환 (역지오코딩 목업)
     */
    public static String getAddressFromCoordinates(double latitude, double longitude) {
        // 세종시 범위 내인지 확인
        if (latitude >= 36.4 && latitude <= 36.7 && longitude >= 127.1 && longitude <= 127.4) {
            return "세종특별자치시";
        }
        // 대전시 범위 내인지 확인
        else if (latitude >= 36.2 && latitude <= 36.5 && longitude >= 127.2 && longitude <= 127.5) {
            return "대전광역시";
        } else {
            return "기타 지역";
        }
    }

    /**
     * 인기 장소 목록 반환
     */
    public static List<SearchLocation> getPopularLocations() {
        List<SearchLocation> popular = new ArrayList<>();
        popular.add(ALL_LOCATIONS.get(0)); // 세종시청
        popular.add(ALL_LOCATIONS.get(6)); // 롯데마트 세종점
        popular.add(ALL_LOCATIONS.get(13)); // 세종호수공원
        popular.add(ALL_LOCATIONS.get(27)); // 한밭대학교
        return popular;
    }
}
