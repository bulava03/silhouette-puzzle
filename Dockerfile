# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies separately from sources for faster rebuilds
COPY pom.xml checkstyle.xml ./
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --no-create-home appuser
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
