# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . /app
RUN mvn -q -DskipTests package

# ---- run ----
FROM eclipse-temurin:21-jre
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75"
WORKDIR /opt/app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["sh","-c","java  -jar app.jar"]
