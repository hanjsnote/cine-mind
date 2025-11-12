package org.cinemind.domain.rag

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import org.cinemind.domain.rag.client.RagEmbeddingClient
import org.cinemind.domain.rag.repository.MovieEmbeddingProjectionDto
import org.cinemind.domain.rag.repository.MovieEmbeddingRepository
import org.cinemind.domain.rag.service.RagRetrievalService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * RagRetrievalService의 핵심 로직
 */
@SpringBootTest(classes = [RagRetrievalService::class])
@ActiveProfiles("test")
class RagRetrievalServiceTest @Autowired constructor (
    private val ragRetrievalService: RagRetrievalService
) {
    // Repository와 Client는 MockBean으로 주입하여 실제 호출을 방지
    @MockkBean
    lateinit var ragEmbeddingClient: RagEmbeddingClient

    @MockkBean
    lateinit var movieEmbeddingRepository: MovieEmbeddingRepository

    private val testQueryVector = FloatArray(1536) {0.1f}

    // 테스트를 위한 더미 ProjectionDTO 생성
    private fun createProjectionDto(id: Long, movieNm: String, movieCd: String, score: Double): MovieEmbeddingProjectionDto {
        return mockk<MovieEmbeddingProjectionDto> {
            every { this@mockk.id } returns id
            every { this@mockk.movieNm } returns movieNm
            every { this@mockk.movieCd } returns movieCd
            every { this@mockk.metaText } returns "Meta Text for $movieNm"
            every { this@mockk.plotText } returns "Plot Text for $movieNm"
            every { this@mockk.similarityScore } returns score
            every { this@mockk.movieId } returns id // 더미 값
            every { this@mockk.metaVector } returns Any() // 더미 값
            every { this@mockk.plotVector } returns Any() // 더미 값
            every { this@mockk.chunkOrder } returns 0 // 더미 값
        }
    }

    @Test
    fun META와_PLOT_검색_결과를_통합하고_중복_ID는_가장_낮은_거리를_선택_최종_3개를_반환한다() {
        // GIVEN
        val userQuery = "궁금한 영화"

        // Embedding Client 모킹: 항상 동일한 벡터 반환
        every { ragEmbeddingClient.getEmbedding(userQuery) } returns testQueryVector

        // Repository 모킹: 테스트 시나리오에 맞는 결과 반환
        // 청크 ID가 중복 되고 PLOT 점수가 더 좋은 경우
        val meta10_bad = createProjectionDto(10L, "영화 A", movieCd = "1234ab123",1.50) // 나쁜 점수 (버려져야 함)
        val plot10_best = createProjectionDto(10L, "영화 A", movieCd = "1234ab124",1.10) // 가장 좋은 점수 (선택)
        // 청크: ID 20
        val meta20_best = createProjectionDto(20L, "영화 B", movieCd = "1234ab125", 1.20) // 선택되어야 함
        // 청크: ID 30
        val plot30_best = createProjectionDto(30L, "영화 C", movieCd = "1234ab126",1.30) // 선택되어야 함
        // 청크: ID 40
        val meta40_bad = createProjectionDto(40L, "영화 D", movieCd = "1234ab127",1.60) // 최종 3개 초과로 버려져야 함

        // META 검색 결과 (3개 LIMIT)
        val mockMetaResults = listOf(meta10_bad, meta20_best, meta40_bad)
        // PLOT 검색 결과 (3개 LIMIT)
        val mockPlotResults = listOf(plot10_best, plot30_best, meta40_bad)

        every {
            movieEmbeddingRepository.findByMetaVectorSimilarity(testQueryVector, 3)
        } returns mockMetaResults

        every {
            movieEmbeddingRepository.findByPlotVectorSimilarity(testQueryVector, 3)
        } returns mockPlotResults

        // WHEN
        val resultList = ragRetrievalService.retrieveRelevantContext(userQuery)

        // THEN
        // 최종 결과는 RETRIEVAL_LIMIT(3)개여야 한다.
        assertEquals(3, resultList.size, "최종 컨텍스트 청크는 3개로 제한되어야 합니다.")

        // 중복 ID(10L)는 가장 낮은 점수(1.10)로 선택되어야 하며 최종 결과는 점수 순으로 정렬
        // 1순위: ID 10, Score1.10
        assertEquals(10L, resultList[0].id, "1순위는 가장 낮은 거리(1.10)를 가진 청크 10이어야 합니다.")

        // 2순위: ID 20, Score 1.20
        assertEquals(20L, resultList[1].id, "2순위는 거리 1.20을 가진 청크 20이어야 합니다.")

        // 3순위: ID 30, Score 1.30
        assertEquals(30L, resultList[2].id, "3순위는 거리 1.30을 가진 청크 30이어야 합니다.")

        // 청크 40 (1.60)은 3개 초과로 제외되어야 함
    }

    @Test
    fun 질문이_비어있으면_빈_목록을_반환한다(){
        // WHEN
        val resultList = ragRetrievalService.retrieveRelevantContext("")

        // THEN
        assertEquals(0, resultList.size)
    }

    @Test
    fun 임베딩_결과가_비어있으면_빈_목록을_반환한다(){
        // GIVEN
        every { ragEmbeddingClient.getEmbedding(any()) } returns FloatArray(0)

        // WHEN
        val resultList = ragRetrievalService.retrieveRelevantContext("질문")

        // THEN
        assertEquals(0, resultList.size)
    }
}