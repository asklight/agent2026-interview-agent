FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY apps/server/pom.xml apps/server/pom.xml

WORKDIR /workspace/apps/server
RUN mvn -B -ntp dependency:go-offline

WORKDIR /workspace
COPY apps/server/src apps/server/src

WORKDIR /workspace/apps/server
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS=""

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /workspace/apps/server/target/*.jar /app/app.jar

USER spring:spring
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
