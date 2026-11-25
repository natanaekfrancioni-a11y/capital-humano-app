# Capit@l Humano — Sistema de Gestión de Evaluaciones de Perfiles

Trabajo Práctico Integrador de **Diseño de Sistemas de Información** y **Desarrollo de Software** — **UTN FRSF (2026)**.

## Alcance y Funcionalidades Implementadas

* **Catálogo y Estructura:** Gestión de candidatos, puestos/funciones, competencias, factores y preguntas con opciones ponderadas.
* **Evaluación y Cuestionario:** Generación de evaluaciones con clave de acceso, inicio/sorteo por bloques, avance bloque a bloque y cálculo de puntaje total al completar.
* **Reportes:** Emisión de **Orden de Méritos** (filtrando candidatos completos que cumplen mínimos requeridos) y **Reporte Comparativo** de puntajes y condiciones.

## Estructura del Repositorio

* `/api`: Backend en Java con Spring Boot (API REST, lógica de negocio y persistencia).
* `/frontend`: Aplicación web en Next.js.
* `/bd`: Scripts SQL de estructura (`schema.sql`) y datos iniciales (`seed.sql`).
* `/docs`: Documentación y diagramas UML (Clases, Entidad-Relación, Secuencia, Despliegue).

## Requisitos Previos

* **Java** 17 o superior (proyecto configurado con Java 21)
* **Maven** 3.8+
* **Node.js** 18 o superior y **npm**
* **Base de Datos Relacional** (PostgreSQL o MySQL, según configuración en `api/src/main/resources/application.*`)

## Ejecución Local

### 1. Base de Datos (`/bd`)

Creá la base de datos y ejecutá los scripts en orden (ejemplo con PostgreSQL; adaptá usuario/host/nombre de DB si corresponde):

```bash
psql -U postgres -d capital_humano -f bd/schema.sql
psql -U postgres -d capital_humano -f bd/seed.sql
```

### 2. Backend (`/api`)

```bash
cd api
mvn spring-boot:run
```

La API quedará disponible por defecto en `http://localhost:8080`.

### 3. Frontend (`/frontend`)

```bash
cd frontend
npm install
npm run dev
```

La aplicación web quedará disponible en `http://localhost:3000`.
