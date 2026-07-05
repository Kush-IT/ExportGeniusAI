FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/*.jar app.jar

# Run on port 8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
