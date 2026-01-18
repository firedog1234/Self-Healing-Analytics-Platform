FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/lineage-service-*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
