FROM maven:3.9.9-eclipse-temurin-21

WORKDIR /app

# No copies db.properties or credentials into the image.
COPY pom.xml ./
COPY src ./src

# Compile at build time. Tests are not run while building the container.
RUN mvn -Pweb -DskipTests package

ENV SIGE_WEB_HOST=0.0.0.0
EXPOSE 10000

# The platform-provided PORT is read by WebApp; local Docker defaults to 8080.
CMD ["mvn", "-Pweb", "-DskipTests", "javafx:run"]
