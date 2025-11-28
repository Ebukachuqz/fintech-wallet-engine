# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Build the JAR, skipping tests to save time during deployment
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM openjdk:25-ea-21-jdk-slim
WORKDIR /app
# Copy the built JAR from the previous stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]