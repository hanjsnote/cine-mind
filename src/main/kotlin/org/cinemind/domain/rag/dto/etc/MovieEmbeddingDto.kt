package org.cinemind.domain.rag.dto.etc

import org.cinemind.domain.rag.entity.MovieEmbedding

// DB 데이터 인덱싱 시 엔티티 변환
// userQuery로 검색 후 엔티티 조회
class MovieEmbeddingDto (

    val id: Long?,
    // 원본 영화 ID
    val movieId: Long,

    // 정형 데이터 텍스트 청크
    val metaText: String,

    // 줄거리 텍스트 청크
    val plotText: String,

    // 메타 벡터
    val metaVector: String,

    // 줄거리 벡터
    val plotVector: String,

    // 청크 순서
    val chunkOrder: Int
){
    companion object {
        // MovieEmbedding 엔티티를 DTO로 변환
        fun from(entity: MovieEmbedding): MovieEmbeddingDto {
            return MovieEmbeddingDto(
                id = entity.id,
                movieId = entity.movie.id!!,
                metaText = entity.metaText,
                plotText = entity.plotText,
                metaVector = entity.metaVector,
                plotVector = entity.plotVector,
                chunkOrder = entity.chunkOrder
            )
        }
    }
}