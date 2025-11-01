package org.cinemind.domain.kofic.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

// KMDb API와 통신을 담당하는 클라이언트
@Component
class KmdbApiClient (
    @Value("\${KMDB_API_KEY}")
    private val apiKey: String,
    private val webClientBuilder: WebClient.Builder
){
    private val baseUrl = "http://api.koreafilm.or.kr/openapi-data2/wisenut/search_json2"
    private val webClient = webClientBuilder.baseUrl(baseUrl).build()
}