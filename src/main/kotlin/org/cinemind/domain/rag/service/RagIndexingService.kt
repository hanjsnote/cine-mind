package org.cinemind.domain.rag.service

import jakarta.transaction.Transactional
import org.cinemind.domain.movie.entity.Movie
import org.cinemind.domain.movie.repository.MovieRepository
import org.cinemind.domain.rag.client.RagEmbeddingClient
import org.cinemind.domain.rag.dto.etc.MovieEmbeddingDto
import org.cinemind.domain.rag.dto.etc.MovieRagDto
import org.cinemind.domain.rag.entity.MovieEmbedding
import org.cinemind.domain.rag.repository.MovieEmbeddingRepository
import org.cinemind.util.TextSplitter
import org.springframework.stereotype.Service

@Service
class RagIndexingService (
    private val movieRepository: MovieRepository, // 원본 데이터 조회
    private val movieEmbeddingRepository: MovieEmbeddingRepository,  // 벡터 DB 저장
    private val ragEmbeddingClient: RagEmbeddingClient,  // 임베딩 클라이언트
    private val textSplitter: TextSplitter // 줄거리 청크 분할 로직
) {
    // 전체 영화 데이터를 RAG 벡터 스토어에 인덱싱
    @Transactional
    fun indexAllMovies() {
       // Fetch Join이 적용된 findAll()을 사용하여 N+1 방지
        val allMovies = movieRepository.findAll()

        // 모든 Movie 엔티티를 미리 조회하여 Map<Long, Movie>로 구성
        // 이후 MovieEmbedding 엔티티를 생성할 때 Movie 엔티티를 조회할 때 사용
        val allMoviesMap = allMovies.associateBy { it.id!! }

        // 전체 Movie 엔티티를 조회하고 MovieRagDto로 변환
        val movieRagDtos = allMovies.map { MovieRagDto.from(it) }

        // 기존 임베딩 데이터가 있다면 삭제
        movieEmbeddingRepository.deleteAll()

        val allEmbeddingEntities = mutableListOf<MovieEmbedding>()

        // 각 영화 DTO에 대해 청크 분할 및 임베딩 작업 수행
        movieRagDtos.forEach { dto ->
            // 청크를 생성하고 임베딩하는 작업 수행
            val embeddingDtos = createChunkAndEmbeddings(dto)

            // 생성된 임베딩 DTO들을 엔티티로 변환하여 저장
            val embeddingEntities = embeddingDtos.map { toEntity(it, allMoviesMap) }
            allEmbeddingEntities.addAll(embeddingEntities)
        }
        
        // 모든 엔티티를 모아서 한 번에 저장
        movieEmbeddingRepository.saveAll(allEmbeddingEntities)
    }

    // MovieRagDto를 기반으로 meta/plot 텍스트 청크를 생성하고 멀티-벡터 전략 임베딩을 수행하는 로직
    private fun createChunkAndEmbeddings(dto: MovieRagDto): List<MovieEmbeddingDto> {

        val movieId = dto.id ?: throw IllegalStateException("Movie ID cannot be null during indexing. Check if Movie entity has been persisted correctly.")

        // 메타데이터 텍스트 생성
        val metaText = createMetaText(dto)

        // 줄거리 텍스트 청크 분할: TextSplitter 사용
        val plotChunk: List<String> = if (dto.plot.isNullOrBlank()) {
            listOf("")
        } else {
            // TextSplitter를 사용하여 줄거리를 충커 단위로 분할
            textSplitter.splitText(dto.plot)
        }

        val embeddingList = mutableListOf<MovieEmbeddingDto>()

        plotChunk.forEachIndexed { index, plotChunk ->
            // 임베딩 클라이언트를 사용하여 벡터 생성
            // 텍스트가 비어있으면 클라이언트 내부에서 빈 벡터 문자열 반환
            val metaVector: FloatArray = ragEmbeddingClient.getEmbedding(metaText)
            val plotVector: FloatArray = ragEmbeddingClient.getEmbedding(plotChunk)

            // 벡터가 유효한지 확인 (빈 문자열이 아닌지)
            if (metaVector.isNotEmpty() || plotVector.isNotEmpty()) {
                embeddingList.add(
                    MovieEmbeddingDto(
                        id = null,  // 저장 시 자동 생성
                        movieId = movieId,
                        movieNm = dto.movieNm,
                        metaText = metaText,
                        plotText = plotChunk,
                        metaVector = metaVector,
                        plotVector = plotVector,
                        chunkOrder = index
                    )
                )
            }
        }

        return embeddingList
    }

    // MovieRagDto로부터 메타데이터 텍스트를 구성하는 함수
    private fun createMetaText(dto: MovieRagDto): String {

        // 박스오피스 통계를 문자열로 변환
        val boxOfficeString = if (dto.boxOfficeStats.isNotEmpty()) {
            "\n[박스오피스 통계]\n" + dto.boxOfficeStats.joinToString("\n")
        } else {
            ""
        }

        return """
            [영화 기본 정보]
            영화명: ${dto.movieNm} (${dto.movieNmEn ?: "정보 없음"})
            개봉: ${dto.openDt}, 상영시간: ${dto.showTm}분, 관람등급: ${dto.watchGradeNm}
            유형: ${dto.typeNm}
            장르: ${dto.genres.joinToString(", ")}
            감독: ${dto.directors.joinToString(", ")}
            배우: ${dto.actors.joinToString(" / ")}
            제작사: ${dto.companies.joinToString (" / ")}
            $boxOfficeString
        """.trimIndent().trim()
    }

    // DTO를 엔티티로 변환
    private fun toEntity(dto: MovieEmbeddingDto, movieMap: Map<Long, Movie>): MovieEmbedding {
        // DTO의 movieId를 사용하여 실제 Movie 엔티티 조회
        val movie = movieMap[dto.movieId]
            ?: throw NoSuchElementException("Movie not found with ID: ${dto.movieId}. 데이터 로딩 순서(Order)를 확인하세요.")

        return MovieEmbedding(
            movie = movie,
            metaText = dto.metaText,
            plotText = dto.plotText,
            metaVector = dto.metaVector,
            plotVector = dto.plotVector,
            chunkOrder = dto.chunkOrder
        )
    }
}