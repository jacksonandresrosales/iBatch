# iBatch Financial Operations

iBatch es una aplicación para procesar, validar y monitorear lotes transaccionales en archivos CSV. Incluye seguimiento del progreso, historial de archivos, reprocesamiento de transacciones rechazadas, indicadores operativos y auditoría persistida.

## Arquitectura

El proyecto se organiza como una aplicación web con dos componentes:

- `frontend`: Next.js y React con actualización periódica de progreso, dashboard, historial y auditoría.
- `backend`: Spring Boot con procesamiento asíncrono, validaciones por estrategia, escritura por lotes y API REST.
- `database`: scripts SQL para crear el esquema de MySQL y sus índices.

## Requisitos

- Java 21
- Maven 3.9 o superior
- Node.js 20 o superior
- MySQL 8

## Configuración local

1. Ejecute `database/001_create_database.sql` y después los demás scripts SQL en orden numérico.
2. Configure las variables `DB_USERNAME` y `DB_PASSWORD`. Opcionalmente puede definir `DB_URL`.
3. Inicie el backend desde `backend` con `mvn spring-boot:run`.
4. Instale las dependencias del frontend con `npm install` dentro de `frontend`.
5. Inicie el frontend con `npm run dev` y abra `http://localhost:3000`.

El backend escucha en `http://localhost:8080` de forma predeterminada. El frontend puede usar otra dirección mediante `NEXT_PUBLIC_API_BASE_URL`.

## Funciones principales

- Detección y procesamiento asíncrono de archivos CSV.
- Validación de cuenta, monto, fecha y duplicados.
- Persistencia eficiente mediante operaciones por lotes.
- Progreso de procesamiento en tiempo real.
- Historial paginado y detalle de transacciones.
- Reprocesamiento de registros rechazados.
- Dashboard con métricas obtenidas de la base de datos.
- Auditoría paginada y exportable.

Consulte [backend/README.md](backend/README.md) y [frontend/README.md](frontend/README.md) para más detalles de cada componente.
