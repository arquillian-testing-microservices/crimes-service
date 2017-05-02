FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD build/libs/crimes-service-*.jar app.jar
RUN sh -c 'touch /app.jar'
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar /app.jar" ]