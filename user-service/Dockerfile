# ================== BUILD STAGE ==================
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw

# Download dependencies first (better caching)
RUN ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw package -DskipTests

# ================== RUNTIME STAGE ==================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (change to 7001 for order-service)
EXPOSE 7000

# For better development experience
ENV JAVA_OPTS="-Dspring.devtools.restart.enabled=true"

# Run with extra options useful for development
ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Dspring.profiles.active=docker", \
            "-jar", \
            "app.jar"]