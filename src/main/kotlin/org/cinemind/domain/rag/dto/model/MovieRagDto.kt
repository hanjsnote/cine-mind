package org.cinemind.domain.rag.dto.model

import org.cinemind.domain.movie.entity.Movie
import org.cinemind.domain.movie.enums.PeopleRole

/**
 * RAG Context 생성을 위해 Movie 엔티티에서 필요한 모든 정보와 연결되는 DTO
 **/
data class MovieRagDto(
    val id: Long?,
    val movieCd: String,
    val movieNm: String,
    val movieNmEn: String?,
    val showTm: Int,
    val openDt: String,
    val typeNm: String,
    val watchGradeNm: String,
    val plot: String?,
    val genres: List<String>,
    val directors: List<String>,
    val actors: List<String>,
    val companies: List<String>,
    val boxOfficeStats: List<String>
) {
    companion object {
        // 엔티티를 DTO로 변환하는 정적 메서드
        fun from(movie: Movie): MovieRagDto {
            // 장르 이름 추출
            val genresNm = movie.movieGenre.map { it.genre.genreNm }

            // 감독 이름 추출
            val directorNm = movie.moviePeople
                .filter { it.role == PeopleRole.DIRECTOR }
                .map { it.people.peopleNm }

            // 배우 이름 및 배역명 추출
            val actorInfo = movie.moviePeople
                .filter { it.role == PeopleRole.ACTOR }
                .map { "${it.people.peopleNm} (${it.castNm})" }

            // 회사 이름 및 참여 분야 추출
            val companyInfo = movie.movieCompany
                .map { "${it.company.companyNm} (${it.companyPartNm})" }

            val latestBoxOffice = movie.boxOffice.maxByOrNull { it.targetDt }

            val boxOfficeStats = if (latestBoxOffice != null) {
                listOf(
                    "최신 순위: ${latestBoxOffice.rank}위",
                    "누적 관객수: ${latestBoxOffice.audiAcc}명",
                    "누적 매출액: ${latestBoxOffice.saleAccess}원"
                )
            } else {
                emptyList()
            }

            return MovieRagDto(
                id = movie.id,
                movieCd = movie.movieCd,
                movieNm = movie.movieNm,
                movieNmEn = movie.movieNm,
                showTm = movie.showTm,
                openDt = movie.openDt,
                typeNm = movie.typeNm,
                watchGradeNm = movie.watchGradeNm,
                plot = movie.plot,
                genres = genresNm,
                directors = directorNm,
                actors = actorInfo,
                companies = companyInfo,
                boxOfficeStats = boxOfficeStats,
            )
        }
    }
}