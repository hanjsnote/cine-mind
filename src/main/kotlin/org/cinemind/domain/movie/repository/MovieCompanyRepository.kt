package org.cinemind.domain.movie.repository

import org.cinemind.domain.movie.entity.Company
import org.springframework.data.jpa.repository.JpaRepository

interface MovieCompanyRepository : JpaRepository<Company, Long>{
}