# image qui contient Maven et un JDK
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiez les fichiers de build
COPY pom.xml .
COPY checkstyle.xml .

# Copiez le code source
COPY src ./src

# Lancez le build Maven pour créer le .jar
RUN mvn clean package -DskipTests

# image JRE légère (sans Maven)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiez uniquement le .jar construit depuis l'étape "build"
COPY --from=build /app/target/app.jar .

# Exposez le port 8080
EXPOSE 8080

# Lancer l'application au démarrage du conteneur
ENTRYPOINT ["java", "-jar", "app.jar"]