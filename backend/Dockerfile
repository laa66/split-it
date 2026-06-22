# --- build deps cache layer ---
FROM eclipse-temurin:21-jdk AS deps
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN ./gradlew --no-daemon dependencies || true

# --- dev: bootRun, source mounted via volume for reload ---
FROM deps AS dev
WORKDIR /app
COPY . .
EXPOSE 8080
CMD ["./gradlew", "--no-daemon", "bootRun"]

# --- build the jar ---
FROM deps AS build
WORKDIR /app
COPY . .
RUN ./gradlew --no-daemon bootJar

# --- prod runtime ---
FROM eclipse-temurin:21-jre AS prod
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
