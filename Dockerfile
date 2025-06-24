# ┌──────────────────────── Build Stage ────────────────────────┐
FROM gradle:7.6-jdk21 AS builder
WORKDIR /home/gradle/project

# gradle 설정 복사
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew

# 소스 복사 후 Jar 빌드
COPY src ./src
RUN ./gradlew bootJar --no-daemon

# └─────────────────────── End Build Stage ────────────────────┘


# ┌──────────────────────── Runtime Stage ──────────────────────┐
FROM openjdk:21-jdk-slim
WORKDIR /app

# 빌드 스테이지에서 생성된 JAR만 복사
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
# └─────────────────────── End Runtime Stage ───────────────────┘

