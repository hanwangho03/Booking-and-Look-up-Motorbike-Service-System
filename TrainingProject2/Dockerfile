# Giai đoạn 1: Xây dựng ứng dụng (Build stage)
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Sao chép file pom.xml và tải dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Sao chép mã nguồn và build với debug
COPY src ./src
RUN mvn clean package -DskipTests -e -X

# Giai đoạn 2: Tạo image runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Sao chép file JAR
COPY --from=build /app/target/TrainingProject2-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 6080

ENTRYPOINT ["java", "-jar", "app.jar"]