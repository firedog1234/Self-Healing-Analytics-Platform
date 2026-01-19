FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/incident-store-service-*.jar app.jar

EXPOSE 8087

ENTRYPOINT ["java", "-jar", "app.jar"]
