# iBatch Backend

Backend Spring Boot para exponer la API que consumirá el frontend.

## Requisitos

- Java 21
- Maven 3.9+
- MySQL local
- JDK 21 activo en `JAVA_HOME`

## Variables de entorno

Valores por defecto para desarrollo local:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/ibatch?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="root"
$env:DB_PASSWORD=""
$env:CORS_ALLOWED_ORIGINS="http://localhost:3000"
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

Ejecuta los scripts de la carpeta `database` en orden:

```text
001_create_database.sql
002_create_batch_processing_model.sql
```

## Ejecutar

```powershell
cd backend
mvn spring-boot:run
```

## Endpoints iniciales

- `GET /api/health`: valida que el backend esta levantado.
- `GET /api/health/database`: valida la conexion con MySQL.
- `GET /files/available`: lista los CSV disponibles en el directorio configurado.
- `GET /files`: lista los archivos procesados registrados en MySQL.
- `GET /files/{id}`: devuelve el detalle del archivo y sus transacciones.
- `GET /files/{id}/progress`: consulta el avance del procesamiento.
- `POST /files/process`: valida, registra y procesa el archivo seleccionado de forma asíncrona.
- `POST /transactions/{id}`: edita el monto y reprocesa una transaccion rechazada.
- `GET /dashboard/summary`: devuelve los indicadores operativos.
- `GET /dashboard/logs`: devuelve los eventos de auditoría paginados.

El procesamiento utiliza validadores independientes, escritura por lotes y un límite
de cinco solicitudes de procesamiento por minuto por dirección IP. El reproceso
permite hasta veinte solicitudes por minuto por dirección IP.

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
