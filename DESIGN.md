---
name: iBatch
description: Sistema visual empresarial para operaciones financieras por lotes con control y trazabilidad.
colors:
  ink-deep: "#102a3a"
  ink-strong: "#17394b"
  ink-body: "#39596a"
  ink-muted: "#6f8793"
  border-strong: "#b8c6cc"
  border-soft: "#dce4e7"
  surface-soft: "#f7f9f9"
  teal-strong: "#236b68"
  teal-operational: "#2c7d78"
  teal-soft: "#dcecea"
  copper-signal: "#a86739"
  white: "#ffffff"
typography:
  display:
    fontFamily: "Segoe UI, Helvetica Neue, Arial, sans-serif"
    fontSize: "clamp(2.6875rem, 4.5vw, 4.25rem)"
    fontWeight: 620
    lineHeight: 0.98
    letterSpacing: "-0.04em"
  headline:
    fontFamily: "Segoe UI, Helvetica Neue, Arial, sans-serif"
    fontSize: "clamp(2.125rem, 3.5vw, 2.875rem)"
    fontWeight: 620
    lineHeight: 1.05
    letterSpacing: "-0.04em"
  body:
    fontFamily: "Segoe UI, Helvetica Neue, Arial, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
    lineHeight: 1.6
  label:
    fontFamily: "Segoe UI, Helvetica Neue, Arial, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 750
    lineHeight: 1.2
rounded:
  none: "0"
  precise: "1px"
spacing:
  xs: "6px"
  sm: "12px"
  md: "18px"
  lg: "24px"
  xl: "38px"
components:
  button-primary:
    backgroundColor: "{colors.ink-deep}"
    textColor: "{colors.white}"
    rounded: "{rounded.precise}"
    padding: "0 22px"
    height: "52px"
  button-primary-hover:
    backgroundColor: "{colors.teal-strong}"
    textColor: "{colors.white}"
    rounded: "{rounded.precise}"
  button-secondary:
    backgroundColor: "{colors.white}"
    textColor: "{colors.ink-strong}"
    rounded: "{rounded.precise}"
    padding: "0 20px"
    height: "42px"
  input:
    backgroundColor: "{colors.white}"
    textColor: "{colors.ink-deep}"
    rounded: "{rounded.precise}"
    padding: "0 15px"
    height: "52px"
---

# Design System: iBatch

## Overview

**Creative North Star: "Umbral operativo"**

iBatch debe sentirse como la entrada a una herramienta financiera controlada: seria, técnica, precisa y confiable. La interfaz combina superficies claras, tinta azul marino, señales teal y pequeños acentos cobre. La identidad se apoya en jerarquía, ritmo y geometría funcional, no en ornamento.

La densidad es contenida y el movimiento es mínimo. Cada pantalla debe ayudar a comprender el estado del lote, la siguiente acción y la evidencia disponible sin competir por atención.

**Key Characteristics:**

- Jerarquía tipográfica contundente y directa.
- Superficies planas separadas por bordes finos.
- Geometría ortogonal con esquinas casi rectas.
- Teal para operación y cobre como señal escasa.
- Estados claros, lenguaje sobrio y accesibilidad verificable.

## Colors

La paleta combina tinta financiera, teal operativo, cobre de señal y neutros fríos.

### Primary

- **Tinta profunda:** estructura, títulos, texto de máxima jerarquía y acciones primarias.
- **Teal operativo:** estados activos, foco, progreso y respuesta interactiva.

### Secondary

- **Cobre de señal:** acento infrecuente para reforzar identidad o advertir sobre un punto específico.

### Neutral

- **Blanco de trabajo:** superficie principal de formularios y contenido.
- **Superficie fría:** agrupación secundaria y contexto operativo.
- **Bordes suaves y firmes:** separación estructural sin sombras innecesarias.
- **Tinta de cuerpo:** texto secundario con contraste suficiente.

**The Rare Copper Rule.** El cobre no compite con acciones, estados ni alertas; su escasez le da valor.

**The Operational Teal Rule.** El teal comunica actividad, selección o foco. No se usa como decoración extensa.

## Typography

**Display Font:** Segoe UI con Helvetica Neue y Arial como respaldo
**Body Font:** Segoe UI con Helvetica Neue y Arial como respaldo
**Label/Mono Font:** Segoe UI para etiquetas; Consolas o SFMono-Regular sólo para nombres técnicos y archivos

**Character:** Una sans serif neutral y nítida evita gestos editoriales ajenos al producto. La personalidad aparece mediante escala, peso y espaciado preciso.

### Hierarchy

- **Display:** peso 620, escala fluida, interlineado compacto; reservado para mensajes de máxima jerarquía.
- **Headline:** peso 620 y tracking negativo; títulos de pantalla y módulos principales.
- **Body:** 14 a 16 px, interlineado amplio; instrucciones y explicación operativa.
- **Label:** 10 a 12 px, peso alto; campos, metadatos y categorías.

**The One Strong Statement Rule.** Cada superficie tiene un único mensaje tipográfico dominante.

## Layout

Las vistas operativas usan contenedores amplios y una retícula de espaciado basada en 6 px. Las divisiones se expresan con bordes y cambios de superficie. El login demuestra una composición asimétrica de dos áreas, pero esa topología no es obligatoria para otras pantallas.

En anchos menores a 820 px, las columnas se apilan, los espacios se reducen y la firma operativa se conserva de forma compacta. Entre 820 y 1100 px se comprime la escala sin ocultar acciones esenciales. Ningún contenido debe provocar desplazamiento horizontal a 390 px.

## Elevation & Depth

El sistema es plano por defecto. La profundidad se expresa con contraste tonal, bordes de 1 px y superposición modal. La sombra suave existente se reserva para capas temporales o elementos que realmente flotan.

**The Flat-by-Default Rule.** Una superficie permanente no recibe sombra si un borde o cambio tonal puede explicar su jerarquía.

## Shapes

La geometría es ortogonal y precisa. Los controles usan radio de 1 px; búsquedas y tablas pueden usar esquinas rectas. Los círculos se limitan a indicadores de estado o controles cuya semántica lo justifique.

## Components

### Buttons

- **Shape:** rectangular y precisa, radio de 1 px.
- **Primary:** tinta profunda con texto blanco; teal fuerte en hover.
- **Secondary:** blanco, borde frío y texto de tinta fuerte.
- **Focus:** anillo teal visible y separado del control.
- **Disabled:** conserva su estructura y reduce opacidad; el cursor comunica indisponibilidad.

### Cards / Containers

- **Corner Style:** recto o radio de 1 px.
- **Background:** blanco o superficie fría.
- **Shadow Strategy:** sin sombra en reposo.
- **Border:** línea fría de 1 px como separador principal.
- **Internal Padding:** entre 18 y 26 px según densidad.

### Inputs / Fields

- **Style:** fondo blanco, borde firme de 1 px, altura cómoda y etiqueta siempre visible.
- **Focus:** borde teal y anillo tonal suave.
- **Placeholder:** tinta de cuerpo con contraste legible; nunca sustituye a la etiqueta.

### Navigation

La navegación usa texto sobrio sobre fondo blanco. El estado activo combina tinta profunda y una línea teal inferior; los estados no disponibles bajan opacidad sin parecer seleccionados.

### Operational Signature

Las categorías Carga, Proceso y Auditoría pueden funcionar como secuencia conceptual cuando la pantalla necesita explicar el ciclo de trabajo. No deben aparecer por rutina en superficies donde no aportan contexto.

## Do's and Don'ts

### Do:

- **Do** usar jerarquía, alineación y contraste antes que sombras.
- **Do** reservar el teal para actividad, selección y foco.
- **Do** mantener etiquetas visibles, foco perceptible y contraste WCAG AA.
- **Do** conservar la firma navy, teal y cobre en expresiones de marca.
- **Do** reducir información en móvil sin borrar la identidad operativa.

### Don't:

- **Don't** introducir gradientes multicolor, vidrio, resplandores o radios grandes.
- **Don't** convertir cada sección en una tarjeta flotante.
- **Don't** usar cobre como color primario de interacción.
- **Don't** añadir animación ornamental; el movimiento debe explicar estado o respuesta.
- **Don't** inventar métricas, clientes o sellos de confianza para llenar espacio.
