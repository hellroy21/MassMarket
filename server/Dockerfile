FROM gradle:8.11.1-jdk21 AS build
WORKDIR /project
COPY . .
RUN --mount=type=cache,target=/home/gradle/.gradle gradle :server:buildFatJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /project/server/build/libs/server-all.jar /app/server.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/server.jar"]
