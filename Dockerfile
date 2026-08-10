# Multi-stage build: Maven compiles + shades SQLite JDBC into one runnable jar.
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/recruitment-crm.jar ./app.jar
COPY public ./public

ENV PORT=8080
ENV DATABASE_PATH=/app/data/crm.db
EXPOSE 8080

# Persist SQLite on a volume mount at /app/data when deploying.
VOLUME ["/app/data"]

CMD ["java", "-jar", "app.jar"]
