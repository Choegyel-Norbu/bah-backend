## Multi-stage Dockerfile for AttireHub Spring Boot (Java 21)

# ====== Build stage ======
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy Maven descriptor and resolve dependencies first (better layer caching)
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Copy source code and build the application
COPY src ./src
RUN mvn -q -B -DskipTests package


# ====== Runtime stage ======
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/attirehub-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose the internal application port (Render will map its own $PORT to this)
EXPOSE 8080

# Default profile can be overridden by Render env var if needed
ENV SPRING_PROFILES_ACTIVE=prod

# Bind Spring Boot to the port provided by Render ($PORT).
# If PORT is not set (local runs), default to 8080.
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]

