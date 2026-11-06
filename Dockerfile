FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package dependency:copy-dependencies \
    -DincludeScope=runtime \
    -DoutputDirectory=/workspace/target/dependency

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build --chown=10001:10001 /workspace/target/classes ./classes
COPY --from=build --chown=10001:10001 /workspace/target/dependency/ ./lib/

USER 10001:10001

ENTRYPOINT ["java", "-cp", "classes:lib/*", "es.activa.main.ActivaApplication"]
