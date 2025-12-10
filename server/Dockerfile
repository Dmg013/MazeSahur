FROM eclipse-temurin:21-jre AS base

# Runtime image expects the fat JAR under /app
WORKDIR /app
COPY build/libs/server-0.0.1-SNAPSHOT.jar server.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/server.jar"]

# ---
# If you prefer a single-step build in CI, you can use this multi-stage:
# FROM gradle:8.7-jdk21 AS build
# WORKDIR /workspace
# COPY . .
# RUN ./gradlew :server:jar --no-daemon
# FROM eclipse-temurin:21-jre
# WORKDIR /app
# COPY --from=build /workspace/server/build/libs/server-0.0.1-SNAPSHOT.jar /app/server.jar
# ENV PORT=8080
# EXPOSE 8080
# ENTRYPOINT ["java","-jar","/app/server.jar"]
