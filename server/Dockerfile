FROM eclipse-temurin:21-jre AS base

# Runtime image expects a built distribution under /app
WORKDIR /app
COPY build/install/server/ .

ENV PORT=8080
EXPOSE 8080

CMD ["bin/server"]

# ---
# If you prefer a single-step build in CI, you can use this multi-stage:
# FROM gradle:8.7-jdk21 AS build
# WORKDIR /workspace
# COPY . .
# RUN ./gradlew :server:installDist --no-daemon
# FROM eclipse-temurin:21-jre
# WORKDIR /app
# COPY --from=build /workspace/server/build/install/server/ .
# ENV PORT=8080
# EXPOSE 8080
# CMD ["bin/server"]
