# Usa una imagen base de Maven
# Etapa 1: Construcción del JAR
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Copiar el archivo pom.xml y resolver las dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y compilar el proyecto
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Configuración de Python y del contenedor final
FROM openjdk:17-jdk-slim

# Instalar Python y las dependencias necesarias
RUN apt-get update && apt-get install -y python3 python3-pip libreoffice && apt-get clean

# Establecer el directorio de trabajo
WORKDIR /app

# Copiar el JAR desde la etapa de construcción
COPY --from=build /app/target/*.jar app.jar

# Copiar el script de Python y el archivo requirements.txt
COPY src/main/resources/conversorLibreOffice.py /app/conversorLibreOffice.py
COPY requirements.txt ./requirements.txt

# Instalar las dependencias de Python
RUN pip3 install --no-cache-dir -r requirements.txt

# Exponer el puerto de la aplicación Java
EXPOSE 8080

# Iniciar la aplicación Java
ENTRYPOINT ["java", "-jar", "app.jar"]