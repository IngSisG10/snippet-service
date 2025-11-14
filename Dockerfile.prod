FROM gradle:8.7-jdk21-alpine AS builder
WORKDIR /app
COPY . .
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew clean bootJar -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S kotlin && adduser -S kotlin -G kotlin
USER kotlin
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]