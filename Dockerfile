# Estágio 1: Compilar usando Gradle oficial
FROM gradle:8.7-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

# Dá permissão e compila o módulo server
RUN chmod +x gradlew
RUN ./gradlew :server:build --no-daemon -x test

# Estágio 2: Imagem final de execução
FROM eclipse-temurin:17-jre-alpine
EXPOSE 8081
RUN mkdir /app
COPY --from=build /home/gradle/src/server/build/libs/*.jar /app/server.jar
ENTRYPOINT ["java", "-jar", "/app/server.jar"]