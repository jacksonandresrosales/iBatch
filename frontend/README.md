# iBatch Frontend

Aplicación Next.js para la detección, selección y procesamiento controlado de
archivos CSV de transacciones financieras.

## Requisitos

- Node.js 22 o superior.
- npm 11 o superior.

## Ejecución local

```bash
npm install
npm run dev
```

La aplicación estará disponible en `http://localhost:3000` y redirigirá a la
pantalla `/files/available`.

## Compilación

```bash
npm run build
npm run start
```

Maven no forma parte del frontend. Se utilizará exclusivamente para gestionar
el backend Spring Boot.


La aplicación consume el backend mediante `NEXT_PUBLIC_API_BASE_URL`.

Crear un archivo `.env.local` si el backend no está en la dirección predeterminada:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```
