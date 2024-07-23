FROM amazoncorretto:17-alpine-jdk
MAINTAINER baeldung.com
COPY target/Recommend-Ai-Sync.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
