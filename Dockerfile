FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:17-jre
EXPOSE 8080
WORKDIR /app
COPY --from=build /app/build/libs/*-all.jar /app/ninjatalk.jar
ENTRYPOINT ["java", "-jar", "/app/ninjatalk.jar"]
