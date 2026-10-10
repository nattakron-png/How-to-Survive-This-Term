# build context คือ root ของ repo (ดู compose.yaml และ railway.json)
FROM maven:3.9-eclipse-temurin-21 AS development
WORKDIR /workspace/code/backend
COPY code/backend/pom.xml .
RUN mvn -B dependency:go-offline
COPY code/backend/src ./src
COPY test/backend /workspace/test/backend
CMD ["mvn", "-B", "spring-boot:run"]

FROM development AS build
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app app \
    && mkdir -p /app/uploads/logos \
    && chown -R app:app /app/uploads
WORKDIR /app
COPY --from=build /workspace/code/backend/target/tournament-0.0.1-SNAPSHOT.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
