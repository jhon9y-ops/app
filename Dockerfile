# ==============================================================================
# Estágio 1: Compilar o Fat JAR com consumo ultra controlado de memória
# ==============================================================================
FROM gradle:8.7-jdk17 AS build

# Limita agressivamente a memória máxima da JVM do Gradle para caber na nuvem
ENV GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx384m -Dorg.gradle.daemon=false -Dorg.gradle.workers.max=1 -Dorg.gradle.vfs.watch=false"

WORKDIR /home/gradle/src

# Copia os arquivos do projeto
COPY --chown=gradle:gradle . .

# Corrige quebras de linha invisíveis do Windows (CRLF para LF) no script gradlew
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Compila o Fat JAR limitando também o compilador do Kotlin via argumentos
RUN ./gradlew :server:buildFatJar -x test -Dorg.gradle.jvmargs="-Xmx384m"

# ==============================================================================
# Estágio 2: Imagem final leve para execução
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine
EXPOSE 8081
RUN mkdir /app

# Copia o arquivo Fat JAR gerado com sucesso no Estágio 1
COPY --from=build /home/gradle/src/server/build/libs/*all.jar /app/server.jar

ENTRYPOINT ["java", "-jar", "/app/server.jar"]
