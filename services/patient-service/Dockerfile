FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY common-libs/common-messaging /workspace/common-libs/common-messaging
RUN mvn -q -DskipTests -f /workspace/common-libs/common-messaging/pom.xml install
COPY services/patient-service /workspace/services/patient-service
RUN mvn -q -DskipTests -f /workspace/services/patient-service/pom.xml clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/patient-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
