package org.cinemind.domain.rag.service

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.cinemind.common.dto.authuser.AuthUser
import org.cinemind.common.exception.CommonErrorCode
import org.cinemind.common.exception.GlobalException
import org.cinemind.domain.movie.repository.MovieRepository
import org.cinemind.domain.rag.client.RagEmbeddingClient
import org.cinemind.domain.rag.dto.model.MovieRagDto
import org.cinemind.domain.rag.entity.MovieEmbedding
import org.cinemind.domain.rag.repository.MovieEmbeddingRepository
import org.cinemind.util.PlotTextSplitter
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class RagIndexingService (
    private val movieRepository: MovieRepository, // 원본 데이터 조회
    private val movieEmbeddingRepository: MovieEmbeddingRepository,  // 벡터 DB 저장
    private val ragEmbeddingClient: RagEmbeddingClient,  // 임베딩 클라이언트
    private val plotTextSplitter: PlotTextSplitter, // 줄거리 청크 분할 로직
    private val transactionTemplate: TransactionTemplate,   // 배치별 커밋
    private val entityManager: EntityManager    // flush/clear로 영속성 컨텍스트
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // OOM 방지를 위해 배치 크기 설정 (임베딩 API 부하와 메모리 사용량에 따라 조정 가능)
    private val BATCH_SIZE = 20

    // 전체 영화 데이터를 RAG 벡터 스토어에 인덱싱
    // 이 메서드는 외부 스케줄러에서 직접 호출되어 모든 작업을 감싸는 메인 트랜잭션 역할
    @Transactional
    fun rebuildAllIndexes(authUser: AuthUser): Int {
        log.warn("!!! [RAG] 전체 벡터 인덱스 재구축 시작: 기존 임베딩 전부 삭제 후 전체 재인덱싱 !!!")

        // 1) 기존 임베딩 삭제를 "먼저 커밋" (아주 중요)
        transactionTemplate.execute {
            // deleteAll()도 되지만, 대량이면 deleteAllInBatch()가 더 빠를 때가 많음
            movieEmbeddingRepository.deleteAllInBatch()
        }

        // 2) 임베딩 테이블이 비었으니, "인덱싱 안 된 영화" = 전체 영화
        val total = indexNotIndexedMoviesLoop(caller = authUser.id.toString())
        log.info("[RAG] 전체 재구축 완료. 총 {}개의 영화가 인덱싱되었습니다.", total)
        return total
    }

    /**
     * 증분 인덱싱
     * - MovieEmbedding이 없는 영화만 배치로 찾아서 인덱싱
     */
    fun indexNewMovies(authUser: AuthUser?): Int {
        val caller: String = authUser?.id?.toString() ?: "Scheduler"
        log.info("[RAG] {}가 증분 인덱싱을 시작합니다.", caller)

        val total = indexNotIndexedMoviesLoop(caller = caller)
        log.info("[RAG] 증분 인덱싱 완료. 총 {}개의 영화가 인덱싱되었습니다.", total)
        return total
    }

     // 아직 인덱싱 안 된 영화를 배치로 계속 처리하는 공통 루프
    private fun indexNotIndexedMoviesLoop(caller: String): Int {
        var totalIndexedMovieCount = 0

        while (true) {
            // 항상 0페이지를 조회 (처리될 때마다 NOT EXISTS 대상이 줄어들기 때문)
            val idPage = movieRepository.findNotIndexedMovieIds(
                PageRequest.of(
                    0,
                    BATCH_SIZE,
                    Sort.by(Sort.Direction.ASC, "id")
                )
            )

            if (idPage.isEmpty) {
                log.info("[RAG] {} 기준: 더 이상 인덱싱할 영화가 없습니다. 루프 종료.", caller)
                break
            }

            val ids = idPage.content
            val movies = movieRepository.findAllWithRelationsByIdIn(ids)

            // 배치마다 "별도 트랜잭션"으로 커밋되게 실행
            val indexedInBatch = transactionTemplate.execute {
                processAndSaveBatch(movies)
            } ?: 0

            totalIndexedMovieCount += indexedInBatch
            log.info("[RAG] {} 기준: 배치 처리 완료 (영화 {}건 인덱싱). 누적={}", caller, indexedInBatch, totalIndexedMovieCount)
        }

        return totalIndexedMovieCount
    }

    /**
     * 배치 저장 (트랜잭션 안에서 호출됨)
     *
     * - 여기서 영화들을 DTO로 변환하고, plot을 쪼개고, 임베딩을 만들고, 엔티티를 생성해서 저장
     * - 저장 후 flush/clear로 영속성 컨텍스트 메모리를 비움 (OOM 방지)
     */
    private fun processAndSaveBatch(movies: List<org.cinemind.domain.movie.entity.Movie>): Int {
        if (movies.isEmpty()) return 0

        val embeddingEntities = mutableListOf<MovieEmbedding>()
        var indexedMovieCount = 0

        for (movie in movies) {
            try {
                val dto = MovieRagDto.from(movie)
                val movieId = dto.id ?: throw GlobalException(CommonErrorCode.RAG_DATA_MISSING)

                // metaText 생성
                val metaText = createMetaText(dto)

                // metaVector는 "영화당 1번만" 생성 (중요)
                val metaVector = ragEmbeddingClient.getEmbedding(metaText)

                // plot 청크 분할
                val plotChunks: List<String> =
                    if (dto.plot.isNullOrBlank()) listOf("")
                    else plotTextSplitter.splitText(dto.plot)

                // plotChunk마다 plotVector 생성 + 엔티티 생성
                plotChunks.forEachIndexed { index, plotChunk ->
                    val plotVector = ragEmbeddingClient.getEmbedding(plotChunk)

                    // 벡터 둘 다 비면 저장하지 않음
                    if (metaVector.isEmpty() && plotVector.isEmpty()) return@forEachIndexed

                    embeddingEntities.add(
                        MovieEmbedding(
                            movie = movie,
                            metaText = metaText,
                            plotText = plotChunk,
                            metaVector = metaVector,
                            plotVector = plotVector,
                            chunkOrder = index
                        )
                    )
                }

                indexedMovieCount++
            } catch (e: Exception) {
                log.error("[RAG] Movie id=${movie.id}, name=${movie.movieNm} 인덱싱 실패: {}", e.message)
            }
        }

        // 5) 저장 (배치 INSERT)
        if (embeddingEntities.isNotEmpty()) {
            movieEmbeddingRepository.saveAll(embeddingEntities)
            movieEmbeddingRepository.flush()   // DB로 밀어넣기
            entityManager.clear()              // 1차 캐시 비우기(메모리 절약)
        }

        return indexedMovieCount
    }

    /**
     * MovieRagDto로부터 메타데이터 텍스트를 구성하는 함수
     */
    private fun createMetaText(dto: MovieRagDto): String {
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
            제작사: ${dto.companies.joinToString(" / ")}
            $boxOfficeString
        """.trimIndent().trim()
    }
}