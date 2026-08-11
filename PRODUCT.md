# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

El usuario principal es personal operativo o administrativo que procesa y supervisa lotes de transacciones financieras. Esta definición se infiere de los flujos existentes de operaciones, historial, dashboard y auditoría, y de la decisión confirmada de incorporar acceso empresarial.

## Product Purpose

iBatch permite cargar archivos CSV autorizados, procesar transacciones en lotes, revisar resultados y rechazos, y consultar trazabilidad operativa. El éxito significa que el usuario puede controlar el ciclo completo de procesamiento con estados claros y evidencia auditable.

## Positioning

El producto reúne carga controlada, validación, procesamiento batch y auditoría de transacciones en una sola experiencia operativa.

## Operating Context

La aplicación se utiliza desde un navegador web y trabaja con archivos `transactions_DDMMYYYY.csv`. El flujo principal comprende detectar o subir un archivo, procesarlo, consultar el avance y revisar resultados, rechazos y eventos de auditoría.

## Capabilities and Constraints

- Frontend en Next.js y backend en Spring Boot con Java 21.
- Persistencia en MySQL.
- Los archivos CSV usan los encabezados `cuenta,monto,fecha`.
- El proyecto se desplegará con opciones de bajo costo o gratuitas.
- El acceso público sin autenticación es una limitación actual que se resolverá con el nuevo flujo de login.
- La recuperación de contraseña y la administración de usuarios siguen sin decidirse.

## Brand Commitments

- Nombre del producto: iBatch.
- Lenguaje empresarial, serio, técnico y confiable.
- Conservar la identidad existente basada en azul tinta, teal y cobre, junto con su marca geométrica.
- La interfaz debe reforzar la calidad del proyecto como pieza pública de portafolio en GitHub.

## Evidence on Hand

- Interfaz funcional en `frontend/app`.
- Marca construida en `frontend/app/components/AppHeader.tsx`.
- Tokens visuales en `frontend/app/globals.css`.
- API y validaciones funcionales en `backend/src/main/java`.
- No hay testimonios, clientes, certificaciones ni métricas comerciales que puedan presentarse como reales.

## Product Principles

- La operación principal debe ser clara antes que decorativa.
- La seguridad se aplica en el backend y se comunica con estados comprensibles en el frontend.
- La trazabilidad y los errores deben poder comprenderse sin conocimiento técnico avanzado.
- Las nuevas superficies deben conservar la identidad visual y terminología existentes.
- No se presentan datos, clientes o garantías que no estén respaldados por el proyecto.

## Accessibility & Inclusion

Las superficies deben permitir navegación por teclado, foco visible, etiquetas explícitas, mensajes de error asociados y contraste mínimo WCAG AA.
