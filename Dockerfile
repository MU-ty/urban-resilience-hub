FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/urban-resilience-hub-*.jar app.jar
USER 10001
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
