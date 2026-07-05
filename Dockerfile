FROM dh-mirror.gitverse.ru/eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY build/libs/smart-subscriptions-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]