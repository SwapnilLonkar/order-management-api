# Build stage: compile and package the Spring Boot app.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn ./.mvn
COPY mvnw .
COPY src ./src
RUN mvn -B package -DskipTests

# Runtime stage: slim JRE image with just the packaged jar.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
