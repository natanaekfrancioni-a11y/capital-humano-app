-- esquema de la base de datos de Capit@l Humano (PostgreSQL)

DROP TABLE IF EXISTS respuesta_cuestionario CASCADE;
DROP TABLE IF EXISTS pregunta_cuestionario CASCADE;
DROP TABLE IF EXISTS cuestionario CASCADE;
DROP TABLE IF EXISTS ponderacion_evaluacion CASCADE;
DROP TABLE IF EXISTS pregunta_evaluacion CASCADE;
DROP TABLE IF EXISTS evaluacion CASCADE;
DROP TABLE IF EXISTS configuracion_cuestionario CASCADE;
DROP TABLE IF EXISTS ponderacion_opcion_pregunta CASCADE;
DROP TABLE IF EXISTS pregunta CASCADE;
DROP TABLE IF EXISTS detalle_opcion_respuesta CASCADE;
DROP TABLE IF EXISTS opcion_respuesta CASCADE;
DROP TABLE IF EXISTS puesto_competencia CASCADE;
DROP TABLE IF EXISTS puesto CASCADE;
DROP TABLE IF EXISTS factor CASCADE;
DROP TABLE IF EXISTS competencia CASCADE;
DROP TABLE IF EXISTS candidato CASCADE;
DROP TABLE IF EXISTS usuario_consultor CASCADE;
DROP TABLE IF EXISTS auditoria_eliminacion CASCADE;

CREATE TABLE usuario_consultor (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(15)  NOT NULL UNIQUE,
    nombre_completo VARCHAR(120) NOT NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE candidato (
    id               BIGSERIAL PRIMARY KEY,
    nro_candidato    VARCHAR(30)  NOT NULL UNIQUE,
    apellido         VARCHAR(80)  NOT NULL,
    nombre           VARCHAR(80)  NOT NULL,
    tipo_documento   VARCHAR(5)   NOT NULL CHECK (tipo_documento IN ('DNI', 'LE', 'LC', 'PP')),
    nro_documento    VARCHAR(25)  NOT NULL,
    fecha_nacimiento DATE         NOT NULL,
    genero           VARCHAR(5)   NOT NULL CHECK (genero IN ('H', 'M')),
    nacionalidad     VARCHAR(60)  NOT NULL,
    escolaridad      VARCHAR(80)  NOT NULL,
    email            VARCHAR(120) NOT NULL,
    eliminado        BOOLEAN      NOT NULL DEFAULT FALSE,
    actualizado_en   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_candidato_doc UNIQUE (tipo_documento, nro_documento)
);

CREATE TABLE competencia (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(30)  NOT NULL UNIQUE,
    nombre      VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255) NOT NULL,
    tipo        VARCHAR(20)  NOT NULL DEFAULT 'TECNICA' CHECK (tipo IN ('TECNICA', 'ACTITUDINAL')),
    activa      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE factor (
    id            BIGSERIAL PRIMARY KEY,
    competencia_id BIGINT      NOT NULL REFERENCES competencia(id),
    codigo        VARCHAR(30)  NOT NULL UNIQUE,
    nombre        VARCHAR(100) NOT NULL,
    descripcion   VARCHAR(255) NOT NULL,
    nro_orden     INTEGER      NOT NULL CHECK (nro_orden > 0),
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_factor_comp_orden UNIQUE (competencia_id, nro_orden),
    CONSTRAINT uq_factor_comp_nombre UNIQUE (competencia_id, nombre)
);

CREATE TABLE opcion_respuesta (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255) NOT NULL,
    activa      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE detalle_opcion_respuesta (
    id                 BIGSERIAL PRIMARY KEY,
    opcion_respuesta_id BIGINT     NOT NULL REFERENCES opcion_respuesta(id) ON DELETE CASCADE,
    nro_orden          INTEGER      NOT NULL CHECK (nro_orden > 0),
    descripcion        VARCHAR(120) NOT NULL,
    CONSTRAINT uq_detalle_op_orden UNIQUE (opcion_respuesta_id, nro_orden),
    CONSTRAINT uq_detalle_op_desc  UNIQUE (opcion_respuesta_id, descripcion)
);

CREATE TABLE pregunta (
    id                 BIGSERIAL PRIMARY KEY,
    factor_id          BIGSERIAL NOT NULL REFERENCES factor(id),
    opcion_respuesta_id BIGINT   NOT NULL REFERENCES opcion_respuesta(id),
    codigo             VARCHAR(30)  NOT NULL UNIQUE,
    nombre             VARCHAR(120) NOT NULL,
    enunciado          TEXT         NOT NULL,
    descripcion        VARCHAR(255),
    activa             BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pregunta_factor_nombre UNIQUE (factor_id, nombre)
);

CREATE TABLE ponderacion_opcion_pregunta (
    id                        BIGSERIAL PRIMARY KEY,
    pregunta_id               BIGSERIAL NOT NULL REFERENCES pregunta(id) ON DELETE CASCADE,
    detalle_opcion_respuesta_id BIGINT   NOT NULL REFERENCES detalle_opcion_respuesta(id) ON DELETE CASCADE,
    ponderacion               NUMERIC(5,2) NOT NULL CHECK (ponderacion >= 0),
    CONSTRAINT uq_pond_preg_detalle UNIQUE (pregunta_id, detalle_opcion_respuesta_id)
);

CREATE TABLE puesto (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(30)  NOT NULL UNIQUE,
    nombre      VARCHAR(120) NOT NULL UNIQUE,
    empresa     VARCHAR(120) NOT NULL,
    descripcion VARCHAR(300) NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE puesto_competencia (
    id                 BIGSERIAL PRIMARY KEY,
    puesto_id          BIGSERIAL NOT NULL REFERENCES puesto(id) ON DELETE CASCADE,
    competencia_id     BIGSERIAL NOT NULL REFERENCES competencia(id),
    ponderacion_minima NUMERIC(5,2) NOT NULL CHECK (ponderacion_minima >= 0 AND ponderacion_minima <= 10),
    CONSTRAINT uq_puesto_competencia UNIQUE (puesto_id, competencia_id)
);

CREATE TABLE configuracion_cuestionario (
    id                      BIGSERIAL PRIMARY KEY,
    preguntas_por_bloque    INTEGER      NOT NULL DEFAULT 3 CHECK (preguntas_por_bloque > 0),
    max_accesos             INTEGER      NOT NULL DEFAULT 5 CHECK (max_accesos > 0),
    tiempo_max_minutos      INTEGER      NOT NULL DEFAULT 60 CHECK (tiempo_max_minutos > 0),
    dias_vencimiento_activo INTEGER      NOT NULL DEFAULT 15 CHECK (dias_vencimiento_activo > 0),
    instrucciones           TEXT         NOT NULL,
    actualizado_en          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- evaluación generada en el CU025 para un puesto, se numera dentro del puesto (EVA-001, EVA-002, ...)
CREATE TABLE evaluacion (
    id              BIGSERIAL PRIMARY KEY,
    puesto_id       BIGINT       NOT NULL REFERENCES puesto(id),
    nro_evaluacion  INTEGER      NOT NULL CHECK (nro_evaluacion > 0),
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_evaluacion_puesto_nro UNIQUE (puesto_id, nro_evaluacion)
);

-- modelo congelado de la evaluación (CU025): copia de cada pregunta que se puede sortear con sus ponderaciones
CREATE TABLE pregunta_evaluacion (
    id             BIGSERIAL PRIMARY KEY,
    evaluacion_id  BIGINT       NOT NULL REFERENCES evaluacion(id) ON DELETE CASCADE,
    factor_id      BIGINT       NOT NULL REFERENCES factor(id),
    nombre         VARCHAR(120) NOT NULL,
    enunciado      TEXT         NOT NULL,
    CONSTRAINT uq_pregunta_evaluacion UNIQUE (evaluacion_id, factor_id, nombre)
);

CREATE TABLE ponderacion_evaluacion (
    id                          BIGSERIAL PRIMARY KEY,
    pregunta_evaluacion_id      BIGINT       NOT NULL REFERENCES pregunta_evaluacion(id) ON DELETE CASCADE,
    detalle_opcion_respuesta_id BIGINT       NOT NULL REFERENCES detalle_opcion_respuesta(id),
    ponderacion                 NUMERIC(5,2) NOT NULL CHECK (ponderacion >= 0),
    CONSTRAINT uq_ponderacion_evaluacion UNIQUE (pregunta_evaluacion_id, detalle_opcion_respuesta_id)
);

CREATE TABLE cuestionario (
    id                  BIGSERIAL PRIMARY KEY,
    candidato_id        BIGSERIAL NOT NULL REFERENCES candidato(id),
    evaluacion_id       BIGINT       NOT NULL REFERENCES evaluacion(id),
    clave_acceso        VARCHAR(8)   NOT NULL,
    estado              VARCHAR(20)  NOT NULL CHECK (estado IN ('ACTIVO', 'EN_PROCESO', 'COMPLETO', 'INCOMPLETO', 'SIN_CONTESTAR')),
    bloque_actual       INTEGER      NOT NULL DEFAULT 1 CHECK (bloque_actual >= 1),
    cantidad_accesos    INTEGER      NOT NULL DEFAULT 0 CHECK (cantidad_accesos >= 0),
    fecha_creacion      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_inicio        TIMESTAMP,
    fecha_ultimo_acceso TIMESTAMP,
    fecha_fin           TIMESTAMP,
    fecha_cambio_estado TIMESTAMP,
    puntaje_total       NUMERIC(6,2)
);

-- preguntas sorteadas para cada cuestionario (CU026), salen del modelo congelado de su evaluación
CREATE TABLE pregunta_cuestionario (
    id                     BIGSERIAL PRIMARY KEY,
    cuestionario_id        BIGSERIAL NOT NULL REFERENCES cuestionario(id) ON DELETE CASCADE,
    pregunta_evaluacion_id BIGINT       NOT NULL REFERENCES pregunta_evaluacion(id),
    nro_bloque             INTEGER      NOT NULL CHECK (nro_bloque >= 1),
    orden_visual           INTEGER      NOT NULL CHECK (orden_visual >= 1),
    respondida             BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_preg_cuestionario UNIQUE (cuestionario_id, pregunta_evaluacion_id)
);

CREATE TABLE respuesta_cuestionario (
    id                        BIGSERIAL PRIMARY KEY,
    pregunta_cuestionario_id  BIGSERIAL NOT NULL UNIQUE REFERENCES pregunta_cuestionario(id) ON DELETE CASCADE,
    detalle_opcion_respuesta_id BIGINT   NOT NULL REFERENCES detalle_opcion_respuesta(id),
    ponderacion_obtenida      NUMERIC(5,2) NOT NULL CHECK (ponderacion_obtenida >= 0),
    respondida_en             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE auditoria_eliminacion (
    id              BIGSERIAL PRIMARY KEY,
    entidad         VARCHAR(50)  NOT NULL,
    entidad_id      BIGSERIAL NOT NULL,
    identificador   VARCHAR(120) NOT NULL,
    usuario_actor   VARCHAR(50)  NOT NULL,
    fecha_hora      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    detalle         VARCHAR(255)
);

CREATE INDEX idx_cuestionario_candidato_estado  ON cuestionario(candidato_id, estado);
CREATE INDEX idx_cuestionario_evaluacion_estado ON cuestionario(evaluacion_id, estado);
CREATE INDEX idx_factor_competencia            ON factor(competencia_id);
CREATE INDEX idx_pregunta_factor               ON pregunta(factor_id);
