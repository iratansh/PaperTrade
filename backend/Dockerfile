# --- Build stage ---------------------------------------------------------
FROM gradle:8.5-jdk21 AS build
WORKDIR /app

# Only copy build inputs first for better layer caching.
# NOTE: gradle.properties is intentionally NOT copied - it pins a local JDK path
# that doesn't exist in the image (the gradle:8.5-jdk21 image already has JDK 21).
COPY build.gradle settings.gradle ./
COPY src ./src

RUN gradle bootJar --no-daemon

# --- Runtime stage -------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Run as a non-root user
RUN useradd -r -u 1001 appuser
USER appuser

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
