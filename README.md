# Capit@l Humano — Sistema de Gestión de Evaluaciones de Perfiles

Trabajo práctico integrador de **Diseño de Sistemas de Información** y **Desarrollo de Software** — **UTN FRSF (2026)**.

## Alcance de esta entrega (DSI)

En esta entrega de Diseño de Sistemas están cubiertos los casos de uso del cronograma y los que se invocan desde ellos:

* **CU001 — Autenticar usuario:** ingreso como consultor o para completar un cuestionario (documento y clave).

* **CU020 a CU023 — Preguntas:** búsqueda, alta, modificación y baja lógica de preguntas con opciones de respuesta ponderadas. Competencias, factores, opciones de respuesta, puestos y candidatos ya vienen cargados en `bd/seed.sql`.

* **CU025 y CU026 — Evaluar candidatos y Completar cuestionario:** generación de evaluaciones con claves de acceso y exportación a `.xls`; cuestionario por bloques con control de tiempo, vencimiento y cantidad de ingresos.

* **CU028 — Emitir orden de mérito:** listado por puesto y evaluación (cumple mínimos por competencia, no alcanza mínimos y no completados), imprimible.

El botón de generar pregunta con IA (`CU033`) está en el alta de pregunta como función auxiliar/opcional, no como requisito central de DSI. Los demás casos de uso y requisitos específicos de Desarrollo de Software (`CU006`, `CU009`, `CU013`, `CU017`, `CU024`, `CU029`, patrones, tests y demás) se trabajan para su entrega correspondiente.

## Estructura del repositorio

* `/api`: Backend en Java con Spring Boot (API REST, lógica de negocio y persistencia).

* `/frontend`: Aplicación web en Next.js.

* `/bd`: Scripts SQL de estructura (`schema.sql`) y datos iniciales (`seed.sql`).

* `/docs`: Documentación y diagramas UML (Clases, Entidad-Relación, Secuencia, Despliegue).

## Requisitos previos

* **Java** 21 (`java.version` 21 en `pom.xml`)

* **Maven** 3.8+ (o el incluido en IntelliJ)

* **Node.js** 20.9+ y **npm** (Next.js 16)

* **PostgreSQL** 16

## Ejecución local

### 1. Base de datos (`/bd`)

Desde la raíz del repositorio, verificá que PostgreSQL esté arriba, creá la base y cargá el esquema y los datos:

```
sudo service postgresql status || sudo service postgresql start
sudo -u postgres psql -c "DROP DATABASE IF EXISTS capital_humano;"
sudo -u postgres psql -c "CREATE DATABASE capital_humano;"
sudo -u postgres psql -d capital_humano -f bd/schema.sql
sudo -u postgres psql -d capital_humano -f bd/seed.sql

```

Si te falla la autenticación al entrar con `psql -U postgres` o necesitás fijar clave al usuario `postgres` en tu entorno local:

```
sudo -u postgres psql -c "ALTER USER postgres WITH PASSWORD 'postgres';"

```

`schema.sql` recrea las tablas desde cero, así que sirve para resetear la base cuando haga falta.

### 2. Backend (`/api`)

Copiá `api/src/main/resources/application.properties.example` como `application.properties` y poné la clave de PostgreSQL en `spring.datasource.password` (si usaste `postgres` arriba, reemplazá `TU_PASSWORD` por `postgres`). Después:

```
cd api
mvn spring-boot:run

```

También podés correr `CapitalHumanoApplication` desde IntelliJ. Queda escuchando en `http://localhost:8080`.

### 3. Frontend (`/frontend`)

```
cd frontend
npm install
npm run dev

```

Queda en `http://localhost:3000`. Si el backend corre en otro host o puerto, creá `frontend/.env.local` con `NEXT_PUBLIC_API_URL=http://host:puerto`.

## Datos de prueba (`seed.sql`)

| Uso | Datos | 
| ----- | ----- | 
| Ingresar como consultor | Usuario `CONSULTOR1` (o `NFRANCIONI`), cualquier contraseña (validación LDAP simulada en local) | 
| Completar cuestionario | DNI `35111222`, clave `A8K3M9P2` (cuestionario activo de `PEREZ` para `PROJECT MANAGER TIC`) | 
| Orden de mérito | Puesto `PUE-PM-01`: `SOSA` cumple mínimos (4,75), `GUTIERREZ` no alcanza el mínimo de `LEALTAD` (3,25), `PEREZ` está activo | 

Hay 3 preguntas por factor (incluyendo ejemplos del enunciado y de Arquitectura de Software) para que en cada cuestionario se sorteen 2 distintas. Cada pregunta usa la opción de respuesta correspondiente (verdadero/falso, escala de valoración, frecuencia o frases propias como en `LEALTAD`).

## Generar preguntas con IA (`CU033`)

Desde el alta de pregunta se puede invocar a la API de Gemini enviando competencia, factor, preguntas existentes del factor y opciones de respuesta para que proponga nombre, enunciado, descripción, opción y ponderaciones. No guarda nada hasta que se revisa y se presiona **Aceptar**.

1. Generá una API key en Google AI Studio (plan gratuito, sin facturación).

2. Agregá en `api/src/main/resources/application.properties` (no se sube al repo):

```
gemini.api.key=TU_CLAVE

```

3. Reiniciá el backend.

Por defecto usa `gemini-3.5-flash-lite` (podés cambiarlo con `gemini.model=nombre-del-modelo`). Si falta la clave o falla la llamada, muestra el aviso del flujo 4.A y el motivo del error. Solo se envían datos del catálogo, nunca datos de candidatos.

## Rutas del frontend

| Ruta | Caso de uso | 
| ----- | ----- | 
| `/` | CU001 Autenticar usuario | 
| `/inicio` | Menú principal del consultor | 
| `/preguntas` | CU020 Gestionar preguntas (CU021, CU022, CU023 y CU033 desde el alta) | 
| `/evaluar` | CU025 Evaluar a candidatos | 
| `/cuestionario` | CU026 Completar cuestionario (acceso desde `/`) | 
| `/reportes/orden-merito` | CU028 Emitir orden de mérito | 

## Endpoints

Los errores de negocio devuelven `400` con `{ "error": "...", "detalles": ["..."] }`.

| CU | Método y ruta | 
| ----- | ----- | 
| CU001 | `POST /api/auth/consultor` `{ username, password }` | 
| CU001 | `POST /api/auth/candidato` `{ tipoDocumento, nroDocumento, clave }` | 
| CU020 | `GET /api/catalogo/preguntas?competenciaId=&factorId=&nombre=` | 
| CU020, CU021 | `GET /api/catalogo/competencias?nombre=` y `GET /api/catalogo/factores?competenciaId=&nombre=` | 
| CU021, CU022 | `GET /api/catalogo/opciones-respuesta?nombre=` | 
| CU021 | `POST /api/catalogo/preguntas` | 
| CU022 | `PUT /api/catalogo/preguntas/{id}` | 
| CU023 | `DELETE /api/catalogo/preguntas/{id}?usuario=` | 
| CU033 | `POST /api/catalogo/preguntas/generar-ia` `{ competenciaId, factorId }` | 
| CU025 | `GET /api/evaluaciones/candidatos?apellido=&nombre=&nroCandidato=` | 
| CU025 | `POST /api/evaluaciones/validar-candidatos` `{ candidatoIds }` | 
| CU025 | `GET /api/catalogo/puestos?codigo=&nombre=&empresa=` | 
| CU025 | `GET /api/evaluaciones/puestos/{puestoId}/competencias` | 
| CU025 | `POST /api/evaluaciones/claves` `{ candidatoIds, puestoId }` | 
| CU025 | `POST /api/evaluaciones/generar` `{ candidatoIds, puestoId, claves }` | 
| CU026 | `GET /api/evaluaciones/configuracion` | 
| CU026 | `POST /api/evaluaciones/cuestionarios/{id}/iniciar` | 
| CU026 | `POST /api/evaluaciones/cuestionarios/{id}/bloques/{nroBloque}/responder` `{ respuestas }` | 
| CU028 | `GET /api/reportes/puestos?codigo=&nombre=&empresa=` | 
| CU028 | `GET /api/reportes/puestos/{puestoId}/evaluaciones` | 
| CU028 | `GET /api/reportes/orden-merito?puestoId=&evaluacion=TODAS&emitidoPor=` | 

Las evaluaciones creadas en conjunto en CU025 para un puesto comparten fecha de creación y se identifican como `EVA-001`, `EVA-002`, etc.

## Interfaz y estilos

* Estética tipo "hoja de respuestas" con colores y tipografía definidos en `frontend/src/app/globals.css` y fuente local Atkinson Hyperlegible Next en `frontend/src/fonts`.

* Componentes reutilizables en `frontend/src/components/ui` y cliente HTTP en `frontend/src/lib/api.ts`.

* Comportamientos de carga: ingreso en mayúsculas, `Shift+Tab` para retroceder campo, `Enter` equivalente a clic y mensajes de validación visibles sin tapar campos ni botones.