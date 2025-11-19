package org.cinemind.domain.rag.repository
//
import org.cinemind.domain.rag.dto.model.MovieEmbeddingProjectionDto
import org.cinemind.domain.rag.entity.MovieEmbedding
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MovieEmbeddingRepository: JpaRepository<MovieEmbedding, Long> {

    /**
     * 메타 벡터 기반으로 유사도 검색을 수행 (가장 유사한 K개의 결과를 찾는다.)
     *
     * @param queryVector 사용자 질문을 임베딩한 벡터(String 타입)
     * @param limit 반환할 결과의 최대 개수 (K)
     * @return 유사도가 높은 MovieEmbedding 엔티티 목록
     * ORDER BY를 통해 거리가 짧은(유사도가 높은) 순서로 정렬한다.
     * '<->' 연산자는 'L2 distance'를 측정하며, pgvector 확장 기능에서 유사도 검색에 사용
     */
    @Query(value = """
    SELECT 
        e.id,
        e.movie_id AS movieId,
        m.movie_cd AS movieCd,
        m.movie_nm AS movieNm,
        e.meta_text AS metaText,
        e.plot_text AS plotText,
        e.meta_vector AS metaVector,
        e.plot_vector AS plotVector,
        e.chunk_order AS chunkOrder,
        e.meta_vector <-> CAST(:queryVector AS vector) AS similarityScore
    FROM movie_embeddings e
    JOIN movies m ON e.movie_id = m.id
    ORDER BY similarityScore
    LIMIT :limit
""", nativeQuery = true)
    fun findByMetaVectorSimilarity(
        @Param("queryVector") queryVector: FloatArray,
        @Param("limit") limit: Int
    ): List<MovieEmbeddingProjectionDto>

    /**
     * 줄거리 벡터 기반으로 유사도 검색을 수행한다. (가장 높은 K개의 결과를 찾는다.)
     */
    @Query(value = """
    SELECT 
        e.id,
        e.movie_id AS movieId,
        m.movie_cd AS movieCd,
        m.movie_nm AS movieNm,
        e.meta_text AS metaText,
        e.plot_text AS plotText,
        e.meta_vector AS metaVector,
        e.plot_vector AS plotVector,
        e.chunk_order AS chunkOrder,
        e.plot_vector <-> CAST(:queryVector AS vector) AS similarityScore
    FROM movie_embeddings e
    JOIN movies m ON e.movie_id = m.id
    ORDER BY similarityScore
    LIMIT :limit
""", nativeQuery = true)
    fun findByPlotVectorSimilarity(
        @Param("queryVector") queryVector: FloatArray,
        @Param("limit") limit: Int
    ): List<MovieEmbeddingProjectionDto>

    /**
     * 특정 movieId에 해당하는 모든 임베딩 청크를 조회
     */
    fun findAllByMovieId(movieId: Long): List<MovieEmbedding>
}