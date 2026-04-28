# Этап 1: Сборка (используем Maven и Java 17)
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
# Копируем pom.xml и исходники
COPY pom.xml .
COPY src ./src
# Собираем проект и пропускаем тесты для скорости
RUN mvn clean package -DskipTests

# Этап 2: Запуск (используем только JRE для легкости)
FROM amazoncorretto:17-alpine
WORKDIR /app
# Копируем готовый jar-файл из первого этапа
COPY --from=build /app/target/*.jar app.jar
# Порт, который использует Spring Boot
EXPOSE 8080
# Команда для запуска
ENTRYPOINT ["java", "-jar", "app.jar"]