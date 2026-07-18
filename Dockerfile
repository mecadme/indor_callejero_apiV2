# Etapa 11: para levantar 2+ instancias detrás de un balanceador hace
# falta empaquetar la app como imagen -- hasta acá siempre corrió con
# `mvnw spring-boot:run` en la máquina del dev, nunca containerizada.
#
# Build con Maven (no `./mvnw`): el wrapper jar (.mvn/wrapper/maven-wrapper.jar)
# está en .gitignore a propósito (no se versiona un binario), así que
# dentro del build de Docker no existe -- usar la imagen oficial de Maven
# evita depender de que mvnw lo descargue en build time.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copiar solo el pom primero: si el pom no cambió, Docker reusa la capa
# con las dependencias ya bajadas y no vuelve a pegarle a Maven Central
# en cada build, aunque el código sí haya cambiado.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src/ src/
RUN mvn -B -DskipTests package

# Imagen final: solo JRE (no el JDK ni Maven completo) + el jar ya armado.
# eclipse-temurin:21-jre-alpine, misma familia que usa el proyecto en dev
# (Etapa 0), para no meter una tercera distribución de Java a la mezcla.
FROM eclipse-temurin:21-jre-alpine

# No correr como root dentro del contenedor -- si algún día un endpoint
# tiene una vulnerabilidad de path traversal o RCE, un usuario sin
# privilegios limita el daño real, no solo en la app sino en lo que ese
# proceso puede tocar del filesystem del contenedor.
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /build/target/indor-callejero-api-*.jar app.jar

# Probado en vivo: un volumen nombrado nuevo (uploads_data en
# docker-compose) se monta root:root por defecto -- el usuario "app"
# sin privilegios no podía crear /app/uploads/teams, StorageService
# tiraba AccessDeniedException al primer upload. Pre-crear el directorio
# acá y darle el owner correcto ANTES de montar el volumen funciona
# porque Docker, al inicializar un volumen nombrado vacío, copia el
# contenido (y los permisos) que ya existían en esa ruta dentro de la
# imagen.
RUN mkdir -p uploads && chown -R app:app app.jar uploads
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
