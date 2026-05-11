# syntax=docker/dockerfile:1

# Build stage -- uses Maven image with JDK 21 so the project can compile with release 21.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline || true
COPY src ./src
RUN mvn -B -DskipTests package

# Runtime stage -- Java 21 JRE plus curl for the compose healthcheck.
FROM eclipse-temurin:21-jre
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
