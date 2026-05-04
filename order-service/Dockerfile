# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy the mvnw, .mvn, and pom.xml files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Give execution permission to mvnw
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy the source code
COPY src src

# Build the application
RUN ./mvnw package -DskipTests

# Use a smaller runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the jar file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port
EXPOSE 7001

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]