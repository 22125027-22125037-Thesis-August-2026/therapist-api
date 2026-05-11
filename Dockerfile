# Stage 1: Build the application
FROM gradle:8.6-jdk17 AS build
# Make sure this path matches where Gradle builds (typically /home/gradle/src or /home/gradle/project)
COPY --chown=gradle:gradle . /home/gradle/project
WORKDIR /home/gradle/project
RUN gradle bootJar -x test --no-daemon

# Stage 2: Run the application
# FIX: Use the standard '17-jre' tag
FROM eclipse-temurin:17-jre 
WORKDIR /app
# Ensure the path below matches the Stage 1 WORKDIR
COPY --from=build /home/gradle/project/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]