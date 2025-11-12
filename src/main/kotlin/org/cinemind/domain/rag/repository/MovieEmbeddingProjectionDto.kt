package org.cinemind.domain.rag.repository

interface MovieEmbeddingProjectionDto {
    val id: Long
    val movieId: Long
    val movieCd: String
    val movieNm: String
    val metaText: String
    val plotText: String
    val metaVector: Any
    val plotVector: Any
    val chunkOrder: Int
    val similarityScore: Double
}