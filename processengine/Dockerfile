#FROM cibseven/cibseven:latest
#COPY src/main/resources/processes /camunda/configuration/resources/processes
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/cibseven-spring-boot-oidc-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]