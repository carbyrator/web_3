# Сначала собираем проект с Maven и JDK
FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp package

# В итоговом контейнере нужны только Java и собранное приложение
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/students-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
