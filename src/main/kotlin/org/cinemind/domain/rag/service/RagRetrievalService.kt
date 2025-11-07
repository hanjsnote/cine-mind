package org.cinemind.domain.rag.service

import org.cinemind.domain.rag.client.RagEmbeddingClient
import org.cinemind.domain.rag.dto.etc.MovieEmbeddingDto
import org.cinemind.domain.rag.repository.MovieEmbeddingRepository
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
    // LLM에 전달할 컨텍스트 청크의 최대 개수
    private val RETRIEVAL_LIMIT = 5

    // 사용자 질문에 가장 관련성이 높은 영화 임베딩 청크(Context)를 검색한다.\
    fun retrieveRelevantContext(userQuery: String): List<MovieEmbeddingDto> {
        if (userQuery.isBlank()) {
            return emptyList()
        }

        // 사용자 질문을 임베딩하여 쿼리 벡터를 생성
        val queryVector = ragEmbeddingClient.getEmbedding(userQuery)

        if (queryVector.isBlank()) {
            // 임베딩 실패 시 빈 리스트 반환
            return emptyList()
        }

        // 메타 벡터 기반 유사도 검색 수행 (Top-K)
        val metaResults = movieEmbeddingRepository.findByMetaVectorSimilarity(queryVector, RETRIEVAL_LIMIT)

        // 줄거리 벡터 기반 유사도 검색 수행 (Top-K)
        // 멀티-벡터 전략을 위해 두 가지 검색 결과를 모두 사용
        val plotResults = movieEmbeddingRepository.findByPlotVectorSimilarity(queryVector, RETRIEVAL_LIMIT)

        // 두 결과를 합치고 중복을 제거하여 최종 컨텍스트를 구성
        val combinedResult = (metaResults + plotResults)
            .distinctBy { it.id }   // 중복 제거 (같은 청크가 메타/줄거리 검색에 모두 잡힐 수 있음
            .sortedBy { it.chunkOrder } // 원본 순서대로 정렬

        // 최종 결과를 DTO로 변환하여 반환
        return combinedResult.map { MovieEmbeddingDto.from(it) }
    }
}