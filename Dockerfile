# Étape 1 : Build de l'application
FROM gradle:8.5-jdk21 AS build
WORKDIR /app

# Copier les fichiers Gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Copier le code source
COPY src ./src

# Build l'application (créer le JAR)
RUN gradle bootJar --no-daemon

# Étape 2 : Image finale légère
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copier le JAR depuis l'étape de build
COPY --from=build /app/build/libs/*.jar app.jar

# Exposer le port
EXPOSE 8090

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]