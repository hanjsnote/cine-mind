package org.cinemind.domain.rag.service

import jakarta.transaction.Transactional
import org.cinemind.common.dto.authuser.AuthUser
import org.cinemind.common.exception.CommonErrorCode
import org.cinemind.common.exception.GlobalException
import org.cinemind.domain.movie.entity.Movie
import org.cinemind.domain.movie.repository.MovieRepository
import org.cinemind.domain.rag.client.RagEmbeddingClient
import org.cinemind.domain.rag.dto.model.MovieEmbeddingDto
import org.cinemind.domain.rag.dto.model.MovieRagDto
import org.cinemind.domain.rag.entity.MovieEmbedding
import org.cinemind.domain.rag.repository.MovieEmbeddingRepository
import org.cinemind.util.PlotTextSplitter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RagIndexingService (
    private val movieRepository: MovieRepository, // 원본 데이터 조회
    private val movieEmbeddingRepository: MovieEmbeddingRepository,  // 벡터 DB 저장
    private val ragEmbeddingClient: RagEmbeddingClient,  // 임베딩 클라이언트
    private val plotTextSplitter: PlotTextSplitter // 줄거리 청크 분할 로직
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 전체 영화 데이터를 RAG 벡터 스토어에 인덱싱
    @Transactional
    fun rebuildAllIndexes(authUser: AuthUser): Int {
        log.warn("!!! [RAG] 전체 벡터 인덱스 재구축을 시작합니다. 기존 데이터 삭제 후 모든 영화를 대상으로 수행됩니다. !!!")

        // 기존 임베딩 데이터가 있다면 삭제
        movieEmbeddingRepository.deleteAll()

        // Fetch Join이 적용된 findAll()을 사용하여 N+1 방지
        val allMovies = movieRepository.findAll()

        // 모든 Movie 엔티티를 미리 조회하여 Map<Long, Movie>로 구성
        // 이후 MovieEmbedding 엔티티를 생성할 때 Movie 엔티티를 조회할 때 사용
        val allMoviesMap = allMovies.associateBy { it.id!! }

        // 전체 Movie 엔티티를 조회하고 MovieRagDto로 변환
        val movieRagDtos = allMovies.map { MovieRagDto.from(it) }

        val indexedCount = processAndSaveEmbeddings(movieRagDtos, allMoviesMap)

        log.info("[RAG] 전체 인덱스 재구축 완료. 총 {}개의 영화가 인덱싱되었습니다.", indexedCount)
        return indexedCount
    }

    // Movie 테이블에는 있지만 MovieEmbedding 테이블에는 없는 (신규) 영화만 찾아서 인덱싱
    @Transactional
    fun indexNewMovies(authUser: AuthUser?): Int {
        val caller = authUser?.id ?: "Scheduler" // 호출 주체 구분
        log.info("[RAG] {}가 신규 영화 데이터 증분 인덱싱을 시작합니다.", caller)

        // 이미 인덱싱된 영화 ID 목록을 조회
        val existingMovieIds = movieEmbeddingRepository.findAll().mapNotNull { it.movie.id }.toSet()

        // 전체 영화 목록 중 인덱싱 되지 않은 영화를 필터링
        val newMovies = movieRepository.findAll().filter {
            it.id != null && !existingMovieIds.contains(it.id)
        }

        if (newMovies.isEmpty()) {
            log.info("[RAG] 새로 인덱싱할 영화 데이터가 없습니다. 증분 인덱싱을 종료합니다.")
            return 0
        }

        log.info("[RAG] 총 {}개의 신규 영화를 대상으로 인덱싱을 진행합니다.", newMovies.size)

        val newMoviesMap = newMovies.associateBy { it.id!! }
        val newMovieRagDtos = newMovies.map { MovieRagDto.from(it) }

        val indexedCount = processAndSaveEmbeddings(newMovieRagDtos, newMoviesMap)

        log.info("[RAG] 신규 영화 증분 인덱싱 완료. 총 {}개의 영화가 인덱싱되었습니다.", indexedCount)
        return indexedCount
    }

    // 인덱싱 로직의 중복을 제거하기 위한 공통 함수
    private fun processAndSaveEmbeddings(movieRagDtos: List<MovieRagDto>, movieMap: Map<Long, Movie>): Int {
        val allEmbeddingEntities = mutableListOf<MovieEmbedding>()
        var indexedMovieCount = 0

        movieRagDtos.forEach { dto ->
            try {
                // 청크를 생성하고 임베딩하는 작업 수행
                val embeddingDtos = createChunkAndEmbeddings(dto)

                // 생성된 임베딩 DTO들을 엔티티로 변환하여 저장
                val embeddingEntities = embeddingDtos.map { toEntity(it, movieMap) }
                allEmbeddingEntities.addAll(embeddingEntities)
                indexedMovieCount++
            } catch (e: Exception) {
                log.error("Movie ID ${dto.id} (${dto.movieNm}) 인덱싱 중 오류 발생: {}", e.message)
                // 오류가 발생한 영화는 건너뛰고 다음 영화를 처리
            }
        }

        // 모든 엔티티를 모아서 한 번에 저장
        movieEmbeddingRepository.saveAll(allEmbeddingEntities)
        return indexedMovieCount
    }

    // MovieRagDto를 기반으로 meta/plot 텍스트 청크를 생성하고 멀티-벡터 전략 임베딩을 수행하는 로직
    private fun createChunkAndEmbeddings(dto: MovieRagDto): List<MovieEmbeddingDto> {

        val movieId = dto.id ?: throw GlobalException(CommonErrorCode.RAG_DATA_MISSING)

        // 메타데이터 텍스트 생성
        val metaText = createMetaText(dto)

        // 줄거리 텍스트 청크 분할: TextSplitter 사용
        val plotChunk: List<String> = if (dto.plot.isNullOrBlank()) {
            listOf("")
        } else {
            // TextSplitter를 사용하여 줄거리를 청크 단위로 분할
            plotTextSplitter.splitText(dto.plot)
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
                        movieCd = dto.movieCd,
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
            ?: throw GlobalException(CommonErrorCode.MOVIE_NOT_FOUND_FOR_RAG)

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