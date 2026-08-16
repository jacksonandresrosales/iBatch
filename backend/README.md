# iBatch Backend

Backend Spring Boot para exponer la API que consumirá el frontend.

## Requisitos

- Java 21
- Maven 3.9+
- PostgreSQL local o Supabase
- JDK 21 activo en `JAVA_HOME`

## Variables de entorno

Valores por defecto para desarrollo local:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/ibatch"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="tu-contrasena"
$env:DB_DRIVER="org.postgresql.Driver"
$env:CORS_ALLOWED_ORIGINS="http://localhost:3000"
$env:IBATCH_ADMIN_USERNAME="admin"
$env:IBATCH_ADMIN_PASSWORD="una-contrasena-inicial-segura"
$env:IBATCH_OPERATOR_USERNAME="operator"
$env:IBATCH_OPERATOR_PASSWORD="otra-contrasena-inicial-segura"
$env:SESSION_COOKIE_SECURE="false"
$env:SESSION_COOKIE_SAME_SITE="lax"
$env:APP_FILES_INPUT_DIR="C:\iroute\input"
$env:MAX_FILE_SIZE_BYTES="52428800"
$env:MAX_FILE_RECORDS="1000000"
$env:PROCESSING_BATCH_SIZE="500"
```

## Directorio de archivos

El backend lee los CSV desde `APP_FILES_INPUT_DIR`. Si no se define, usa `input` en `application.yml`:

```yaml
app:
  files:
    input-dir: ${APP_FILES_INPUT_DIR:input}
```

Cuando el backend se ejecuta desde la carpeta `backend` y no se define la variable, esa ruta apunta a `backend/input`.
La carpeta `backend/input` es local de pruebas y no se sube al repositorio.

## Base de datos

Ejecuta los scripts de la carpeta `database/postgresql` en orden:

```text
001_create_batch_processing_model.sql
002_add_authentication.sql
```

## Ejecutar

```powershell
cd backend
mvn spring-boot:run
```

## Ejecutar con Docker

Desde la carpeta `backend`:

```powershell
docker build -t ibatch-backend:local .
docker run --rm -p 8080:8080 --env-file .env.local ibatch-backend:local
```

Para Render, configure `backend` como directorio raíz, `Dockerfile` como archivo Docker y `/api/health` como health check. Las variables de PostgreSQL, Supabase, CORS y las contraseñas iniciales deben configurarse como secretos desde el panel del servicio.

## Endpoints iniciales

- `GET /api/health`: valida que el backend esta levantado.
- `GET /auth/csrf`: crea la sesion y entrega el token CSRF.
- `POST /auth/login`: inicia sesion con usuario y contrasena.
- `POST /auth/logout`: cierra la sesion autenticada.
- `GET /auth/me`: devuelve el usuario autenticado.
- `GET /api/health/database`: valida PostgreSQL; requiere rol `ADMIN`.
- `GET /files/available`: lista los CSV disponibles en el directorio configurado.
- `GET /files`: lista los archivos procesados registrados en PostgreSQL.
- `GET /files/{id}`: devuelve el detalle del archivo y sus transacciones.
- `GET /files/{id}/progress`: consulta el avance del procesamiento.
- `POST /files/process`: valida, registra y procesa el archivo seleccionado de forma asíncrona.
- `POST /transactions/{id}`: edita el monto y reprocesa una transaccion rechazada.
- `GET /dashboard/summary`: devuelve los indicadores operativos.
- `GET /dashboard/logs`: devuelve los eventos de auditoría paginados.

El procesamiento utiliza validadores independientes, escritura por lotes y un límite
de cinco solicitudes de procesamiento por minuto por dirección IP. El reproceso
permite hasta veinte solicitudes por minuto por dirección IP.

Todos los endpoints excepto `GET /api/health`, `GET /auth/csrf` y `POST /auth/login`
requieren una sesion. Los roles `ADMIN` y `OPERATOR` pueden consultar y ejecutar las
operaciones de archivos, transacciones, dashboard y auditoria. La comprobacion interna
`GET /api/health/database` es exclusiva de `ADMIN`. Las solicitudes que modifican datos deben incluir
el encabezado indicado por `GET /auth/csrf`. En produccion configure
`SESSION_COOKIE_SECURE=true` y proporcione la contrasena inicial como secreto del entorno.

## Pruebas

```powershell
mvn test
```

El proyecto requiere JDK 21. Si se ejecuta con una versión de Java no soportada por
Byte Buddy, las pruebas que usan Mockito pueden fallar aunque el código compile.

Ejemplo para validar un archivo:

```json
{
  "fileName": "transactions_31072026.csv"
}
```

Respuesta exitosa:

```json
{
  "fileId": 1,
  "fileName": "transactions_31072026.csv",
  "status": "PROCESADO_CON_RECHAZOS",
  "message": "Archivo procesado correctamente",
  "totalRecords": 10,
  "processedCount": 8,
  "rejectedCount": 2
}
```
