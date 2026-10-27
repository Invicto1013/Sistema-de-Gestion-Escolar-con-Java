# ---------- Etapa 1: compilar con Maven ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# No se copia db.properties ni ninguna credencial a la imagen.
COPY pom.xml ./
COPY src ./src

# Genera target/sige-1.0.0.jar (JAR unico con todas las dependencias). No ejecuta las pruebas.
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

# ---------- Etapa 2: ejecutar solo con Java (sin Maven) ----------
# Imagen final pequena: arranca en segundos y usa menos memoria que "mvn javafx:run".
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --home-dir /app --shell /usr/sbin/nologin sige \
    && chown sige:sige /app
COPY --from=build --chown=sige:sige /app/target/sige-1.0.0.jar ./sige.jar
USER sige

ENV SIGE_WEB_HOST=0.0.0.0
# Pensado para el plan gratis de Render (512 MB): recolector sencillo y arranque rapido.
ENV JAVA_TOOL_OPTIONS="-XX:+UseSerialGC -XX:MaxRAMPercentage=70 -XX:TieredStopAtLevel=1"
EXPOSE 10000

# La plataforma define PORT (Render usa 10000); WebApp lo lee. En Docker local usa 8080 por defecto.
CMD ["java", "-cp", "sige.jar", "com.unev.sige.web.WebApp"]
