# Capit@l Humano — Sistema de Gestión de Evaluaciones de Perfiles

Trabajo Práctico Integrador de **Diseño de Sistemas de Información** y **Desarrollo de Software** — **UTN FRSF (2026)**.

> **Rama `entrega-dsi`:** versión para la entrega de Diseño de Sistemas de Información. Tiene solo los casos de uso que pide el cronograma de DSI (CU020, CU021, CU023, CU025, CU026 y CU028) y los que esos casos de uso necesitan para funcionar: CU001 (el candidato ingresa por ahí al CU026), CU022 (se llega desde el CU020) y CU033 (botón "Generar con IA" del CU021, flujo alternativo 1.A.1).

## Alcance y Funcionalidades Implementadas

* **Autenticación (CU001):** ingreso como consultor (usuario y contraseña) o para realizar un cuestionario (documento y clave).
* **Preguntas (CU020 a CU023):** búsqueda, alta, modificación y baja lógica de preguntas con sus opciones de respuesta ponderadas. Las competencias, factores, opciones de respuesta, puestos y candidatos vienen cargados en `bd/seed.sql`.
* **Evaluación y Cuestionario (CU025 y CU026):** asistente de evaluación con claves de ingreso y exportación a Excel (.xls); cuestionario por bloques con cronómetro, control de tiempo máximo, vencimiento y cantidad de accesos.
* **Orden de mérito (CU028):** por puesto y evaluación (cumple mínimos por competencia, no alcanza mínimos y no completados), imprimible.
* **Generar pregunta con IA (CU033):** desde el alta de pregunta, propone una pregunta nueva para un factor usando la API de Gemini.

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

Para usar **Generar con IA** (CU033) hace falta además una clave de Gemini: ver la sección de abajo.

Para correr las pruebas unitarias y ver la cobertura, ver [Pruebas unitarias y cobertura](#pruebas-unitarias-y-cobertura).

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

## Patrones de diseño

| Patrón | Dónde está | Para qué se usa |
|---|---|---|
| **DAO** | `api/.../dao/*Dao` | Los servicios leen y guardan solo a través de estas interfaces (Spring Data JPA); no conocen SQL ni la base. |
| **Template Method** | `service/baja/BajaLogica` y su subclase `BajaPregunta` | La baja lógica de pregunta (CU023) sigue pasos fijos en un orden fijo: buscar, verificar que no esté en uso, dar de baja lo asociado, desactivar, guardar y auditar. `ejecutar` es el método plantilla (final); la subclase completa solo lo que depende de la entidad. |
| **State** | `service/estado/`: `EstadoCuestionario`, `EstadoActivo`, `EstadoEnProceso` y los estados finales | Lo que se puede hacer con un cuestionario (vencer, ingresar, responder, completar) depende de su estado. Cada estado tiene sus reglas y hace sus propias transiciones, sin `if` por estado en los servicios. |
| **Strategy** | `service/ProveedorIA` (estrategia), `ClienteGemini` (estrategia concreta), `GeneradorPreguntaIAService` (contexto) | El CU033 no depende de Gemini: usar otro proveedor de IA es agregar otra clase que implemente `ProveedorIA`. En los tests se usa un proveedor falso. |

**Herencia y polimorfismo:** `BajaLogica<T>` es abstracta y `BajaPregunta` la extiende; `CatalogoService` la usa como `BajaLogica<Pregunta>`. `EstadoBase` define el comportamiento de un estado final y `EstadoActivo` y `EstadoEnProceso` redefinen lo que les corresponde; `EvaluacionCuestionarioService` llama a `estado.registrarIngreso(...)` o `estado.completar(...)` sin preguntar en qué estado está el cuestionario.

## Pruebas unitarias y cobertura

Pruebas de la capa de servicio con **JUnit 5 + Mockito** (113 tests). No necesitan PostgreSQL ni la clave de Gemini: los DAOs se reemplazan con mocks y la API de Gemini con un servidor HTTP local.

```bash
cd api
mvn test
```

Al terminar, el informe de cobertura de **JaCoCo** queda en `api/target/site/jacoco/index.html`. Para ver cada archivo, entrá al paquete `ar.edu.utn.frsf.isi.capitalhumano.service` (y a sus subpaquetes `baja` y `estado`). Desde IntelliJ también se puede: clic derecho en `api/src/test/java` → **More Run/Debug** → **Run 'All Tests' with Coverage**.

| Clase de test | Qué prueba |
|---|---|
| `CatalogoServiceTest` | CU020 a CU023 (búsqueda, alta, modificación y baja de preguntas) y las búsquedas de competencias, factores, opciones de respuesta y puestos |
| `EvaluacionCuestionarioServiceTest` | CU025 (validaciones, claves y generación de cuestionarios) y CU026 (sorteo de preguntas, bloques y puntaje) |
| `VigenciaCuestionarioServiceTest`, `estado/EstadosCuestionarioTest` | Vencimientos de CU026 y transiciones de estado (State) |
| `ReporteServiceTest` | CU028 Orden de mérito |
| `GeneradorPreguntaIAServiceTest`, `ClienteGeminiTest` | CU033 (Strategy): el servicio con un proveedor de IA falso, y el cliente de Gemini contra un servidor local |
| `AuthServiceTest` | CU001 Autenticar usuario |
| `baja/BajaLogicaTest` | El orden fijo de los pasos de una baja (Template Method) |

Los mocks usan el modo estricto de Mockito (`MockitoExtension`): un test falla si prepara una respuesta que el servicio nunca usa, o si el servicio llama a un método preparado pero con otros datos.

## Diseño del Frontend

* Sistema visual "hoja de respuestas": tinta rosada de formulario sobre papel blanco y marcas en grafito. Los colores y la escala tipográfica están en `frontend/src/app/globals.css`.
* Componentes compartidos en `frontend/src/components/ui` (hoja, grilla con círculo de selección, campos, botones, diálogos de los cuatro tipos de mensaje) y llamadas a la API en `frontend/src/lib/api.ts`.
* Tipografía Atkinson Hyperlegible Next (licencia SIL OFL, incluida en `frontend/src/fonts`), servida localmente.
* Requerimientos especiales: el texto se ingresa siempre en mayúsculas, Shift+Tab retrocede un campo, Enter equivale al clic y las altas muestran los errores sin tapar campos ni botones.
