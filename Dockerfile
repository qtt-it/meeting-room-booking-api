# syntax=docker/dockerfile:1
FROM eclipse-temurin:22-jdk-jammy AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY libs/ libs/
COPY apps/ apps/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp package -DskipTests

FROM eclipse-temurin:22-jre-jammy AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 meeting \
    && useradd --uid 10001 --gid meeting --no-create-home meeting
WORKDIR /app
COPY --from=build --chown=meeting:meeting /workspace/apps/meeting-api/target/meeting-api.jar /app/meeting-api.jar
USER 10001:10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Duser.timezone=UTC"
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD curl --fail --silent http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java","-jar","/app/meeting-api.jar"]
