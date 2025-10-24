package org.cinemind.domain.kofic.service

class BoxOfficeSyncService {


    // 개별 영화 데이터를 DB에 적재 (BoxOffice 통계도 함께 적재)
//    private fun saveMovieData(targetDt: String, dailyBoxOfficeList: List<BoxOfficeInfo>) {
//
//        // BoxOfficeInfo 목록을 순회
//        dailyBoxOfficeList.forEach { boxOfficeInfo ->
//            val movieCd = boxOfficeInfo.movieCd
//
//            // Movie 엔티티 확보 (없으면 상세 조회 api를 호출하여 Movie와 매핑 엔티티를 모두 저장)
//            val movie = saveOrFindMovieAndMappingData(movieCd) ?: return@forEach
//
//            // BoxOfficeStat 적재
//            saveBoxOfficeStat(movie, targetDt, boxOfficeInfo)
//        }
//    }

}