# syntax=docker/dockerfile:1

# 1단계: 빌드용 컨테이너
FROM eclipse-temurin:17-jdk AS builder

# 작업 디렉토리
WORKDIR /app

# Gradle wrapper & 설정 파일들 복사
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

# 소스 코드 복사
COPY src src

# 실행 권한
RUN chmod +x ./gradlew

# JAR 빌드 (테스트는 일단 스킵해서 속도 ↑)
RUN ./gradlew clean bootJar -x test


# 2단계: 런타임 컨테이너
FROM eclipse-temurin:17-jre

WORKDIR /app

# 빌드 단계에서 만들어진 JAR만 가져옴
# build/libs 안에 JAR이 하나라는 전제 (지금 구조 그대로라면 OK)
COPY --from=builder /app/build/libs/*.jar app.jar

# 스프링 부트 기본 포트
EXPOSE 8080

# 프로파일은 여기서 강제로 고정하지 말고,
# docker run 할 때 또는 나중에 docker-compose에서 환경변수/파라미터로 넘기는 쪽이 더 유연함.
#
# 예: docker run ... -e "SPRING_PROFILES_ACTIVE=local"
# ENV SPRING_PROFILES_ACTIVE=local

ENTRYPOINT ["java", "-jar", "/app/app.jar"]