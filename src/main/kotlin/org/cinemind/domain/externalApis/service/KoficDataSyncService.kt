package org.cinemind.domain.externalApis.service

import jakarta.transaction.Transactional
import org.cinemind.domain.externalApis.client.KmdbApiClient
import org.cinemind.domain.externalApis.client.KoficApiClient
import org.cinemind.domain.externalApis.dto.response.MovieInfo
import org.cinemind.domain.movie.entity.Company
import org.cinemind.domain.movie.entity.Genre
import org.cinemind.domain.movie.entity.Movie
import org.cinemind.domain.movie.entity.MovieCompany
import org.cinemind.domain.movie.entity.MovieGenre
import org.cinemind.domain.movie.entity.MoviePeople
import org.cinemind.domain.movie.entity.People
import org.cinemind.domain.movie.enums.PeopleRole
import org.cinemind.domain.movie.repository.CompanyRepository
import org.cinemind.domain.movie.repository.GenreRepository
import org.cinemind.domain.movie.repository.MovieCompanyRepository
import org.cinemind.domain.movie.repository.MovieGenreRepository
import org.cinemind.domain.movie.repository.MoviePeopleRepository
import org.cinemind.domain.movie.repository.MovieRepository
import org.cinemind.domain.movie.repository.PeopleRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@Transactional
class KoficDataSyncService (
    private val koficApiClient: KoficApiClient,
    private val kmdbApiClient: KmdbApiClient,
    private val movieRepository: MovieRepository,
    private val companyRepository: CompanyRepository,
    private val genreRepository: GenreRepository,
    private val peopleRepository: PeopleRepository,
    private val movieGenreRepository: MovieGenreRepository,
    private val moviePeopleRepository: MoviePeopleRepository,
    private val movieCompanyRepository: MovieCompanyRepository,
    private val movieMatchingService: MovieMatchingService,
) {
    private val log = LoggerFactory.getLogger(KoficDataSyncService::class.java)

    // 전체 영화 목록을 조회
    fun saveMovieList() {
        val itemPerPage = 0    // 한 번에 적재할 영화 갯수
        var currentPage = 1     // 시작할 현재 페이지 번호
        var totalPages = 1

        // 테스트로 10건만 저장
        val result = koficApiClient.getMovieList(currentPage, itemPerPage)

        if (result == null) {
            println("Warning: Kofic API returned null for the first page.")
            return
        }

        result.movieList.forEach { listItem ->
            saveOrFindMovieData(listItem.movieCd)
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

    // BoxOfficeSyncService와 saveMovieList, getKmdbMovieDetail에서 모두 사용하는 통합 메서드
    fun saveOrFindMovieData(movieCd: String): Movie? {
        // DB 존재 여부 확인: 이미 DB에 있는 영화라면 상세 조회 API 호출 없이 바로 반환
        movieRepository.findByMovieCd(movieCd)?.let {
            return it
        }

        // 상세 조회 API 호출
        val movieInfo = koficApiClient.getMovieDetailList(movieCd) ?: run {
            //상세 정보가 없는 경우
            return null
        }

        // KMDb 줄거리 조회 API 호출 (줄거리 확보)
        // KOFIC의 제목(movieNm)과 개봉일(openDt)을 KMDb 검색 파라미터로 사용
        val kmdbInfo = kmdbApiClient.getKmdbMovieDetail(movieInfo.movieNm, movieInfo.openDt)

        // --- [매칭 로직 및 디버그 로깅 시작] ---
        // 1. 상세 매칭 후보 리스트를 가져옴
        val detailedMatches =
            movieMatchingService.getDetailedMatchCandidates(movieInfo.movieNm, movieInfo.openDt, kmdbInfo)

        if (detailedMatches.isEmpty()) {
            log.warn("[{}] KOFIC 제목 '{}' 에 대한 유효한 KMDb 후보가 없습니다. (유사도 임계값 미달)", movieInfo.movieCd, movieInfo.movieNm)
            return null
        }

        // 2. 디버깅용 로그 출력
        log.info("------ [매칭 분석] KOFIC 영화: {} ({}) ------", movieInfo.movieNm, movieInfo.openDt)
        detailedMatches.forEachIndexed { index, detail ->
            val matchResult = detail.kmdbResult
            val kmdbReleaseDate = matchResult.ratings?.rating?.firstOrNull()?.releaseDate
            val kmdbYear = if (!kmdbReleaseDate.isNullOrBlank() && kmdbReleaseDate.length >= 4) {
                kmdbReleaseDate.substring(0, 4)
            } else {
                "N/A"
            }

            log.info(
                "  [{}. KMDB: {} ({}년) | 제목 유사도: {} | 연도 일치: {} | 완벽 보너스: {} | 최종 점수: {}]",
                index + 1,
                matchResult.title,
                kmdbYear,
                String.format("%.4f", detail.titleScore),
                if (detail.yearMatch) "true" else "false",
                if (detail.isHighScoreBonusApplied) "true" else "false",
                String.format("%.4f", detail.totalScore)
            )
        }
        log.info("----------------------------------------------")

        // 퍼지 매칭을 통해 유사도가 높은 최적의 KMDb 영화를 찾음
        val bestMatch = movieMatchingService.findBestMatch(movieInfo.movieNm, movieInfo.openDt, kmdbInfo)

        // KoficDataSyncService
        // 줄거리 추출: KmMovieResponse 구조에 맞춰서 추출
        val plotText = bestMatch
            ?.plots
            ?.plot?.firstOrNull { it.plotLang == "한국어" }
            ?.plotText
            ?.replace("!", "")
            ?.trim()
            ?: "줄거리 정보 없음"

        // Movie 엔티티 저장 (목록 API 정보 + 상세 API 정보)
        val savedMovie = saveMovie(movieInfo, plotText)

        // 매핑 엔티티 저장 (Genre, People, Company)
        // [수정된 부분]: saveAllMappingEntites 메서드를 최적화된 로직으로 대체합니다.
        saveAllMappingEntites(savedMovie, movieInfo)

        return savedMovie
    }

    // Movie 엔티티 저장 로직
    private fun saveMovie(movieInfo: MovieInfo, plot: String): Movie {

        return movieRepository.save(
            Movie(
                movieCd = movieInfo.movieCd,
                movieNm = movieInfo.movieNm,
                movieNmEn = movieInfo.movieNmEn,
                showTm = movieInfo.showTm.toIntOrNull() ?: 0,
                openDt = movieInfo.openDt,
                typeNm = movieInfo.typeNm,
                watchGradeNm = movieInfo.audits.firstOrNull()?.watchGradeNm ?: "전체 관람가",
                plot = plot
            )
        )
    }

    /**
     * 매핑 엔티티 저장 로직: 장르, 인물, 회사 매핑 처리 (최적화 버전)
     * findAllBy...In 메서드를 사용하여 DB 조회 횟수를 최소화 (N+1 문제 해결)
     */
    private fun saveAllMappingEntites(movie: Movie, movieInfo: MovieInfo) {

        // ----------------------------
        // 1) Genre
        // ----------------------------
        val genreNames = movieInfo.genres
            .map { it.genreNm.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (genreNames.isNotEmpty()) {
            // 1) 먼저 DB에 insert 시도 (동시성 안전)
            genreRepository.upsertIgnoreAll(genreNames)

            // 2) 다시 조회해서 엔티티 확보
            val genreMap = genreRepository
                .findAllByGenreNmIn(genreNames)
                .associateBy { it.genreNm }

            // 3) 매핑 생성 (중복 제거)
            val movieGenres = genreNames
                .mapNotNull { name -> genreMap[name]?.let { MovieGenre(movie = movie, genre = it) } }
                .distinctBy { it.genre.id } // (movie_id, genre_id) 유니크 대비

            movieGenreRepository.saveAll(movieGenres)
        }


        // ----------------------------
        // 2) People
        // ----------------------------
        val allPeopleDtos = (
                movieInfo.directors.map { it.peopleNm.trim() to (it.peopleNmEn ?: "").trim() } +
                        movieInfo.actors.map { it.peopleNm.trim() to (it.peopleNmEn ?: "").trim() }
                )
            .filter { it.first.isNotBlank() }
            .distinctBy { it.first } // peopleNm 기준

        if (allPeopleDtos.isNotEmpty()) {
            val names = allPeopleDtos.map { it.first }
            val ens = allPeopleDtos.map { it.second }

            // 1) 먼저 upsert
            peopleRepository.upsertAll(names, ens)

            // 2) 다시 조회
            val peopleMap = peopleRepository
                .findAllByPeopleNmIn(names)
                .associateBy { it.peopleNm }

            // 3) 매핑 생성 + 중복 제거
            val moviePeoples = buildList {
                // 감독
                movieInfo.directors.forEach { d ->
                    val nm = d.peopleNm.trim()
                    val people = peopleMap[nm] ?: return@forEach
                    add(MoviePeople(movie = movie, people = people, role = PeopleRole.DIRECTOR, castNm = ""))
                }

                // 배우
                movieInfo.actors.forEach { a ->
                    val nm = a.peopleNm.trim()
                    val people = peopleMap[nm] ?: return@forEach
                    val cast = a.castNm?.trim() ?: ""
                    add(MoviePeople(movie = movie, people = people, role = PeopleRole.ACTOR, castNm = cast))
                }
            }.distinctBy { mp ->
                // (movie_id, people_id, role, cast_nm) 유니크 대비
                "${mp.people.id}-${mp.role}-${mp.castNm}"
            }

            moviePeopleRepository.saveAll(moviePeoples)
        }


        // ----------------------------
        // 3) Company
        // ----------------------------
        val companyDtos = movieInfo.companys
            .map { c ->
                val cd = c.companyCd.trim()
                val nm = c.companyNm.trim()
                val en = (c.companyNmEm ?: "").trim()
                Triple(cd, nm, en)
            }
            .filter { it.first.isNotBlank() }
            .distinctBy { it.first } // companyCd 기준

        if (companyDtos.isNotEmpty()) {
            val cds = companyDtos.map { it.first }
            val nms = companyDtos.map { it.second }
            val ens = companyDtos.map { it.third }

            // 1) 먼저 upsert
            companyRepository.upsertAll(cds, nms, ens)

            // 2) 다시 조회
            val companyMap = companyRepository
                .findAllByCompanyCdIn(cds)
                .associateBy { it.companyCd }

            // 3) 매핑 생성 + 중복 제거 (movie_id, company_id, company_part_nm)
            val movieCompanies = movieInfo.companys
                .mapNotNull { dto ->
                    val company = companyMap[dto.companyCd.trim()] ?: return@mapNotNull null
                    MovieCompany(
                        movie = movie,
                        company = company,
                        companyPartNm = dto.companyPartNm.trim()
                    )
                }
                .distinctBy { mc -> "${mc.company.id}-${mc.companyPartNm}" }

            movieCompanyRepository.saveAll(movieCompanies)
        }
    }
}