# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY common-core/pom.xml ./common-core/pom.xml
COPY common-security/pom.xml ./common-security/pom.xml
COPY common-mybatis/pom.xml ./common-mybatis/pom.xml
COPY publish-service/pom.xml ./publish-service/pom.xml
RUN mvn dependency:go-offline -pl publish-service -am
COPY common-core/src ./common-core/src
COPY common-security/src ./common-security/src
COPY common-mybatis/src ./common-mybatis/src
COPY publish-service/src ./publish-service/src
RUN mvn clean package -pl publish-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /app/publish-service/target/*.jar app.jar
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8085
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
