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
    val metaVector: FloatArray,

    // 줄거리 벡터
    val plotVector: FloatArray,

    // 청크 순서
    val chunkOrder: Int
){
    // Array 비교를 위한 equals/hashCode 오버라이딩 (데이터 클래스 기본 동작 방지)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MovieEmbeddingDto

        if (id != other.id) return false
        if (movieId != other.movieId) return false
        if (metaText != other.metaText) return false
        if (plotText != other.plotText) return false
        if (!metaVector.contentEquals(other.metaVector)) return false
        if (!plotVector.contentEquals(other.plotVector)) return false
        if (chunkOrder != other.chunkOrder) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + movieId.hashCode()
        result = 31 * result + metaText.hashCode()
        result = 31 * result + plotText.hashCode()
        result = 31 * result + metaVector.contentHashCode()
        result = 31 * result + plotVector.contentHashCode()
        result = 31 * result + chunkOrder
        return result
    }

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