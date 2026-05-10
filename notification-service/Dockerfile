# syntax=docker/dockerfile:1
#
# Pre-built jar layout: run `./mvnw -DskipTests package` (or have CI run
# it) before `docker compose up --build`. Building inside an emulated
# multi-arch Maven image was unstable on the local Colima setup, so the
# build is kept on the host where it's cheap and fast.

FROM eclipse-temurin:21-jre
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY target/notification-service-*.jar app.jar
EXPOSE 8087
# -XX:TieredStopAtLevel=1 keeps the JVM on the C1 compiler. The C2
# compiler segfaults under qemu user-mode emulation (Apple Silicon →
# x86_64 Colima VM). Real ARM/Linux deployments can drop this flag.
ENTRYPOINT ["java", "-XX:TieredStopAtLevel=1", "-jar", "/app/app.jar"]
