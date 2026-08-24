FROM docker.io/library/eclipse-temurin:24-jdk AS build
WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle* .
COPY settings.gradle* .
RUN ./gradlew --no-daemon dependencies

COPY . .
RUN ./gradlew bootJar -x test

FROM docker.io/library/eclipse-temurin:24-jdk-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar ./application.jar
EXPOSE 8080
CMD ["java","-jar","application.jar"]