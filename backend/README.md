# iBatch Backend

Backend Spring Boot para exponer la API que consumira el frontend.

## Requisitos

- Java 21
- Maven 3.9+
- MySQL local

## Variables de entorno

Valores por defecto para desarrollo local:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/ibatch?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="root"
$env:DB_PASSWORD=""
$env:CORS_ALLOWED_ORIGINS="http://localhost:3000"
```

## Directorio de archivos

El backend lee los CSV desde el directorio configurado en `application.yml`:

```yaml
app:
  files:
    input-dir: input
```

Cuando el backend se ejecuta desde la carpeta `backend`, esa ruta apunta a `backend/input`.
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
- `POST /files/process`: valida y registra el archivo seleccionado con estado `PROCESANDO`.

Ejemplo para validar un archivo:

```json
{
  "fileName": "transactions_31072026.csv"
}
```
