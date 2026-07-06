# OnControlBackend - Render Deploy

Backend principal Spring Boot para la demo OnControl.

## Stack detectado

- Java 21
- Spring Boot 3.5.6
- Maven Wrapper (`mvnw`)
- MySQL
- Dockerfile disponible

## Opcion recomendada en Render

Usar Web Service con entorno Docker o Java.

### Render con Dockerfile

Render detecta el `Dockerfile`.

- Build command: no requiere comando manual si usas Docker.
- Start command: no requiere comando manual; el `Dockerfile` usa `java -jar app.jar`.
- Health check path: `/health`

### Render nativo Java

Build command:

```sh
./mvnw clean package -DskipTests
```

Start command:

```sh
java -jar target/oncontrol-backend-0.0.1-SNAPSHOT.jar
```

## Variables de entorno

```env
PORT=8080
MYSQL_HOST=TU-MYSQL-HOST
MYSQL_PORT=3306
MYSQL_DATABASE=oncontrol
MYSQL_USER=avnadmin
MYSQL_PASSWORD=TU_PASSWORD_DE_AIVEN
MYSQL_SSL_MODE=REQUIRED
JWT_SECRET=CAMBIA_ESTE_SECRETO_DEMO_MIN_256_BITS
CORS_ORIGINS=http://localhost:3000,https://TU-FRONTEND.vercel.app
```

Notas:

- `server.port=${PORT:8080}` permite que Render asigne el puerto.
- Para Aiven MySQL, usa el usuario que te da Aiven, normalmente `avnadmin`; no uses `root`.
- `MYSQL_SSL_MODE=REQUIRED` mantiene SSL obligatorio para Aiven. Para una base local sin SSL puedes usar `MYSQL_SSL_MODE=DISABLED`.
- `CORS_ORIGINS` acepta origenes separados por coma.
- No hardcodees `JWT_SECRET` ni passwords reales en el repo.

## Base de datos

Crear una base MySQL accesible desde Render. Para Aiven usa host, puerto, database, usuario y password provistos por Aiven. La app usa:

```properties
spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:oncontrol}?sslMode=${MYSQL_SSL_MODE:REQUIRED}&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=${MYSQL_USER:root}
spring.datasource.password=${MYSQL_PASSWORD:}
```

## Endpoints de verificacion

- `GET /health`
- `GET /`
- Swagger: `/swagger-ui.html`
- OpenAPI JSON: `/api-docs`

## Credenciales demo del seed

El `DataSeeder` se ejecuta cuando la base esta vacia.

Credenciales del grupo:

```text
admin.grupo2@oncontrol.com / OnControl2026!
doctor.grupo2@oncontrol.com / OnControl2026!
paciente.grupo2@oncontrol.com / OnControl2026!
```

Credenciales base existentes:

```text
admin@hospital.com / password123
dr.garcia@hospital.com / password123
dr.rodriguez@hospital.com / password123
juan.perez@email.com / password123
```

## Probar login

```powershell
curl -X POST https://TU-BACKEND.onrender.com/api/auth/login `
  -H "Content-Type: application/json" `
  -d "{\"email\":\"paciente.grupo2@oncontrol.com\",\"password\":\"OnControl2026!\"}"
```
