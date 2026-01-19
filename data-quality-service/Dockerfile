FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/data-quality-service-*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
