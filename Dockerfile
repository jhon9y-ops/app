# Estágio 1: Compilar o Fat JAR usando o Gradle oficial
FROM gradle:8.7-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

# Dá permissão ao script
RUN chmod +x gradlew

# Compila o Fat JAR do servidor Ktor sem rodar os testes para economizar memória
# Compila limitando o consumo máximo de memória da JVM
RUN ./gradlew :server:buildFatJar --no-daemon -x test -Dorg.gradle.jvmargs="-Xmx384m -XX:MaxMetaspaceSize=128m"


# Estágio 2: Imagem final leve para execução
FROM eclipse-temurin:17-jre-alpine
EXPOSE 8081
RUN mkdir /app

# Copia especificamente o arquivo Fat JAR gerado
COPY --from=build /home/gradle/src/server/build/libs/*all.jar /app/server.jar

ENTRYPOINT ["java", "-jar", "/app/server.jar"]