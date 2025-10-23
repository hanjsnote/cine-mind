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
    private val dateUtils: DateUtils
){
    // targetDt를 이용해 적재 시작
    fun startLoadProcess(startDate: String, endDate: String) {

        val allBoxOfficeInfos = mutableListOf<BoxOfficeInfo>()

        // 날짜 범위 생성 및 반복 호출
        val datesToLoad = dateUtils.generateDate(startDate, endDate)

        datesToLoad.forEach { targetDt ->
            // 단일 날짜의 BoxOffice 목록을 가져옴
            val dailyBoxOfficeList = koficApiClient.getMovieBoxOffice(targetDt)

            // 날짜 정보(targetDt)와 BoxOffice를 결합하여 처리
            saveMovieData(targetDt, dailyBoxOfficeList)
        }
    }

    // 개별 영화 데이터를 DB에 적재 (BoxOffice 통계도 함께 적재)
    private fun saveMovieData(targetDt: String, dailyBoxOfficeList: List<BoxOfficeInfo>) {

        // BoxOfficeInfo 목록을 순회
        dailyBoxOfficeList.forEach { boxOfficeInfo ->
            val movieCd = boxOfficeInfo.movieCd


        }
    }
}