package org.cinemind.domain.externalApis.service

import jakarta.transaction.Transactional
import org.cinemind.domain.externalApis.client.KoficApiClient
import org.cinemind.domain.externalApis.dto.response.BoxOfficeInfo
import org.cinemind.domain.movie.entity.BoxOffice
import org.cinemind.domain.movie.entity.Movie
import org.cinemind.domain.movie.repository.BoxOfficeRepository
import org.cinemind.util.DateUtils
import org.springframework.stereotype.Service

@Service
@Transactional
class BoxOfficeSyncService (
    private val koficApiClient: KoficApiClient,
    private val boxOfficeRepository: BoxOfficeRepository,
    private val dateUtils: DateUtils,
    private val koficDataSyncService: KoficDataSyncService,
    private val boxOfficeCarryService: BoxOfficeCarryService,
){

    //    // targetDt를 이용해 BoxOffice 데이터 적재
    fun startLoadBoxOffice(startDate: String, endDate: String) {

        // 날짜 범위 생성 및 반복 호출
        val datesToLoad = dateUtils.generateDate(startDate, endDate)

        datesToLoad.forEach { targetDt ->
            // 단일 날짜의 BoxOffice 목록을 가져옴
            val dailyBoxOfficeList = koficApiClient.getMovieBoxOffice(targetDt)
            // 날짜 정보(targetDt)와 BoxOffice를 결합하여 처리
            saveBoxOfficeData(targetDt, dailyBoxOfficeList)
        }
    }

    // 주말 박스오피스 기반 인기 영화 데이터 적재를 시작
    fun syncPopularMovies() {
        boxOfficeCarryService.syncPopularMovies()
    }

//     개별 영화 데이터를 DB에 적재 (BoxOffice 통계도 함께 적재)
    private fun saveBoxOfficeData(targetDt: String, dailyBoxOfficeList: List<BoxOfficeInfo>) {

    // BoxOfficeInfo 목록을 순회
    dailyBoxOfficeList.forEach { boxOfficeInfo ->
        val movieCd = boxOfficeInfo.movieCd

        // Movie 엔티티 조회 (movies 테이블에 해당 영화가 없으면 상세 조회 api를 호출하여 Movie와 매핑 엔티티를 모두 저장. )
        val movie = koficDataSyncService.saveOrFindMovieData(movieCd) ?: return@forEach

        // BoxOffice 적재
        saveBoxOffice(
            movie, targetDt, boxOfficeInfo
        )
    }
}

    private fun saveBoxOffice(movie: Movie, target: String, boxOffice: BoxOfficeInfo): BoxOffice {
        // BoxOffice 엔티티 저장 로직
        return boxOfficeRepository.save(BoxOffice(
            movie = movie,
            targetDt = target,
            rank = boxOffice.rank,
            movieNm = boxOffice.movieNm,
            saleAccess = boxOffice.salesAcc,
            audiAcc = boxOffice.audiAcc
        ))
    }
}

