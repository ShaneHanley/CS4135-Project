FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Install shared library dependency required by notification-service
COPY common-libs/common-messaging /workspace/common-libs/common-messaging
RUN mvn -q -DskipTests -f /workspace/common-libs/common-messaging/pom.xml install

# Build notification-service module
COPY services/notification-service /workspace/services/notification-service
RUN mvn -q -DskipTests -f /workspace/services/notification-service/pom.xml clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/notification-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
