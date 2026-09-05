# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

# Expo records AAC/M4A on devices and WebM in browsers. Normalize both to the
# 16 kHz mono PCM WAV required by Azure Pronunciation Assessment.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg libgomp1 libssl3 ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system japlearn && useradd --system --gid japlearn --create-home japlearn
COPY --from=build /workspace/target/demo-0.0.1-SNAPSHOT.jar app.jar

USER japlearn
EXPOSE 10000

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
