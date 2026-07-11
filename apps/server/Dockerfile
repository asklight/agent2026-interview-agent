FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS=""

RUN addgroup -S spring && adduser -S spring -G spring

COPY apps/server/target/layers/dependencies/ ./
COPY apps/server/target/layers/spring-boot-loader/ ./
COPY apps/server/target/layers/snapshot-dependencies/ ./
COPY apps/server/target/layers/application/ ./

USER spring:spring
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
