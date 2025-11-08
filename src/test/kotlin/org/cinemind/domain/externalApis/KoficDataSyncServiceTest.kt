package org.cinemind.domain.externalApis

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import jakarta.transaction.Transactional
import org.assertj.core.api.Assertions.assertThat
import org.cinemind.AbstractIntegrationTest
import org.cinemind.domain.chatbot.controller.ChatController
import org.cinemind.domain.chatbot.service.ChatService
import org.cinemind.domain.externalApis.client.KmdbApiClient
import org.cinemind.domain.externalApis.client.KoficApiClient
import org.cinemind.domain.externalApis.dto.etc.MatchDetail
import org.cinemind.domain.externalApis.dto.response.ActorKofic
import org.cinemind.domain.externalApis.dto.response.AuditKofic
import org.cinemind.domain.externalApis.dto.response.CompanyKofic
import org.cinemind.domain.externalApis.dto.response.DirectorKofic
import org.cinemind.domain.externalApis.dto.response.GenreKofic
import org.cinemind.domain.externalApis.dto.response.KmdbDataContainer
import org.cinemind.domain.externalApis.dto.response.KmdbMovieResponse
import org.cinemind.domain.externalApis.dto.response.KmdbPlots
import org.cinemind.domain.externalApis.dto.response.KmdbRating
import org.cinemind.domain.externalApis.dto.response.KmdbRatings
import org.cinemind.domain.externalApis.dto.response.KmdbResult
import org.cinemind.domain.externalApis.dto.response.MovieInfo
import org.cinemind.domain.externalApis.dto.response.PlotInfo
import org.cinemind.domain.externalApis.service.KoficDataSyncService
import org.cinemind.domain.externalApis.service.MovieMatchingService
import org.cinemind.domain.movie.entity.Movie
import org.cinemind.domain.movie.repository.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import javax.xml.crypto.Data
import kotlin.test.Test

// KoficDataSyncService의 비즈니스 로직(DB 조회 및 API 연동)을 테스트
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class KoficDataSyncServiceTest : AbstractIntegrationTest(){
    @MockkBean
    private lateinit var movieMatchingService: MovieMatchingService
    @Autowired
    private lateinit var movieRepository: MovieRepository
    @Autowired
    private lateinit var genreRepository: GenreRepository
    @Autowired
    private lateinit var peopleRepository: PeopleRepository
    @Autowired
    private lateinit var companyRepository: CompanyRepository
    @Autowired
    private lateinit var movieCompanyRepository: MovieCompanyRepository
    @Autowired
    private lateinit var movieGenreRepository: MovieGenreRepository
    @Autowired
    private lateinit var moviePeopleRepository: MoviePeopleRepository

    @Autowired
    private lateinit var koficService: KoficDataSyncService

    @MockkBean
    private lateinit var koficApiClient: KoficApiClient

    @MockkBean
    private lateinit var kmdbApiClient: KmdbApiClient

    @MockkBean
    private lateinit var chatService: ChatService

    @MockkBean
    private lateinit var chatController: ChatController

    @Test
    fun 신규_movieCd_전달시_Movie와_모든_매핑_엔티티를_저장한다() {
        // given
        val movieCd = "2025A123"
        val movieNm = "테스트 영화"
        val openDt = "20240101"

        // KOFIC API Mock 설정 (MockK은 every, Mockito에선 given)
        val mockKoficMovieInfo = createMockKoficMovieInfo(movieCd, movieNm, openDt)
        every { koficApiClient.getMovieDetailList(movieCd) } returns mockKoficMovieInfo

        // KMDb API Mock 설정 (KMDb API 호출이 유효한 데이터를 반환)
        val mockKmdbResponse = createMockKmdbMovieDetail(movieNm, openDt)
        every { kmdbApiClient.getKmdbMovieDetail(movieNm, openDt) } returns mockKmdbResponse

        // MatchDetail DTO의 kmdbResult 필드에 필요한 KmdbResult 객체 추출
        val mockKmdbResult: KmdbResult = mockKmdbResponse.Data.firstOrNull()?.Result?.firstOrNull()
            ?: throw IllegalStateException("Mock KmdbResult cannot be null")

        // MovieMatchingService Mock 설정
        val mockMatchDetail = createMockMatchDetail(mockKmdbResult, titleScore = 0.85, yearMatch = true, totalScore = 1.0)
        every {
            movieMatchingService.getDetailedMatchCandidates(any(), any(), any())
        } returns listOf(mockMatchDetail)

        // findBestMatch Mock 설정
        every {
            movieMatchingService.findBestMatch(any(), any(), any())
        } returns mockKmdbResult

        // when
        // 신규 영화 코드를 전달하여 저장 로직 실행
        val resultMovie = koficService.saveOrFindMovieData(movieCd)

        // then
        // Movie 엔티티 검증
        assertThat(resultMovie).isNotNull
        assertThat(resultMovie!!.movieCd).isEqualTo(movieCd)
        assertThat(resultMovie.movieNm).isEqualTo(movieNm)

        // 메서드가 1번 호출되었는지 검증
        verify(exactly = 1) { koficApiClient.getMovieDetailList(movieCd) }

        // 매핑 엔티티 검증
        assertThat(movieGenreRepository.findAll().size).isEqualTo(2)
        assertThat(moviePeopleRepository.findAll().size).isEqualTo(2)
        assertThat(movieCompanyRepository.findAll().size).isEqualTo(1)

        // 연관 엔티티도 저장되었는지 확인
        assertThat(genreRepository.findByGenreNm("액션")).isNotNull
    }

    @Test
    fun 이미_존재하는_movieCd_전달시_API_호출없이_Movie를_반환한다() {
        // given
        val movieCd = "2025A111"
        val movieNm = "기존 영화 이름"

        movieRepository.save(
            Movie(
                movieCd = movieCd,
                movieNm = movieNm,
                movieNmEn = "",
                showTm = 100,
                openDt = "20250101",
                typeNm = "장편",
                watchGradeNm = "전체 관람가"
            )
        )

        // when 테스트 대상 메서드 실행
        val resultMovie = koficService.saveOrFindMovieData(movieCd)

        // then
        assertThat(resultMovie).isNotNull
        assertThat(resultMovie!!.movieCd).isEqualTo(movieCd)
        assertThat(resultMovie.movieNm).isEqualTo(movieNm)
    }

    // KOFIC API 응답 DTO (MovieInfo) Mock 생성
    private fun createMockKoficMovieInfo(movieCd: String, movieNm: String, openDt: String): MovieInfo {
        return MovieInfo(
            movieCd = movieCd,
            movieNm = movieNm,
            movieNmEn = "Test Movie",
            showTm = "120",
            openDt = openDt,
            typeNm = "장편",
            genres = listOf(GenreKofic("액션"), GenreKofic("판타지")),
            directors = listOf(DirectorKofic("김감독", "Kim Directors")),
            actors = listOf(ActorKofic("박배우", "Park Actor", "주연")),
            companys = listOf(
                CompanyKofic(
                    "20131234", "(주)테스트 영화사", "", companyPartNm = "",
                )
            ),
            audits = listOf(AuditKofic(watchGradeNm = "전체 관람가"))
        )
    }

    // KMDb API 응답 DTO(Mock) 생성 - 실제 응답 구조에 맞춤
    private fun createMockKmdbMovieDetail(title: String, releaseDate: String): KmdbMovieResponse {
        return KmdbMovieResponse(
            Data = listOf(
                KmdbDataContainer(
                    Result = listOf(
                        KmdbResult(
                            movieId = "KMDB12345",
                            title = title,
                            titleEng = "Test Movie",
                            plots = KmdbPlots(
                                plot = listOf(
                                    PlotInfo(
                                        plotLang = "한국어",
                                        plotText = "테스트 줄거리입니다."
                                    )
                                )
                            ),
                            ratings = KmdbRatings(
                                rating = listOf(
                                    KmdbRating(
                                        releaseDate = releaseDate
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    // 매칭 상세 Mock (실제 DTO 타입 사용)
    private fun createMockMatchDetail(
        kmdbResult: KmdbResult,
        titleScore: Double,
        yearMatch: Boolean,
        totalScore: Double
    ): MatchDetail {
        return MatchDetail(
            kmdbResult = kmdbResult,
            normalizedKmdbTitle = kmdbResult.title.replace(" ", "").uppercase(), // 임시 정규화 로직
            titleScore = titleScore,
            totalScore = totalScore,
            yearMatch = yearMatch,
            isHighScoreBonusApplied = totalScore > titleScore
        )
    }
}




