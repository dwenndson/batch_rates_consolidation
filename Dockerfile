FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=build /workspace/target/*.jar /app/app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
