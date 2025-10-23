package org.cinemind.domain.kofic.service

import jakarta.transaction.Transactional
import org.cinemind.domain.kofic.client.KoficApiClient
import org.cinemind.domain.movie.enums.PeopleRole
import org.cinemind.domain.movie.repository.CompanyRepository
import org.cinemind.domain.movie.repository.GenreRepository
import org.cinemind.domain.movie.repository.MovieRepository
import org.cinemind.domain.movie.repository.PeopleRepository
import org.springframework.stereotype.Service

@Service
@Transactional
class KoficDataLoadService (
    private val koficApiClient: KoficApiClient,
    private val movieRepository: MovieRepository,
    private val companyRepository: CompanyRepository,
    private val genreRepository: GenreRepository,
    private val peopleRepository: PeopleRepository
){
    // 적재 시작
    fun startLoadProcess(startDate: String, endDate: String) {
        // movieCd 목록 확보
        val allMovieCds = mutableSetOf<String>()

        // 확보된 모든 movieCd에 대해 상세 정보 적재 실행
        loadMovieData(allMovieCds.toList())
    }

    // 개별 영화 데이터를 DB에 적재
    private fun loadMovieData(movieCdList: List<String>) {
        movieCdList.forEach { movieCd ->
            // 이미 DB에 존재하는 영화인지 확인 (중복 적재 방지)
            if (movieRepository.existsByMovieCd(movieCd)) {
                return@forEach  // 이미 있으면 건너뜀
            }

            val movieInfo = koficApiClient.getMovieDetailList(movieCd) ?: return@forEach

//            // Movie 엔티티 저장
//            val movie = saveMovie(movieInfo)
//
//            // Genre, Company, Person 등 단일 엔티티 중복 확인 후 저장
//            val genres = saveOrFindGenres(movieInfo.genres)
//            val companys = saveOrFindCompanys(movieInfo.companys)
//            val directors = saveOrFindPeoples(movieInfo.directors, role = PeopleRole.DIRECTOR)
//            val actors = saveOrFindPeoples(movieInfo.actors, role = PeopleRole.ACTOR)
//
//            // 매핑 테이블 엔티티 (MovieGenre, MovieCompany, MoviePerson) 저장
//            saveMappingEntities(movie, genres, companys, directors, actors)
        }
    }
}