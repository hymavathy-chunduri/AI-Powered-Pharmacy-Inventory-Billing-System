# Multi-stage build for Spring Boot Backend
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
ENV MAVEN_OPTS="-Xmx384m -XX:+UseSerialGC"
COPY pom.xml .
RUN mvn dependency:go-offline -B || true
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-alpine AS runner
WORKDIR /app
COPY --from=builder /app/target/pharmacy-backend-0.0.1-SNAPSHOT.jar app.jar
ENV PORT=8080
ENV JAVA_OPTS="-Xmx384m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
