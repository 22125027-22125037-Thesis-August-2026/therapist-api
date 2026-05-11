# Stage 1: Build
FROM gradle:8.6-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/project
WORKDIR /home/gradle/project
# Make the wrapper executable
RUN chmod +x ./gradlew
# Use the wrapper instead of the system gradle
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Run
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*.jar /app/app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-Xms128m", "-Xmx512m", "-XX:+UseSerialGC", "-jar", "app.jar"]