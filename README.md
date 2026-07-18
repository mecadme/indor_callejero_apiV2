# Indor Callejero API

Reconstrucción del backend de gestión del torneo Indor Callejero, hecha de forma
incremental siguiendo las recomendaciones de `AUDITORIA_TECNICA.md` del proyecto
original. Ver la hoja de ruta completa (12 etapas) para el contexto de por qué
cada pieza se construye en este orden.

- Java 21 (Temurin)
- Spring Boot 4.1.0 / Maven
- MySQL 8.4 (vía Docker, solo para desarrollo local)

## Arrancar en desarrollo

1. Copiar `.env.example` a `.env` y completar `DB_PASSWORD` (cualquier valor local sirve).
2. Levantar la base de datos: `docker compose up -d`
3. Correr la app apuntando al perfil de desarrollo:

   ```
   export $(cat .env | xargs) && SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
   ```

   (En PowerShell: cargar las variables de `.env` a la sesión y luego
   `$env:SPRING_PROFILES_ACTIVE="dev"; ./mvnw spring-boot:run`)

La app no tiene un perfil activo por defecto — es intencional. Correrla sin
`SPRING_PROFILES_ACTIVE` explícito falla en vez de arrancar con una
configuración adivinada.

## Por qué no hay perfil por defecto

El proyecto original construía la imagen Docker con `-Pdev`, lo que dejaba el
perfil `dev` horneado como default del jar incluso en producción. Acá esa
ambigüedad no existe: sin perfil explícito, no arranca.
