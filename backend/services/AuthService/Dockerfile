## Stage 1 : build with maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
COPY pom.xml /usr/src/app/
COPY src /usr/src/app/src
WORKDIR /usr/src/app
RUN mvn package -DskipTests

## Stage 2 : create the docker final image
FROM eclipse-temurin:17-jre-jammy
ENV QUARKUS_LAUNCH_DEVMODE=false
ENV JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
WORKDIR /deployments/
COPY --from=build /usr/src/app/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build /usr/src/app/target/quarkus-app/*.jar /deployments/
COPY --from=build /usr/src/app/target/quarkus-app/app/ /deployments/app/
COPY --from=build /usr/src/app/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080
USER 185
ENV AB_JOLOKIA_OFF=""
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT [ "java", "-jar", "/deployments/quarkus-run.jar" ]
