# Capit@l Humano — Sistema de Gestión de Evaluaciones de Perfiles

Trabajo Práctico Integrador de **Diseño de Sistemas de Información** y **Desarrollo de Software** — **UTN FRSF (2026)**.

## Alcance y Funcionalidades Implementadas

* **Autenticación (CU001):** ingreso como consultor (usuario y contraseña) o para realizar un cuestionario (documento y clave).
* **Catálogo y Estructura:** gestión de puestos/funciones, competencias, factores, preguntas con opciones ponderadas y opciones de respuesta; importación de candidatos desde CSV.
* **Evaluación y Cuestionario:** asistente de evaluación con claves de ingreso y exportación a Excel (.xls); cuestionario por bloques con cronómetro, control de tiempo máximo, vencimiento y cantidad de accesos.
* **Reportes:** **Orden de mérito** por puesto y evaluación (cumple mínimos por competencia, no alcanza mínimos y no completados) y **Reporte comparativo** (tabla y gráfico), ambos imprimibles.

## Estructura del Repositorio

* `/api`: Backend en Java con Spring Boot (API REST, lógica de negocio y persistencia).
* `/frontend`: Aplicación web en Next.js.
* `/bd`: Scripts SQL de estructura (`schema.sql`) y datos iniciales (`seed.sql`).
* `/docs`: Documentación y diagramas UML (Clases, Entidad-Relación, Secuencia, Despliegue).

## Requisitos Previos

* **Java** 21 (el `pom.xml` fija `java.version` 21)
* **Maven** 3.8+ (o el que trae IntelliJ)
* **Node.js** 20.9 o superior y **npm** (lo exige Next.js 16)
* **PostgreSQL** 16

## Ejecución Local

### 1. Base de Datos (`/bd`)

Creá la base y ejecutá los scripts en orden (desde la raíz del repositorio; adaptá usuario y host si corresponde):

```bash
psql -U postgres -c "CREATE DATABASE capital_humano;"
psql -U postgres -d capital_humano -f bd/schema.sql
psql -U postgres -d capital_humano -f bd/seed.sql
```

`schema.sql` borra y vuelve a crear todas las tablas, así que sirve también para reiniciar los datos de prueba.

### 2. Backend (`/api`)

Copiá `api/src/main/resources/application.properties.example` como `application.properties` y poné tu contraseña de PostgreSQL. Después:

```bash
cd api
mvn spring-boot:run
```

(o ejecutá `CapitalHumanoApplication` desde IntelliJ). La API queda en `http://localhost:8080`.

Pruebas unitarias (JUnit 5 + Mockito):

```bash
cd api
mvn test
```

### 3. Frontend (`/frontend`)

```bash
cd frontend
npm install
npm run dev
```

La aplicación queda en `http://localhost:3000`. Si la API corre en otra dirección, creá `frontend/.env.local` con `NEXT_PUBLIC_API_URL=http://host:puerto`.

## Datos de prueba (seed)

| Para | Datos |
|---|---|
| Ingresar como consultor | Usuario `CONSULTOR1` (o `NFRANCIONI`), cualquier contraseña: la validación contra el LDAP está simulada |
| Realizar un cuestionario | DNI `35111222`, clave `A8K3M9P2` (cuestionario activo de PEREZ para PROJECT MANAGER TIC) |
| Orden de mérito y reporte comparativo | Puesto `PUE-PM-01`: SOSA cumple los mínimos, GUTIERREZ no alcanza el de LEALTAD, PEREZ está activo |

## Pantallas del Frontend

| Ruta | Caso de uso |
|---|---|
| `/` | CU001 Autenticar usuario |
| `/inicio` | Menú principal del consultor |
| `/candidatos/importar` | CU006 Importar candidatos |
| `/puestos` | CU008 Gestionar puestos (CU009 alta, CU010 modificar, CU011 baja) |
| `/competencias` | CU012 Gestión de competencias (CU013, CU014, CU015) |
| `/factores` | CU016 Gestión de factores (CU017, CU018, CU019) |
| `/preguntas` | CU020 Gestionar preguntas (CU021, CU022, CU023) |
| `/opciones-respuesta` | CU024 Gestionar opciones de respuesta (CU029, CU030, CU031) |
| `/evaluar` | CU025 Evaluar a candidatos |
| `/cuestionario` | CU026 Completar cuestionario (el candidato entra por `/`) |
| `/reportes/comparativo` | CU027 Emitir reporte comparativo |
| `/reportes/orden-merito` | CU028 Emitir orden de mérito |

## Endpoints por Caso de Uso

Los errores de negocio responden `400` con `{ "error": "...", "detalles": ["..."] }`.

| CU | Método y ruta |
|---|---|
| CU001 | `POST /api/auth/consultor` `{ username, password }` |
| CU001 | `POST /api/auth/candidato` `{ tipoDocumento, nroDocumento, clave }` |
| CU006 | `POST /api/candidatos/importar` `{ nombreArchivo, contenido }` (texto del CSV) |
| CU008 | `GET /api/catalogo/puestos?codigo=&nombre=&empresa=` |
| CU009 | `POST /api/catalogo/puestos` |
| CU010 | `PUT /api/catalogo/puestos/{id}` |
| CU011 | `GET /api/catalogo/puestos/{id}/en-uso` y `DELETE /api/catalogo/puestos/{id}?usuario=` |
| CU012 | `GET /api/catalogo/competencias?nombre=` |
| CU013 | `POST /api/catalogo/competencias` |
| CU014 | `PUT /api/catalogo/competencias/{id}` |
| CU015 | `GET /api/catalogo/competencias/{id}/en-uso` y `DELETE /api/catalogo/competencias/{id}?usuario=` |
| CU016 | `GET /api/catalogo/factores?competenciaId=&nombre=` |
| CU017 | `POST /api/catalogo/factores` |
| CU018 | `PUT /api/catalogo/factores/{id}` |
| CU019 | `DELETE /api/catalogo/factores/{id}?usuario=` |
| CU020 | `GET /api/catalogo/preguntas?competenciaId=&factorId=&nombre=` |
| CU021 | `POST /api/catalogo/preguntas` (el código se genera si no se envía) |
| CU022 | `PUT /api/catalogo/preguntas/{id}` |
| CU023 | `DELETE /api/catalogo/preguntas/{id}?usuario=` |
| CU024 | `GET /api/catalogo/opciones-respuesta?nombre=` |
| CU029 | `POST /api/catalogo/opciones-respuesta` |
| CU030 | `PUT /api/catalogo/opciones-respuesta/{id}` |
| CU031 | `DELETE /api/catalogo/opciones-respuesta/{id}?usuario=` |
| CU025 | `GET /api/evaluaciones/candidatos?apellido=&nombre=&nroCandidato=` |
| CU025 | `POST /api/evaluaciones/validar-candidatos` `{ candidatoIds }` (paso 6) |
| CU025 | `GET /api/evaluaciones/puestos/{puestoId}/competencias` (paso 9) |
| CU025 | `POST /api/evaluaciones/claves` `{ candidatoIds, puestoId }` (pasos 10-11, no guarda) |
| CU025 | `POST /api/evaluaciones/generar` `{ candidatoIds, puestoId, claves }` (paso 13) |
| CU026 | `GET /api/evaluaciones/configuracion` |
| CU026 | `POST /api/evaluaciones/cuestionarios/{id}/iniciar` |
| CU026 | `POST /api/evaluaciones/cuestionarios/{id}/bloques/{nroBloque}/responder` `{ respuestas }` |
| CU027 | `GET /api/reportes/comparativo/candidatos?puestoId=` y `GET /api/reportes/comparativo?cuestionarioId=` |
| CU028 | `GET /api/reportes/puestos?codigo=&nombre=&empresa=` |
| CU028 | `GET /api/reportes/puestos/{puestoId}/evaluaciones` |
| CU028 | `GET /api/reportes/orden-merito?puestoId=&evaluacion=TODAS&emitidoPor=` |

Una **evaluación** es el grupo de cuestionarios que se generan juntos en el CU025 para un puesto: comparten la fecha de creación y se numeran `EVA-001`, `EVA-002`, ... por puesto.

## Diseño del Frontend

* Sistema visual "hoja de respuestas": tinta rosada de formulario sobre papel blanco y marcas en grafito. Los colores y la escala tipográfica están en `frontend/src/app/globals.css`.
* Componentes compartidos en `frontend/src/components/ui` (hoja, grilla con círculo de selección, campos, botones, diálogos de los cuatro tipos de mensaje) y llamadas a la API en `frontend/src/lib/api.ts`.
* Tipografía Atkinson Hyperlegible Next (licencia SIL OFL, incluida en `frontend/src/fonts`), servida localmente.
* Requerimientos especiales: el texto se ingresa siempre en mayúsculas, Shift+Tab retrocede un campo, Enter equivale al clic y las altas muestran los errores sin tapar campos ni botones.
