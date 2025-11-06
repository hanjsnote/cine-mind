package org.cinemind.domain.rag.service

import jakarta.transaction.Transactional
import org.cinemind.domain.movie.repository.MovieRepository
import org.cinemind.domain.rag.client.RagEmbeddingClient
import org.cinemind.domain.rag.dto.etc.MovieEmbeddingDto
import org.cinemind.domain.rag.dto.etc.MovieRagDto
import org.cinemind.domain.rag.repository.MovieEmbeddingRepository
import org.springframework.stereotype.Service

@Service
class RagIndexingService (
    private val movieRepository: MovieRepository, // 원본 데이터 조회
    private val movieEmbeddingRepository: MovieEmbeddingRepository,  // 벡터 DB 저장
    private val ragEmbeddingClient: RagEmbeddingClient  // 임베딩 클라이언트
) {
    // 전체 영화 데이터를 RAG 벡터 스토어에 인덱싱
    @Transactional
    fun indexAllMovies() {
        // 전체 Movie 엔티티를 조회하고 MovieRagDto로 변환
        val movieRagDtos = movieRepository.findAll().map { MovieRagDto.from(it) }

        // 기존 임베딩 데이터가 있다면 삭제
        movieEmbeddingRepository.deleteAll()

        // 각 영화 DTO에 대해 청크 분할 및 임베딩 작업 수행
        movieRagDtos.forEach { dto ->
            // 청크를 생성하고 임베딩하는 작업 수행
            val embeddingDtos = createChunkAndEmbeddings(dto)

            // 생성된 임베딩 DTO들을 엔티티로 변환하여 저장
            val embeddingEntities = embeddingDtos.map { toEntity(it) }
//            movieEmbeddingRepository.saveAll(embeddingEntities)
        }



    }

    private fun createChunkAndEmbeddings(dto: MovieRagDto): List<MovieEmbeddingDto> {

        return emptyList()
    }

    private fun toEntity(dto: MovieEmbeddingDto): MovieEmbeddingDto {


        return TODO("반환 값을 제공하세요")
    }
}