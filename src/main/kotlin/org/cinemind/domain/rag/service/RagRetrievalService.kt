package org.cinemind.domain.rag.service

import org.cinemind.domain.rag.client.RagEmbeddingClient
import org.cinemind.domain.rag.dto.etc.MovieEmbeddingDto
import org.cinemind.domain.rag.repository.MovieEmbeddingRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * RAG 파이프라인의 검색(Retrieval) 부분을 담당하는 서비스
 * 사용자 질문을 벡터화하고 벡터 DB에서 가장 유사한 컨텍스트 청크를 검색
 */
@Service
class RagRetrievalService (
    private val ragEmbeddingClient: RagEmbeddingClient,
    private val movieEmbeddingRepository: MovieEmbeddingRepository
) {
    private val log = LoggerFactory.getLogger(RagRetrievalService::class.java)
    // LLM에 전달할 컨텍스트 청크의 최대 개수
    private val RETRIEVAL_LIMIT = 3

    // 사용자 질문에 가장 관련성이 높은 영화 임베딩 청크(Context)를 검색한다.\
    fun retrieveRelevantContext(userQuery: String): List<MovieEmbeddingDto> {
        if (userQuery.isBlank()) return emptyList()

        val queryVector = ragEmbeddingClient.getEmbedding(userQuery)
        if (queryVector.isEmpty()) return emptyList()

        val metaResults = movieEmbeddingRepository.findByMetaVectorSimilarity(queryVector, RETRIEVAL_LIMIT)
        val plotResults = movieEmbeddingRepository.findByPlotVectorSimilarity(queryVector, RETRIEVAL_LIMIT)

        // 유사도 점수 로그 출력
        metaResults.forEach {
            log.info("[META] 영화명=${it.movieNm}, 청크ID=${it.id}, 거리=${"%.6f".format(it.similarityScore)}")
        }
        plotResults.forEach {
            log.info("[PLOT] 영화명=${it.movieNm}, 청크ID=${it.id}, 거리=${"%.6f".format(it.similarityScore)}")
        }

        val combinedResult = (metaResults + plotResults)
            .distinctBy { it.id }
            .sortedBy { it.similarityScore } // 유사도 거리가 짧은 순서로 정렬
            .take(RETRIEVAL_LIMIT)      // LLM에 전달할 컨텍스트 청크 최대 갯수 제한

        // DTO 변환
        return combinedResult.map {
            MovieEmbeddingDto(
                id = it.id,
                movieId = it.movieId,
                movieNm = it.movieNm,
                metaText = it.metaText,
                plotText = it.plotText,
                metaVector = floatArrayOf(), // 필요 시 파싱 추가
                plotVector = floatArrayOf(),
                chunkOrder = it.chunkOrder
            )
        }
    }
}