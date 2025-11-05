package org.cinemind.domain.externalApis

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import jakarta.transaction.Transactional
import org.assertj.core.api.Assertions.assertThat
import org.cinemind.domain.externalApis.client.KmdbApiClient
import org.cinemind.domain.externalApis.client.KoficApiClient
import org.cinemind.domain.externalApis.dto.response.ActorKofic
import org.cinemind.domain.externalApis.dto.response.AuditKofic
import org.cinemind.domain.externalApis.dto.response.CompanyKofic
import org.cinemind.domain.externalApis.dto.response.DirectorKofic
import org.cinemind.domain.externalApis.dto.response.GenreKofic
import org.cinemind.domain.externalApis.dto.response.MovieInfo
import org.cinemind.domain.externalApis.service.KoficDataSyncService
import org.cinemind.domain.movie.entity.Movie
import org.cinemind.domain.movie.repository.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Test

// KoficDataSyncService의 비즈니스 로직(DB 조회 및 API 연동)을 테스트
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class KoficDataSyncServiceTest {
    @Autowired private lateinit var movieRepository: MovieRepository
    @Autowired private lateinit var genreRepository: GenreRepository
    @Autowired private lateinit var peopleRepository: PeopleRepository
    @Autowired private lateinit var companyRepository: CompanyRepository
    @Autowired private lateinit var movieCompanyRepository: MovieCompanyRepository
    @Autowired private lateinit var movieGenreRepository: MovieGenreRepository
    @Autowired private lateinit var moviePeopleRepository: MoviePeopleRepository

    @MockkBean
    private lateinit var koficApiClient: KoficApiClient

    @MockkBean
    private lateinit var kmdbApiClient: KmdbApiClient

    @Autowired
    private lateinit var koficService: KoficDataSyncService

    @Test
    fun 신규_movieCd_전달시_Movie와_모든_매핑_엔티티를_저장한다() {
        // given
        val movieCd = "2025A123"
        val movieNm = "테스트 영화"

        // Mocking 설정: koficApiClient.getMovieDetailList()가 호출되면 가짜 데이터(movieInfo)를 반환
        val mockMovieInfo = createMockMovieInfo(movieCd, movieNm)

        // 실제 api 호출 대신 미리 정의된 mockMovieInfo 반환 // MockK은 every, Mockito에선 given
        every { koficApiClient.getMovieDetailList(movieCd) } returns mockMovieInfo

        // 임시로 테스트 성공을 위해 null 반환
        every { kmdbApiClient.getKmdbMovieDetail(any(), any()) } returns null


        // when
        // 신규 영화 코드를 전달하여 저장 로직 실행
        val resultMovie = koficService.saveOrFindMovieData(movieCd)

        // then
        // Movie 엔티티 검증
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

        movieRepository.save(Movie(
            movieCd = movieCd,
            movieNm = movieNm,
            movieNmEn = "",
            showTm = 100,
            openDt = "20250101",
            typeNm = "장편",
            watchGradeNm = "전체 관람가"
        ))

        // when 테스트 대상 메서드 실행
        val resultMovie = koficService.saveOrFindMovieData(movieCd)

        // then
        assertThat(resultMovie).isNotNull
        assertThat(resultMovie!!.movieCd).isEqualTo(movieCd)
        assertThat(resultMovie.movieNm).isEqualTo(movieNm)
    }

    // 테스트 데이터 생성을 위한 헬퍼 함수
    private fun createMockMovieInfo(movieCd: String, movieNm: String): MovieInfo {
        return MovieInfo(
            movieCd = movieCd,
            movieNm = movieNm,
            movieNmEn = "Test Movie",
            showTm = "120",
            openDt = "20250101",
            typeNm = "장편",
            genres = listOf(GenreKofic("액션"), GenreKofic("판타지")),
            directors = listOf(DirectorKofic("김감독", "Kim Directors")),
            actors = listOf(ActorKofic("박배우", "Park Actor", "주연")),
            companys = listOf(CompanyKofic(
                "20131234", "(주)테스트 영화사", "", companyPartNm = "",
            )),
            audits = listOf(AuditKofic(watchGradeNm = "전체 관람가"))
        )
    }
}