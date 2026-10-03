FROM amazoncorretto:21-alpine-jdk
COPY target/stats-server.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "/app.jar"]