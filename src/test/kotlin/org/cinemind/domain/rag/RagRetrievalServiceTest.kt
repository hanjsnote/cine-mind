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
    private fun createProjectionDto(id: Long, movieNm: String, score: Double): MovieEmbeddingProjectionDto {
        return mockk<MovieEmbeddingProjectionDto> {
            every { this@mockk.id } returns id
            every { this@mockk.movieNm } returns movieNm
            every { this@mockk.metaText } returns "Meta Text for $movieNm"
            every { this@mockk.plotText } returns "Plot Text for $movieNm"
            every { this@mockk.similarityScore } returns score
            every { this@mockk.movieId } returns id // 더미 값
            every { this@mockk.metaVector } returns Any() // 더미 값
            every { this@mockk.plotVector } returns Any() // 더미 값
            every { this@mockk.chunkOrder } returns 0 // 더미 값
        }
    }





}