FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/ai-ops-engine-*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]
