FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS=""

RUN addgroup -S spring && adduser -S spring -G spring

COPY apps/server/target/interview-agent-server-*.jar /app/app.jar

USER spring:spring
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
