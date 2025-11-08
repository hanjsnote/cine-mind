package org.cinemind

import org.springframework.test.context.ActiveProfiles

/**
 * 모든 통합 테스트의 기반이 되는 추상 클래스.
 * 'test' 프로파일을 활성화하여 `application-test.yml`의 설정을 불러옵니다.
 */
@ActiveProfiles("test") // 'test' 프로파일을 활성화하여 application-test.yml을 로드
abstract class AbstractIntegrationTest {

}