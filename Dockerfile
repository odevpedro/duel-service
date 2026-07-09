FROM gradle:8.14.0-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends \
    libc6 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
COPY --from=build /app/src/main/resources/native/ /app/native/

ENV LD_LIBRARY_PATH=/app/native:${LD_LIBRARY_PATH}

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
