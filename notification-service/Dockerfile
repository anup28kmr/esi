# syntax=docker/dockerfile:1
#
# Self-contained multi-stage build. Maven runs inside the build image,
# so teammates do not need to run `./mvnw package` on the host first —
# `docker compose up --build` does the whole thing.

# Build stage -- uses Maven image with JDK 21 pre-installed.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline || true
COPY src ./src
RUN mvn -B -DskipTests package

# Runtime stage -- JRE plus curl for the compose healthcheck.
FROM eclipse-temurin:21-jre
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/target/notification-service-*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
