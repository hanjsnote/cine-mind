package org.cinemind.util

/**
 * RAG 시스템을 위한 재귀적 문자 분할(Recursive Character Splitter) 클래스입니다.
 * 이 클래스는 langchain4j-kotlin을 직접 사용할 수 없을 때, 사용합니다.
 * 문맥의 손실을 최소화하기 위해 '의미 있는 구분자'를 우선순위로 사용하여 텍스트를 분할합니다.
 */
class TextSplitter (
    private val chunkSize: Int,
    private val chunkOverlap: Int,
    // 구분자 우선순위, 1. 문단 (\n\n) -> 2. 문장 끝 (. ? !) -> 3. 공백 ( ) -> 4. 문자열 단위 ('')
    private val separators: List<String> = listOf("\n\n", ". ", "? ", "! ", " ", "")
) {
    // 텍스트를 지정된 크기와 중첩을 사용하여 청크 목록으로 분할
    fun splitText(text: String): List<String> {
        val trimmedText = text.trim()
        if(trimmedText.isEmpty()) return emptyList()

        // 재귀 분할 시작
        val rawSplits = recursiveSplit(trimmedText, separators)

        // 최종적으로 생성된 청크 목록에 오버랩을 적용하여 반환
        return applyOverlap(rawSplits)
            .filter {it.isNotBlank() }
            .map { it.trim() }
            .distinct()
    }

    // 재귀적 분할을 시도하는 내부 함수, text 현재 분할할 텍스트, currentSeparators 현재 시도할 구분자 목록, return 분할된 청크 목록
    private fun recursiveSplit(text: String, currentSeparators: List<String>): List<String> {
        // 1. 텍스트가 이미 청크 크기 이하라면 그대로 반환
        if (text.length <= chunkSize) {
            return listOf(text)
        }

        // 2. 사용할 구분자를 찾음
        val separatorIndex = currentSeparators.indexOfFirst { text.contains(it) }

        // 더 이상 효과적인 구분자가 없는 경우 (또는 마지막이 ""인 경우)
        if (separatorIndex == -1 || currentSeparators[separatorIndex].isEmpty()) {
            // 강제로 문자 단위 분할 시도
            return splitByCharacters(text)
        }

        val separator = currentSeparators[separatorIndex]
        val remainingSeparators = currentSeparators.drop(separatorIndex + 1)

        // 3. 텍스트를 구분자로 나눔
        val parts = text.split(separator)

        // 4. 나뉜 조각들을 합쳐 청크 생성 (현재 구분자를 사용하여 병합 시도)
        val chunks = mutableListOf<String>()
        var currentChunk: String? = null

        for (part in parts) {
            val segment = part.trim()
            if (segment.isEmpty()) continue

            // 5. 각 조기이 너무 크면 다음 구분자로 재귀 호출
            if (segment.length > chunkSize) {
                // 현재까지 모은 청크를 확정하고
                if (currentChunk != null) {
                    chunks.add(currentChunk)
                    currentChunk = null
                }
                // 큰 조각을 다음 단계의 구분자로 재귀 분할하여 추가
                chunks.addAll(recursiveSplit(segment, remainingSeparators))
                continue
            }

            // 6. 현재 조각을 이전 청크에 병합 시도
            val potentialNewChunk = if (currentChunk == null) {
                segment // 첫 시작
            } else {
                // 이전 청크의 오버랩 부분과 현재 구분자 + 현재 조각을 합침 (문맥 보존)
                val overlap = currentChunk.takeLast(chunkOverlap)
                overlap + separator + segment
            }

            if (potentialNewChunk.length <= chunkSize) {
                // 청크 크기 제한 내라면 계속 병합
                currentChunk = potentialNewChunk
            } else {
                // 청크 크기 초과: 이전 청크를 확정하고 새 청크를 시작
                if (currentChunk != null) {
                    chunks.add(currentChunk)
                }

                // 새 청크는 현재 조각으로 시작하며 이 때 이전 청크의 오버랩을 붙임
                val overlap = if (currentChunk != null) currentChunk.takeLast(chunkOverlap) else ""
                currentChunk = overlap + segment
            }
        }
        // 7. 마지막 청크 추가
        if (currentChunk != null) {
            chunks.add(currentChunk)
        }
        return chunks
    }

    // 강제로 문자 단위 분할 수행
    private fun splitByCharacters(text: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            val end = (i + chunkSize).coerceAtMost(text.length)

            // 청크 생성
            val chunk = text.substring(i, end)
            result.add(chunk)

            // 다음 시작 지점 계산 (오버랩 적용)
            i += chunkSize - chunkOverlap
            // 다음 시작 지점이 텍스트 끝을 초과하지 않도록 보정
            if (i > text.length - chunkOverlap) break
        }
        return result
    }

    // 최종적으로 생성된 청크 목록에 오버랩을 적용하여 반환
    private fun applyOverlap(splits: List<String>): List<String> {
        if (chunkOverlap <= 0 || splits.size <= 1) return splits

        val finalChunks = mutableListOf<String>()

        // 첫 번째 청크는 그대로 추가
        finalChunks.add(splits.first())

        // 두 번째 청크부터 오버랩 적용하여 추가
        for (i in 1 until splits.size) {
            val previousChunk = splits[i - 1]
            val currentChunk = splits[i]

            // 이전 청크의 끝 부분을 가져와 현재 청크 앞에 붙임
            val overlapText = previousChunk.substring(
                maxOf(0, previousChunk.length - chunkOverlap)
            )
            finalChunks.add(overlapText + currentChunk)
        }
        return finalChunks
    }
}
