package org.cinemind.util

import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter


fun String.toLocalDate() : LocalDate {
    // KOFIC 날짜 형식
    val fomatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    return LocalDate.parse(this, fomatter)
}
// 두 날짜 사이의 모든 날짜를 포맷하여 리스트로 반환
@Component
class DateUtils {

    fun generateDate(startDate: String, endDate: String): List<String> {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        var startDt = LocalDate.parse(startDate, formatter)
        val endDt = LocalDate.parse(endDate, formatter)

        val dateList = mutableListOf<String>()
        // 조회하려는 시작 날짜(startDt)가 마지막 날짜(endDt)와 같을때까지 모든 날짜를 순차적으로 생성
        while (!startDt.isAfter(endDt)) {
            dateList.add(startDt.format(formatter))
            startDt = startDt.plusDays(1)
        }
        return dateList
    }
}