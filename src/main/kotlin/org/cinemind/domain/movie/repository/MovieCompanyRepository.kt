package org.cinemind.domain.movie.repository

import org.cinemind.domain.movie.entity.Company
import org.cinemind.domain.movie.entity.MovieCompany
import org.springframework.data.jpa.repository.JpaRepository

interface MovieCompanyRepository : JpaRepository<MovieCompany, Long>{
}