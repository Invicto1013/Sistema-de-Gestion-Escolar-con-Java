# ---------- Etapa 1: compilar ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
# Compila y copia las dependencias del servidor web (JavaFX no hace falta en la web).
RUN mvn -B -DskipTests compile dependency:copy-dependencies \
      -DexcludeGroupIds=org.openjfx -DoutputDirectory=target/lib \
    # Nunca dejar credenciales dentro de la imagen.
    && rm -f target/classes/db.properties

# ---------- Etapa 2: ejecutar (imagen ligera, apta para 512 MB de RAM) ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/classes ./classes
COPY --from=build /app/target/lib ./lib

ENV SIGE_WEB_HOST=0.0.0.0 \
    SIGE_WEB_SECURE_COOKIE=true
EXPOSE 10000

# La plataforma define PORT; WebApp lo lee automaticamente.
CMD ["java", "-Xmx300m", "-XX:+UseSerialGC", "-cp", "classes:lib/*", "com.unev.sige.web.WebApp"]
