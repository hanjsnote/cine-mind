package org.cinemind.domain.movie.repository

import org.cinemind.domain.movie.entity.People
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PeopleRepository : JpaRepository<People, Long> {

    fun findAllByPeopleNmIn(peopleNames: List<String>): List<People>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            INSERT INTO peoples (people_nm, people_nm_en, created_at, modified_at)
            SELECT t.nm, t.en, now(), now()
            FROM unnest(CAST(:names AS text[]), CAST(:ens AS text[])) AS t(nm, en)
            ON CONFLICT (people_nm) DO UPDATE
            SET people_nm_en = CASE 
                WHEN peoples.people_nm_en = '' AND EXCLUDED.people_nm_en <> '' THEN EXCLUDED.people_nm_en
                ELSE peoples.people_nm_en
            END,
            modified_at = now()
        """,
        nativeQuery = true
    )
    fun upsertAll(
        @Param("names") names: Array<String>,
        @Param("ens") ens: Array<String>
    ): Int
}