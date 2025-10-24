package org.cinemind.domain.kofic.service

import jakarta.transaction.Transactional
import org.cinemind.domain.kofic.client.KoficApiClient
import org.cinemind.domain.kofic.dto.response.BoxOfficeInfo
import org.cinemind.domain.movie.enums.PeopleRole
import org.cinemind.domain.movie.repository.BoxOfficeRepository
import org.cinemind.domain.movie.repository.CompanyRepository
import org.cinemind.domain.movie.repository.GenreRepository
import org.cinemind.domain.movie.repository.MovieRepository
import org.cinemind.domain.movie.repository.PeopleRepository
import org.cinemind.util.DateUtils
import org.springframework.stereotype.Service

@Service
@Transactional
class KoficDataLoadService (
    private val koficApiClient: KoficApiClient,
    private val movieRepository: MovieRepository,
    private val companyRepository: CompanyRepository,
    private val genreRepository: GenreRepository,
    private val peopleRepository: PeopleRepository,
    private val boxOfficeRepository: BoxOfficeRepository,
    private val dateUtils: DateUtils,
){
    // 전체 영화 목록을 DB에 적재
    fun saveMovieList() {
        val itemPerPage = 100   // 한 번에 적재할 영화 갯수
        var currentPage = 1     // 시작할 현재 페이지 번호
        var totalPages = 1

        // 페이지네이션을 반복하여 전체 목록 확보
        while (currentPage <= totalPages) {
            val result = koficApiClient.getMovieList(currentPage, itemPerPage) ?: break

            // 전체 페이지 수 계산 1151 페이지
            if (currentPage == 1) {
                totalPages = (result.totCnt + itemPerPage - 1) / itemPerPage
            }

            //확보된 목록을 순회하며 상세 정보 적재
            result.movieList.forEach { listItem ->
                // 목록 DTO를 사용하여 Movie 엔티티를 찾거나 생성/저장
//                saveMovieData(listItem.movieCd)
            }
            currentPage++
        }
    }


    // targetDt를 이용해 BoxOffice 데이터 적재
    fun startLoadProcess(startDate: String, endDate: String) {

        // 날짜 범위 생성 및 반복 호출
        val datesToLoad = dateUtils.generateDate(startDate, endDate)

        datesToLoad.forEach { targetDt ->
            // 단일 날짜의 BoxOffice 목록을 가져옴
            val dailyBoxOfficeList = koficApiClient.getMovieBoxOffice(targetDt)
            // 날짜 정보(targetDt)와 BoxOffice를 결합하여 처리
//            saveBoxOffice(targetDt, dailyBoxOfficeList)
        }
    }

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