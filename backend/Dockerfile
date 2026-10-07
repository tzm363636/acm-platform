FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod 755 /app/docker-entrypoint.sh

# Cloud containers must enable the database/auth module, rather than expose health-only APIs.
ENV SPRING_PROFILES_ACTIVE=aiven,render
ENTRYPOINT ["/app/docker-entrypoint.sh"]

EXPOSE 10000

CMD ["java", "-jar", "app.jar"]
