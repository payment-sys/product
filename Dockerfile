FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build
COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
