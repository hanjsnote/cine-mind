package org.cinemind.domain.movie.repository

import org.cinemind.domain.movie.entity.Movie
import org.springframework.data.jpa.repository.JpaRepository

interface MovieRepository : JpaRepository<Movie, Long>{
    fun existsByMovieCd(movieCd: String): Boolean

    // KoficDataSyncService의 saveOrFindMovieData 함수에서 사용: 영화 코드로 Movie 엔티티를 조회
    fun findByMovieCd(movieCd: String): Movie?

    // 챗봇 영화명 키워드 검색 (대소문자 구분 없이 포함 검색
    fun findByMovieNmContainingIgnoreCase(keyword: String): List<Movie>
}