package org.cinemind.domain.rag.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.cinemind.common.entity.BaseEntity
import org.cinemind.domain.movie.entity.Movie

@Entity
@Table(name = "movie_embeddings")
class MovieEmbedding(

    // movies 테이블의 FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    val movie: Movie,

    // 벡터화된 정형 데이터 텍스트 원본
    @Column(name = "meta_text", columnDefinition = "TEXT", nullable = false)
    val metaText : String,

    // 벡터화된 비정형(줄거리) 데이터 텍스트 원본
    @Column(name = "plot_text", columnDefinition = "TEXT", nullable = false)
    val plotText : String,

    // 정형 데이터 임베딩
    @Column(name = "meta_vector", columnDefinition = "vector", nullable = false)
    val metaVector: String,

    // 비정형(줄거리) 데이터 임베딩
    @Column(name = "plot_vector", columnDefinition = "vector", nullable = false)
    val plotVector: String,

    // 청크가 원본 줄거리에서 몇 번째 청크인지 (순서 재구성용)
    @Column(name = "chunk_order")
    val chunkOrder: Int

) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}