package org.cinemind.domain.rag.repository

import org.cinemind.domain.rag.entity.MovieEmbedding
import org.springframework.data.jpa.repository.JpaRepository

interface MovieEmbeddingRepository: JpaRepository<MovieEmbedding, Long>{
}