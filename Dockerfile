# Многослойная сборка с кэшированием зависимостей
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# 1. Копируем только POM файл (отдельный слой)
COPY pom.xml .

# 2. Скачиваем все зависимости (этот слой будет кэшироваться)
RUN mvn dependency:go-offline -B

# 3. Копируем исходный код
COPY src ./src

# 4. Собираем проект (используем уже скачанные зависимости)
RUN mvn clean package -DskipTests -B

# 5. Финальный образ
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]