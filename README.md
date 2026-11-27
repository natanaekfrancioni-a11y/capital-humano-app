# Capit@l Humano — Sistema de Gestión de Evaluaciones de Perfiles

Trabajo Práctico Integrador de **Diseño de Sistemas de Información** y **Desarrollo de Software** — **UTN FRSF (2026)**.

## Alcance y Funcionalidades Implementadas

- **Autenticación (CU001):** ingreso como consultor (usuario y contraseña) o para realizar un cuestionario (documento y clave).
- **Preguntas (CU020 a CU023):** búsqueda, alta, modificación y baja lógica de preguntas con sus opciones de respuesta ponderadas. Las competencias, factores, opciones de respuesta, puestos y candidatos vienen cargados en `bd/seed.sql`.
- **Evaluación y Cuestionario (CU025 y CU026):** asistente de evaluación con claves de ingreso y exportación a Excel (.xls); cuestionario por bloques con cronómetro, control de tiempo máximo, vencimiento y cantidad de accesos.
- **Orden de mérito (CU028):** por puesto y evaluación (cumple mínimos por competencia, no alcanza mínimos y no completados), imprimible.
- **Generar pregunta con IA (CU033):** desde el alta de pregunta, propone una pregunta nueva para un factor usando la API de Gemini.

## Estructura del Repositorio

- `/api`: Backend en Java con Spring Boot (API REST, lógica de negocio y persistencia).
- `/frontend`: Aplicación web en Next.js.
- `/bd`: Scripts SQL de estructura (`schema.sql`) y datos iniciales (`seed.sql`).
- `/docs`: Documentación y diagramas UML (Clases, Entidad-Relación, Secuencia, Despliegue).

## Requisitos Previos

- **Java** 21 (el `pom.xml` fija `java.version` 21)
- **Maven** 3.8+ (o el que trae IntelliJ)
- **Node.js** 20.9 o superior y **npm** (lo exige Next.js 16)
- **PostgreSQL** 16

## Ejecución Local

### 1. Base de Datos (`/bd`)

Asegurate de que PostgreSQL esté corriendo y creá la base junto con el esquema y los datos iniciales (desde la raíz del repositorio):

```bash
sudo service postgresql status || sudo service postgresql start
sudo -u postgres psql -c "DROP DATABASE IF EXISTS capital_humano;"
sudo -u postgres psql -c "CREATE DATABASE capital_humano;"
sudo -u postgres psql -d capital_humano -f bd/schema.sql
sudo -u postgres psql -d capital_humano -f bd/seed.sql
```

Si te conectás directamente con `psql -U postgres` y falla por autenticación (`Peer authentication` o contraseña), podés definir la contraseña del usuario `postgres` en local (por ejemplo `postgres`):

```bash
sudo -u postgres psql -c "ALTER USER postgres WITH PASSWORD 'postgres';"
```

`schema.sql` borra y vuelve a crear todas las tablas, así que sirve también para reiniciar los datos de prueba.

### 2. Backend (`/api`)

Copiá `api/src/main/resources/application.properties.example` como `application.properties` y configurá `spring.datasource.password` con la contraseña de tu usuario `postgres` (por ejemplo, si usaste `postgres` arriba, reemplazá `TU_PASSWORD` por `postgres`). Después:

```bash
cd api
mvn spring-boot:run
```

(o ejecutá `CapitalHumanoApplication` desde IntelliJ). La API queda en `http://localhost:8080`.

Para usar **Generar con IA** (CU033) hace falta además una clave de Gemini: ver la sección de abajo.

Para correr las pruebas unitarias y ver la cobertura, ver [Pruebas unitarias y cobertura](#pruebas-unitarias-y-cobertura).

### 3. Frontend (`/frontend`)

```bash
cd frontend
npm install
npm run dev
```

La aplicación queda en `http://localhost:3000`. Si la API corre en otra dirección, creá `frontend/.env.local` con `NEXT_PUBLIC_API_URL=http://host:puerto`.

## Pruebas unitarias y cobertura

Desde `api/`:

```bash
cd api
mvn clean test
```

Si tienen configurado JaCoCo en `pom.xml`, podés generar el reporte con `mvn test jacoco:report` y revisar el HTML en `api/target/site/jacoco/index.html`.

## Datos de prueba (seed)

| Para | Datos |
|---|---|
| Ingresar como consultor | Usuario `CONSULTOR1` (o `NFRANCIONI`), cualquier contraseña: la validación contra el LDAP está simulada |
| Realizar un cuestionario | DNI `35111222`, clave `A8K3M9P2` (cuestionario activo de PEREZ para PROJECT MANAGER TIC) |
| Orden de mérito | Puesto `PUE-PM-01`: SOSA cumple los mínimos (4,75), GUTIERREZ no alcanza el de LEALTAD (3,25), PEREZ está activo |

Las preguntas son las del enunciado más algunas de Arquitectura de Software: 3 por factor, así cada cuestionario sortea 2 distintas. Cada pregunta tiene una opción de respuesta que le corresponde: verdadero o falso, una escala de valoración, una de frecuencia o frases propias de la pregunta, como en el ejemplo de LEALTAD del enunciado.

## Generar preguntas con IA (CU033)

El botón **Generar con IA** del alta de pregunta usa la API de Google Gemini. El backend le envía la competencia, el factor, las preguntas que ya tiene ese factor y las opciones de respuesta cargadas, y la IA propone el nombre, el texto, la descripción, la opción de respuesta y las ponderaciones. No se guarda nada hasta que el consultor revisa los datos y presiona **Aceptar**.

1. Sacá una clave gratuita en [Google AI Studio](https://aistudio.google.com/apikey) con **Create API key**. No actives la facturación: así queda en el plan gratuito.
2. Agregá esta línea a tu `api/src/main/resources/application.properties` (ese archivo no se sube al repositorio):

   ```properties
   gemini.api.key=TU_CLAVE
   ```

3. Reiniciá el backend.

Por defecto se usa el modelo `gemini-3.5-flash-lite`, que en el plan gratuito permite más consultas por día. Para usar otro, agregá `gemini.model=nombre-del-modelo`. Sin clave, o si el servicio falla, la pantalla muestra el mensaje del flujo 4.A ("Ocurrió un error al comunicarse con el servicio de IA...") y debajo la causa: clave inválida, límite de consultas alcanzado, modelo inexistente o sin conexión. En el plan gratuito Google puede usar los pedidos para mejorar sus productos; el sistema solo envía datos del catálogo, nunca datos de candidatos.

## Pantallas del Frontend

| Ruta | Caso de uso |
|---|---|
| `/` | CU001 Autenticar usuario |
| `/inicio` | Menú principal del consultor |
| `/preguntas` | CU020 Gestionar preguntas (CU021, CU022, CU023 y CU033 desde el alta) |
| `/evaluar` | CU025 Evaluar a candidatos |
| `/cuestionario` | CU026 Completar cuestionario (el candidato entra por `/`) |
| `/reportes/orden-merito` | CU028 Emitir orden de mérito |

## Endpoints por Caso de Uso

Los errores de negocio responden `400` con `{ "error": "...", "detalles": ["..."] }`.

| CU | Método y ruta |
|---|---|
| CU001 | `POST /api/auth/consultor` `{ username, password }` |
| CU001 | `POST /api/auth/candidato` `{ tipoDocumento, nroDocumento, clave }` |
| CU020 | `GET /api/catalogo/preguntas?competenciaId=&factorId=&nombre=` |
| CU020, CU021 | `GET /api/catalogo/competencias?nombre=` y `GET /api/catalogo/factores?competenciaId=&nombre=` (combos de búsqueda y de alta) |
| CU021, CU022 | `GET /api/catalogo/opciones-respuesta?nombre=` (opciones de respuesta para elegir) |
| CU021 | `POST /api/catalogo/preguntas` (el código se genera si no se envía) |
| CU022 | `PUT /api/catalogo/preguntas/{id}` |
| CU023 | `DELETE /api/catalogo/preguntas/{id}?usuario=` |
| CU033 | `POST /api/catalogo/preguntas/generar-ia` `{ competenciaId, factorId }` (no guarda: devuelve los datos para el alta) |
| CU025 | `GET /api/evaluaciones/candidatos?apellido=&nombre=&nroCandidato=` |
| CU025 | `POST /api/evaluaciones/validar-candidatos` `{ candidatoIds }` (paso 6) |
| CU025 | `GET /api/catalogo/puestos?codigo=&nombre=&empresa=` (paso 8) |
| CU025 | `GET /api/evaluaciones/puestos/{puestoId}/competencias` (paso 9) |
| CU025 | `POST /api/evaluaciones/claves` `{ candidatoIds, puestoId }` (pasos 10-11, no guarda) |
| CU025 | `POST /api/evaluaciones/generar` `{ candidatoIds, puestoId, claves }` (paso 13) |
| CU026 | `GET /api/evaluaciones/configuracion` |
| CU026 | `POST /api/evaluaciones/cuestionarios/{id}/iniciar` |
| CU026 | `POST /api/evaluaciones/cuestionarios/{id}/bloques/{nroBloque}/responder` `{ respuestas }` |
| CU028 | `GET /api/reportes/puestos?codigo=&nombre=&empresa=` |
| CU028 | `GET /api/reportes/puestos/{puestoId}/evaluaciones` |
| CU028 | `GET /api/reportes/orden-merito?puestoId=&evaluacion=TODAS&emitidoPor=` |

Una **evaluación** es el grupo de cuestionarios que se generan juntos en el CU025 para un puesto: comparten la fecha de creación y se numeran `EVA-001`, `EVA-002`, ... por puesto.

## Diseño del Frontend

- Sistema visual "hoja de respuestas": tinta rosada de formulario sobre papel blanco y marcas en grafito. Los colores y la escala tipográfica están en `frontend/src/app/globals.css`.
- Componentes compartidos en `frontend/src/components/ui` (hoja, grilla con círculo de selección, campos, botones, diálogos de los cuatro tipos de mensaje) y llamadas a la API en `frontend/src/lib/api.ts`.
- Tipografía Atkinson Hyperlegible Next (licencia SIL OFL, incluida en `frontend/src/fonts`), servida localmente.
- Requerimientos especiales: el texto se ingresa siempre en mayúsculas, Shift+Tab retrocede un campo, Enter equivale al clic y las altas muestran los errores sin tapar campos ni botones.