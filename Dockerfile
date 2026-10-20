FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /srv
COPY --from=build /build/target/nevis-search.jar app.jar
EXPOSE 8000
ENV SERVER_PORT=8000
ENTRYPOINT ["java", "-jar", "app.jar"]
