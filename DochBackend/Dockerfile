# Dockerfile

# --- Stage 1: Build the application ---
# Use a Maven-based image with OpenJDK 25 to build our project
FROM maven:3.9-eclipse-temurin-25 AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml file first to leverage Docker's layer caching.
# If pom.xml doesn't change, Docker won't re-download dependencies.
COPY pom.xml .

# Download all dependencies
RUN mvn dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Package the application, skipping the tests for faster build
RUN mvn package -DskipTests

# --- Stage 2: Create the final, lightweight runtime image ---
# Use a lightweight JRE image for a smaller footprint
FROM eclipse-temurin:25-jre-jammy

# Set the working directory
WORKDIR /app

# Copy the executable JAR file from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port the application runs on
EXPOSE 8080

# The command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]