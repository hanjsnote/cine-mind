package org.cinemind.domain.movie.repository

import org.cinemind.domain.movie.entity.Company
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CompanyRepository : JpaRepository<Company, Long> {

    fun findAllByCompanyCdIn(companyCds: List<String>): List<Company>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            INSERT INTO companies (company_cd, company_nm, company_nm_en, created_at, modified_at)
            SELECT t.cd, t.nm, t.en, now(), now()
            FROM unnest(
                CAST(:cds AS text[]),
                CAST(:nms AS text[]),
                CAST(:ens AS text[])
            ) AS t(cd, nm, en)
            ON CONFLICT (company_cd) DO UPDATE
            SET company_nm = EXCLUDED.company_nm,
                company_nm_en = COALESCE(NULLIF(EXCLUDED.company_nm_en, ''), companies.company_nm_en),
                modified_at = now()
        """,
        nativeQuery = true
    )
    fun upsertAll(
        @Param("cds") cds: Array<String>,
        @Param("nms") nms: Array<String>,
        @Param("ens") ens: Array<String>
    ): Int
}