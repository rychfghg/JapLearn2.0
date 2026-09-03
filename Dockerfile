# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache ffmpeg && addgroup -S japlearn && adduser -S japlearn -G japlearn
COPY --from=build /workspace/target/demo-0.0.1-SNAPSHOT.jar app.jar

USER japlearn
EXPOSE 10000

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
