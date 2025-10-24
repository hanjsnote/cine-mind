package org.cinemind.domain.kofic.service

import jakarta.transaction.Transactional
import org.cinemind.domain.kofic.client.KoficApiClient
import org.cinemind.domain.kofic.dto.response.MovieInfo
import org.cinemind.domain.movie.entity.Company
import org.cinemind.domain.movie.entity.Genre
import org.cinemind.domain.movie.entity.Movie
import org.cinemind.domain.movie.entity.MovieCompany
import org.cinemind.domain.movie.entity.MovieGenre
import org.cinemind.domain.movie.entity.MoviePeople
import org.cinemind.domain.movie.entity.People
import org.cinemind.domain.movie.enums.PeopleRole
import org.cinemind.domain.movie.repository.BoxOfficeRepository
import org.cinemind.domain.movie.repository.CompanyRepository
import org.cinemind.domain.movie.repository.GenreRepository
import org.cinemind.domain.movie.repository.MovieCompanyRepository
import org.cinemind.domain.movie.repository.MovieGenreRepository
import org.cinemind.domain.movie.repository.MoviePeopleRepository
import org.cinemind.domain.movie.repository.MovieRepository
import org.cinemind.domain.movie.repository.PeopleRepository
import org.cinemind.util.DateUtils
import org.cinemind.util.toLocalDate
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@Transactional
class KoficDataSyncService (
    private val koficApiClient: KoficApiClient,
    private val movieRepository: MovieRepository,
    private val companyRepository: CompanyRepository,
    private val genreRepository: GenreRepository,
    private val peopleRepository: PeopleRepository,
    private val movieGenreRepository: MovieGenreRepository,
    private val moviePeopleRepository: MoviePeopleRepository,
    private val movieCompanyRepository: MovieCompanyRepository,
    private val boxOfficeRepository: BoxOfficeRepository,
    private val dateUtils: DateUtils,
){
    // 전체 영화 목록을 조회
    fun saveMovieList() {
        val itemPerPage = 10   // 한 번에 적재할 영화 갯수
        var currentPage = 1     // 시작할 현재 페이지 번호
        var totalPages = 1

        // 테스트로 100건만 저장
        val result = koficApiClient.getMovieList(currentPage, itemPerPage)

        if (result == null) {
            println("Warning: Kofic API returned null for the first page.")
            return
        }

        result.movieList.forEach { listItem ->
            saveMovieData(listItem.movieCd)
        }

        // 페이지네이션을 반복하여 전체 목록 확보
//        while (currentPage <= totalPages) {
//            val result = koficApiClient.getMovieList(currentPage, itemPerPage) ?: break
//
//            // 전체 페이지 수 계산 1151 페이지
//            if (currentPage == 1) {
//                totalPages = (result.totCnt + itemPerPage - 1) / itemPerPage
//            }
//
//            //확보된 목록을 순회하며 상세 정보 적재
//            result.movieList.forEach { listItem ->
//                // 목록 DTO를 사용하여 Movie 엔티티를 찾거나 생성/저장
//                saveMovieData(listItem.movieCd)
//            }
//            currentPage++
//        }
    }

//    // targetDt를 이용해 BoxOffice 데이터 적재
//    fun startLoadProcess(startDate: String, endDate: String) {
//
//        // 날짜 범위 생성 및 반복 호출
//        val datesToLoad = dateUtils.generateDate(startDate, endDate)
//
//        datesToLoad.forEach { targetDt ->
//            // 단일 날짜의 BoxOffice 목록을 가져옴
//            val dailyBoxOfficeList = koficApiClient.getMovieBoxOffice(targetDt)
//            // 날짜 정보(targetDt)와 BoxOffice를 결합하여 처리
////            saveBoxOffice(targetDt, dailyBoxOfficeList)
//        }
//    }

    // 영화 코드를 기준으로 DB에서 Movie 엔티티를 찾거나 없으면 Movie와 모든 매핑 엔티티를 저장
    private fun saveMovieData(movieCd: String): Movie? {
        // DB 존재 여부 확인: 이미 DB에 있는 영화라면 상세 조회 API 호출 없이 바로 반환
        movieRepository.findByMovieCd(movieCd)?.let {
            return it
        }

        // 상세 조회 API 호출
        val movieInfo = koficApiClient.getMovieDetailList(movieCd) ?: run {
            // 상세 정보가 없는 경우
            println("Warning: Detailed info for $movieCd not found. Skipping.")
            return null
        }

        // Movie 엔티티 저장 (목록 API 정보 + 상세 API 정보 통합
        val savedMovie = saveMovie(movieInfo)

        // 매핑 엔티티 저장 (장르, 인물, 회사)
        saveAllMappingEntites(savedMovie, movieInfo)

        return savedMovie
    }

    // Movie 엔티티 저장 로직 MovieInfoResponse를 Movie 엔티티로 변환 후 저장
    private fun saveMovie(movieInfo: MovieInfo): Movie {

        val openDate = movieInfo.openDt.toLocalDate() ?: LocalDate.of(1990, 1, 1)

        return movieRepository.save(Movie(
            movieCd = movieInfo.movieCd,
            movieNm = movieInfo.movieNm,
            movieNmEn = movieInfo.movieNmEn,
            showTm = movieInfo.showTm.toIntOrNull() ?: 0,
            openDt = openDate,
            typeNm = movieInfo.typeNm,
            watchGradeNm = movieInfo.audits.firstOrNull()?.watchGradeNm ?: "전체 관람가"
        ))
    }

    // 매핑 엔티티 저장 로직: 장르, 인물, 회사 매핑 처리
    private fun saveAllMappingEntites(movie: Movie, movieInfo: MovieInfo) {
        // 장르 처리 DTO 목록 -> DB에서 찾아서 비어있다면 저장 -> 매핑 테이블 저장
        movieInfo.genres.forEach { genreDto ->
            val genre = genreRepository.findByGenreNm(genreDto.genreNm)
                ?: genreRepository.save(Genre(genreDto.genreNm))
            movieGenreRepository.save(MovieGenre(movie = movie, genre = genre))
        }
        // 감독 목록 순회 및 저장
        movieInfo.directors.forEach { directorDto ->
            val people = peopleRepository.findByPeopleNm(directorDto.peopleNm)
                ?: peopleRepository.save(People(directorDto.peopleNm, directorDto.peopleNmEn ?: ""))
            moviePeopleRepository.save(MoviePeople(movie = movie, people = people, role = PeopleRole.DIRECTOR, castNm = ""))
        }

        // 배우 목록 순회 및 저장
        movieInfo.actors.forEach { actorDto  ->
            val people = peopleRepository.findByPeopleNm(actorDto.peopleNm)
                ?: peopleRepository.save(People(actorDto.peopleNm, actorDto.peopleNmEn ?: ""))
            moviePeopleRepository.save(MoviePeople(movie = movie, people = people, role = PeopleRole.ACTOR, castNm = actorDto.castNm))
        }

        // 회사 목록 순회 및 저장
        movieInfo.companys.forEach { companyDto ->
            val company = companyRepository.findByCompanyCd(companyDto.companyCd)
                ?: companyRepository.save(Company(companyDto.companyCd, companyDto.companyNm, companyDto.companyNmEm ?: "" ))
            movieCompanyRepository.save(MovieCompany(movie = movie, company = company, companyPartNm = companyDto.companyPartNm))
        }
    }
}