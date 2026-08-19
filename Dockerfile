# ==============================================================================
# Estágio 1: Compilar o Fat JAR isolando o módulo Server
# ==============================================================================
FROM gradle:8.7-jdk17 AS build

# Define variáveis para o Gradle rodar de forma leve na nuvem
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.workers.max=2"

# Define o diretório de trabalho no container
WORKDIR /home/gradle/src

# Copia todo o código do repositório com as permissões corretas do usuário gradle
COPY --chown=gradle:gradle . .

# Corrige quebras de linha invisíveis do Windows (CRLF para LF) no script gradlew
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Compila o Fat JAR focando EXCLUSIVAMENTE na pasta do servidor (-p server)
# Isso ignora os módulos Android/JS e evita erros de falta de SDK na hospedagem
RUN ./gradlew :server:buildFatJar -x test -p server

# ==============================================================================
# Estágio 2: Imagem final leve para execução (Apenas o que vai rodar na nuvem)
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine

# Expõe a porta de rede que sua API do Ktor utiliza
EXPOSE 8081

# Cria o diretório interno da aplicação
RUN mkdir /app

# Copia o arquivo Fat JAR gerado com sucesso no Estágio 1
COPY --from=build /home/gradle/src/server/build/libs/*all.jar /app/server.jar

# Comando que inicializa o servidor Java/Kotlin
ENTRYPOINT ["java", "-jar", "/app/server.jar"]
