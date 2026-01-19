# Etapa 1: Construcción (Usamos Maven con Java 21 oficial de Eclipse)
FROM maven:3.9.6-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución (Usamos la imagen ligera de Java 21)
FROM eclipse-temurin:21-jdk-alpine
COPY --from=build /target/*.jar app.jar

# Tu puerto personalizado
EXPOSE 2323
ENTRYPOINT ["java", "-Dserver.port=2323", "-jar", "app.jar"]