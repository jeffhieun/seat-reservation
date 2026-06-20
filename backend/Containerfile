FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built Spring Boot JAR
COPY build/libs/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
