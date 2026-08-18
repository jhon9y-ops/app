# Estágio 1: Compilar o servidor usando o Gradle oficial
FROM gradle:8.7-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

# Concede permissao de execucao ao script do gradlew no Linux
RUN chmod +x gradlew
# Compila o Fat JAR do servidor Ktor
RUN ./gradlew :server:buildFatJar --no-daemon

# Estágio 2: Criar a imagem final leve para rodar na nuvem de graça
FROM eclipse-temurin:17-jre-alpine
EXPOSE 8081
RUN mkdir /app
COPY --from=build /home/gradle/src/server/build/libs/*all.jar /app/server.jar
ENTRYPOINT ["java", "-jar", "/app/server.jar"]
