FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/lineage-service-*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
