# Estágio 1: Compilar o Fat JAR usando o Gradle oficial
FROM gradle:8.7-jdk17 AS build
WORKDIR /home/gradle/src

# Copia todos os arquivos do projeto para dentro do container
COPY --chown=gradle:gradle . .

# Corrige quebras de linha invisíveis do Windows (CRLF para LF) no script gradlew
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Compila o Fat JAR do Ktor
RUN ./gradlew :server:buildFatJar --no-daemon -x test

# Estágio 2: Imagem final leve para execução
FROM eclipse-temurin:17-jre-alpine
EXPOSE 8081
RUN mkdir /app

# Copia especificamente o arquivo Fat JAR gerado
COPY --from=build /home/gradle/src/server/build/libs/*all.jar /app/server.jar

ENTRYPOINT ["java", "-jar", "/app/server.jar"]
