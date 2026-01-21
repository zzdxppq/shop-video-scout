# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy parent pom and common modules first for better caching
COPY pom.xml ./pom.xml
COPY common-core/pom.xml ./common-core/pom.xml
COPY common-security/pom.xml ./common-security/pom.xml
COPY common-mybatis/pom.xml ./common-mybatis/pom.xml
COPY user-service/pom.xml ./user-service/pom.xml

# Download dependencies
RUN mvn dependency:go-offline -pl user-service -am

# Copy source code
COPY common-core/src ./common-core/src
COPY common-security/src ./common-security/src
COPY common-mybatis/src ./common-mybatis/src
COPY user-service/src ./user-service/src

# Build the application
RUN mvn clean package -pl user-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy jar from build stage
COPY --from=build /app/user-service/target/*.jar app.jar

# JVM options for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
