-- ============================================================================
-- Capit@l Humano - Datos Iniciales de Prueba (Seed)
-- ============================================================================
-- El archivo está en UTF-8. Se aclara para que psql en Windows no altere las tildes.
SET client_encoding = 'UTF8';

INSERT INTO usuario_consultor (username, nombre_completo, activo) VALUES
    ('CONSULTOR1', 'MARIANA GOMEZ', TRUE),
    ('NFRANCIONI', 'NATANAEL FRANCIONI', TRUE);

INSERT INTO configuracion_cuestionario (
    preguntas_por_bloque, max_accesos, tiempo_max_minutos, dias_vencimiento_activo, instrucciones
) VALUES (
    2,
    5,
    90,
    30,
    'LEA ATENTAMENTE CADA PREGUNTA Y SELECCIONE UNA ÚNICA OPCIÓN. LAS PREGUNTAS SE PRESENTAN EN BLOQUES; UNA VEZ AVANZADO UN BLOQUE NO PODRÁ VOLVER A MODIFICARLO NI REVISAR SUS RESPUESTAS ANTERIORES.'
);

-- Candidatos
INSERT INTO candidato (
    nro_candidato, apellido, nombre, tipo_documento, nro_documento,
    fecha_nacimiento, genero, nacionalidad, escolaridad, email, eliminado
) VALUES
    ('CAND-001', 'PEREZ', 'LUCAS MARTIN', 'DNI', '35111222', '1995-04-12', 'H', 'ARGENTINA', 'UNIVERSITARIO COMPLETO', 'lucas.perez@ejemplo.com', FALSE),
    ('CAND-002', 'SOSA', 'VALERIA SOFIA', 'DNI', '37444555', '1997-08-23', 'M', 'ARGENTINA', 'UNIVERSITARIO COMPLETO', 'valeria.sosa@ejemplo.com', FALSE),
    ('CAND-003', 'GUTIERREZ', 'TOMAS ARIEL', 'DNI', '36888999', '1996-01-15', 'H', 'ARGENTINA', 'TERCIARIO COMPLETO', 'tomas.gutierrez@ejemplo.com', FALSE),
    ('CAND-004', 'RODRIGUEZ', 'CAMILA BELEN', 'DNI', '39222333', '1999-11-02', 'M', 'ARGENTINA', 'UNIVERSITARIO EN CURSO', 'camila.rodriguez@ejemplo.com', FALSE),
    ('CAND-005', 'MENDEZ', 'FEDERICO JUAN', 'DNI', '34555666', '1994-06-30', 'H', 'ARGENTINA', 'UNIVERSITARIO COMPLETO', 'federico.mendez@ejemplo.com', FALSE);

-- Competencias (las dos primeras son los ejemplos del enunciado)
INSERT INTO competencia (codigo, nombre, descripcion, tipo, activa) VALUES
    ('COMP-GP', 'GESTION DE PROYECTOS', 'CAPACIDAD PARA PLANIFICAR, EJECUTAR Y CONTROLAR PROYECTOS TIC', 'TECNICA', TRUE),
    ('COMP-LE', 'LEALTAD', 'COMPROMISO, INTEGRIDAD Y RESGUARDO DE INTERESES ORGANIZACIONALES', 'ACTITUDINAL', TRUE),
    ('COMP-AR', 'ARQUITECTURA DE SOFTWARE', 'DISEÑO DE SISTEMAS ESCALABLES, MANTENIBLES Y SEGUROS', 'TECNICA', TRUE);

-- Factores (2 por competencia)
INSERT INTO factor (competencia_id, codigo, nombre, descripcion, nro_orden, activo)
SELECT c.id, v.codigo, v.nombre, v.descripcion, v.nro_orden, TRUE
FROM (
    VALUES
        ('COMP-GP', 'FAC-GP-01', 'GESTION DEL ALCANCE', 'DEFINICION DE WBS Y LINEA BASE DE REQUERIMIENTOS', 1),
        ('COMP-GP', 'FAC-GP-02', 'GESTION DEL TIEMPO', 'CRONOGRAMAS, DEPENDENCIAS Y VALOR GANADO', 2),
        ('COMP-LE', 'FAC-LE-01', 'CREDIBILIDAD', 'COHERENCIA ENTRE ACCIONES Y COMPROMISOS PROFESIONALES', 1),
        ('COMP-LE', 'FAC-LE-02', 'COMPROMISO', 'ORIENTACION AL LOGRO COLECTIVO Y RESPONSABILIDAD', 2),
        ('COMP-AR', 'FAC-AR-01', 'DISEÑO EN CAPAS', 'SEPARACION DE RESPONSABILIDADES Y BAJO ACOPLAMIENTO', 1),
        ('COMP-AR', 'FAC-AR-02', 'PERSISTENCIA Y CALIDAD', 'MODELADO RELACIONAL, TRANSACCIONES Y PRUEBAS', 2)
) AS v(comp_codigo, codigo, nombre, descripcion, nro_orden)
JOIN competencia c ON c.codigo = v.comp_codigo;

-- ----------------------------------------------------------------------------
-- Opciones de respuesta.
-- Tres son genéricas (verdadero/falso, valoración y frecuencia) y el resto son
-- propias de una pregunta, como en el ejemplo de LEALTAD del enunciado, donde
-- cada pregunta tiene sus cinco frases a) a e).
-- ----------------------------------------------------------------------------
INSERT INTO opcion_respuesta (nombre, descripcion, activa) VALUES
    ('VERDADERO-FALSO', 'RESPUESTA BINARIA: VERDADERO O FALSO', TRUE),
    ('ESCALA-VALORACION-5', 'ESCALA DE CINCO NIVELES, DE MALO A EXCELENTE', TRUE),
    ('FRECUENCIA-5', 'FRECUENCIA CON QUE OCURRE UNA CONDUCTA, DE NUNCA A SIEMPRE', TRUE),
    ('TIPO-DEPENDENCIA-CRONOGRAMA', 'SIGNIFICADOS POSIBLES DE UNA DEPENDENCIA ENTRE ACTIVIDADES DE UN CRONOGRAMA', TRUE),
    ('RUTA-CRITICA', 'DEFINICIONES POSIBLES DE LA RUTA CRÍTICA DE UN PROYECTO', TRUE),
    ('FRASES-CREDIBILIDAD', 'FRASES DE IDENTIFICACIÓN PROFESIONAL SOBRE LA CREDIBILIDAD', TRUE),
    ('FRASES-COMPROMISO', 'MOTIVOS POR LOS QUE IMPORTA EL LOGRO DE OBJETIVOS COLECTIVOS', TRUE),
    ('OBJETIVO-ARQUITECTURA-CAPAS', 'OBJETIVOS POSIBLES DE SEPARAR UN SISTEMA EN CAPAS', TRUE),
    ('PROPIEDADES-ACID', 'LO QUE GARANTIZA CADA PROPIEDAD DE UNA TRANSACCIÓN', TRUE);

INSERT INTO detalle_opcion_respuesta (opcion_respuesta_id, nro_orden, descripcion)
SELECT o.id, v.nro_orden, v.descripcion
FROM (
    VALUES
        ('VERDADERO-FALSO', 1, 'VERDADERO'),
        ('VERDADERO-FALSO', 2, 'FALSO'),
        ('ESCALA-VALORACION-5', 1, 'MALO'),
        ('ESCALA-VALORACION-5', 2, 'REGULAR'),
        ('ESCALA-VALORACION-5', 3, 'BUENO'),
        ('ESCALA-VALORACION-5', 4, 'MUY BUENO'),
        ('ESCALA-VALORACION-5', 5, 'EXCELENTE'),
        ('FRECUENCIA-5', 1, 'NUNCA'),
        ('FRECUENCIA-5', 2, 'CASI NUNCA'),
        ('FRECUENCIA-5', 3, 'A VECES'),
        ('FRECUENCIA-5', 4, 'CASI SIEMPRE'),
        ('FRECUENCIA-5', 5, 'SIEMPRE'),
        ('TIPO-DEPENDENCIA-CRONOGRAMA', 1, 'LA ACTIVIDAD SUCESORA NO PUEDE COMENZAR HASTA QUE TERMINE LA PREDECESORA'),
        ('TIPO-DEPENDENCIA-CRONOGRAMA', 2, 'LA ACTIVIDAD SUCESORA NO PUEDE TERMINAR HASTA QUE COMIENCE LA PREDECESORA'),
        ('TIPO-DEPENDENCIA-CRONOGRAMA', 3, 'AMBAS ACTIVIDADES DEBEN COMENZAR AL MISMO TIEMPO'),
        ('TIPO-DEPENDENCIA-CRONOGRAMA', 4, 'AMBAS ACTIVIDADES DEBEN TERMINAR AL MISMO TIEMPO'),
        ('RUTA-CRITICA', 1, 'LA SECUENCIA DE ACTIVIDADES MÁS LARGA, QUE DETERMINA LA DURACIÓN MÍNIMA DEL PROYECTO'),
        ('RUTA-CRITICA', 2, 'EL CONJUNTO DE ACTIVIDADES DE MAYOR COSTO'),
        ('RUTA-CRITICA', 3, 'LAS ACTIVIDADES QUE TIENEN MÁS RECURSOS ASIGNADOS'),
        ('RUTA-CRITICA', 4, 'LAS ACTIVIDADES QUE TODAVÍA TIENEN HOLGURA'),
        ('FRASES-CREDIBILIDAD', 1, 'SOY DIGNO DE CONFIANZA, PORQUE A TRAVÉS DE MIS HECHOS PROFESIONALES LO DEMUESTRO.'),
        ('FRASES-CREDIBILIDAD', 2, 'CUMPLO DE MANERA ÍNTEGRA CON LO QUE SE ME PIDE EN EL TRABAJO.'),
        ('FRASES-CREDIBILIDAD', 3, 'GUARDO LOS INTERESES DE LA EMPRESA SOBRE CUALQUIER SITUACIÓN QUE PUDIERA AFECTARLA.'),
        ('FRASES-CREDIBILIDAD', 4, 'SOY SOLIDARIO CON MIS COMPAÑEROS DE TRABAJO BUSCANDO EL BIEN DE LA EMPRESA.'),
        ('FRASES-CREDIBILIDAD', 5, 'EN MIS ACTIVIDADES PROFESIONALES ASUMO TODOS LOS COMPROMISOS QUE CONTRAIGO.'),
        ('FRASES-COMPROMISO', 1, 'GRACIAS A MI DESEMPEÑO COTIDIANO MIS COMPAÑEROS DE TRABAJO CONFÍAN EN MÍ.'),
        ('FRASES-COMPROMISO', 2, 'ME GUSTA COMPROMETERME CON LOS PROYECTOS QUE ME ASIGNAN EN EL TRABAJO.'),
        ('FRASES-COMPROMISO', 3, 'LA INFORMACIÓN QUE SE GENERA EN MI PUESTO LA RESGUARDO PARA EL MEJOR USO POR PARTE DE LA EMPRESA.'),
        ('FRASES-COMPROMISO', 4, 'SOY PARTIDARIO DE AYUDAR A MIS COMPAÑEROS DE TRABAJO, CON TAL DE BENEFICIAR LOS INTERESES LABORALES.'),
        ('FRASES-COMPROMISO', 5, 'CUANDO ME ASIGNAN PROYECTOS ESTRATÉGICOS PARA LA EMPRESA, ME HAGO CARGO DE ELLOS TOTALMENTE.'),
        ('OBJETIVO-ARQUITECTURA-CAPAS', 1, 'QUE CADA CAPA TENGA UNA RESPONSABILIDAD CLARA Y EL SISTEMA QUEDE POCO ACOPLADO'),
        ('OBJETIVO-ARQUITECTURA-CAPAS', 2, 'QUE EL SISTEMA SE EJECUTE MÁS RÁPIDO'),
        ('OBJETIVO-ARQUITECTURA-CAPAS', 3, 'EVITAR EL USO DE UNA BASE DE DATOS'),
        ('OBJETIVO-ARQUITECTURA-CAPAS', 4, 'REDUCIR LA CANTIDAD DE CLASES DEL SISTEMA'),
        ('PROPIEDADES-ACID', 1, 'QUE SE EJECUTEN TODAS SUS OPERACIONES O NINGUNA'),
        ('PROPIEDADES-ACID', 2, 'QUE OTRAS TRANSACCIONES NO VEAN SUS CAMBIOS HASTA QUE TERMINE'),
        ('PROPIEDADES-ACID', 3, 'QUE SUS CAMBIOS PERDUREN AUNQUE EL SISTEMA FALLE'),
        ('PROPIEDADES-ACID', 4, 'QUE LOS DATOS CUMPLAN LAS REGLAS DE INTEGRIDAD DEFINIDAS')
) AS v(op_nombre, nro_orden, descripcion)
JOIN opcion_respuesta o ON o.nombre = v.op_nombre;

-- ----------------------------------------------------------------------------
-- Preguntas: 3 por factor, así el sorteo de 2 preguntas por factor (CU026) da
-- cuestionarios distintos. Cada pregunta se responde con su opción de respuesta.
-- Los códigos siguen el formato que genera el sistema: PREG-<factor>-<número>.
-- ----------------------------------------------------------------------------
INSERT INTO pregunta (factor_id, opcion_respuesta_id, codigo, nombre, enunciado, descripcion, activa)
SELECT f.id, o.id, v.codigo, v.nombre, v.enunciado, v.descripcion, TRUE
FROM (
    VALUES
        ('FAC-GP-01', 'VERDADERO-FALSO', 'PREG-GP-01-01', 'WBS Y DEFINICION DEL ALCANCE',
         '¿LA DESCOMPOSICIÓN DE LA ESTRUCTURA DE TRABAJO (WBS) FORMA PARTE DE LA DEFINICIÓN DEL ALCANCE?', 'CONOCIMIENTO SOBRE LA WBS'),
        ('FAC-GP-01', 'VERDADERO-FALSO', 'PREG-GP-01-02', 'LINEA BASE DE REQUERIMIENTOS',
         '¿CUANDO SE DEFINE UN ALCANCE, DEBE DEFINIRSE UNA LÍNEA BASE DE LOS REQUERIMIENTOS?', 'CONTROL DE CAMBIOS DEL ALCANCE'),
        ('FAC-GP-01', 'ESCALA-VALORACION-5', 'PREG-GP-01-03', 'EXPERIENCIA EN GESTION DEL ALCANCE',
         '¿CÓMO CALIFICARÍAS TU EXPERIENCIA DEFINIENDO Y CONTROLANDO EL ALCANCE DE UN PROYECTO?', 'AUTOEVALUACIÓN'),
        ('FAC-GP-02', 'TIPO-DEPENDENCIA-CRONOGRAMA', 'PREG-GP-02-01', 'DEPENDENCIA FIN A COMIENZO',
         'EN EL CRONOGRAMA DE UN PROYECTO, ¿QUÉ SIGNIFICA QUE UNA ACTIVIDAD SEA DEL TIPO "FIN A COMIENZO"?', 'DEPENDENCIAS ENTRE ACTIVIDADES'),
        ('FAC-GP-02', 'VERDADERO-FALSO', 'PREG-GP-02-02', 'METODOLOGIA DE VALOR GANADO',
         '¿LA METODOLOGÍA DE "VALOR GANADO" PERMITE PREDECIR CUÁNTO DINERO SE GASTARÁ EN UN PROYECTO SEGÚN LOS AVANCES QUE TENGA A LA FECHA?', 'GESTIÓN DEL VALOR GANADO (EVM)'),
        ('FAC-GP-02', 'RUTA-CRITICA', 'PREG-GP-02-03', 'RUTA CRITICA',
         '¿QUÉ REPRESENTA LA RUTA CRÍTICA DE UN PROYECTO?', 'PLANIFICACIÓN DEL CRONOGRAMA'),
        ('FAC-LE-01', 'FRASES-CREDIBILIDAD', 'PREG-LE-01-01', 'FRASE DE CREDIBILIDAD',
         '¿CON CUÁL DE LAS SIGUIENTES FRASES TE IDENTIFICAS MÁS EN TU VIDA PROFESIONAL?', 'IDENTIFICACIÓN CON CONDUCTAS DE CREDIBILIDAD'),
        ('FAC-LE-01', 'FRECUENCIA-5', 'PREG-LE-01-02', 'CUMPLIMIENTO DE COMPROMISOS',
         '¿CON QUÉ FRECUENCIA CUMPLES EN TIEMPO Y FORMA LOS COMPROMISOS QUE ASUMES EN EL TRABAJO?', 'COHERENCIA ENTRE LO QUE SE DICE Y LO QUE SE HACE'),
        ('FAC-LE-01', 'FRECUENCIA-5', 'PREG-LE-01-03', 'CONFIANZA DE LOS COMPAÑEROS',
         '¿CON QUÉ FRECUENCIA TUS COMPAÑEROS TE CONFÍAN TAREAS O INFORMACIÓN IMPORTANTE?', 'CONFIANZA QUE GENERA EN EL EQUIPO'),
        ('FAC-LE-02', 'FRASES-COMPROMISO', 'PREG-LE-02-01', 'IMPORTANCIA DE LOS OBJETIVOS COLECTIVOS',
         'PARA MÍ EL LOGRO DE OBJETIVOS COLECTIVOS ES IMPORTANTE PORQUE:', 'MOTIVACIÓN POR EL LOGRO COLECTIVO'),
        ('FAC-LE-02', 'FRECUENCIA-5', 'PREG-LE-02-02', 'RESGUARDO DE LA INFORMACION',
         '¿CON QUÉ FRECUENCIA RESGUARDAS LA INFORMACIÓN QUE SE GENERA EN TU PUESTO PARA EL USO EXCLUSIVO DE LA EMPRESA?', 'CONFIDENCIALIDAD'),
        ('FAC-LE-02', 'FRECUENCIA-5', 'PREG-LE-02-03', 'AYUDA ANTE OBJETIVOS EN RIESGO',
         '¿CON QUÉ FRECUENCIA AYUDAS A TUS COMPAÑEROS CUANDO UN OBJETIVO DEL EQUIPO ESTÁ EN RIESGO?', 'COMPROMISO CON EL EQUIPO'),
        ('FAC-AR-01', 'OBJETIVO-ARQUITECTURA-CAPAS', 'PREG-AR-01-01', 'OBJETIVO DE LA ARQUITECTURA EN CAPAS',
         '¿CUÁL ES EL PRINCIPAL OBJETIVO DE SEPARAR UN SISTEMA EN CAPAS?', 'SEPARACIÓN DE RESPONSABILIDADES'),
        ('FAC-AR-01', 'VERDADERO-FALSO', 'PREG-AR-01-02', 'INVERSION DE DEPENDENCIAS',
         '¿EL PRINCIPIO DE INVERSIÓN DE DEPENDENCIAS PERMITE DESACOPLAR LOS MÓDULOS DE ALTO NIVEL DE LOS DETALLES DE IMPLEMENTACIÓN?', 'PRINCIPIOS SOLID'),
        ('FAC-AR-01', 'ESCALA-VALORACION-5', 'PREG-AR-01-03', 'DOMINIO DE PATRONES DE DISEÑO',
         '¿CÓMO CALIFICARÍAS TU DOMINIO DE LOS PATRONES DE DISEÑO (DAO, FACHADA, ESTRATEGIA)?', 'AUTOEVALUACIÓN'),
        ('FAC-AR-02', 'PROPIEDADES-ACID', 'PREG-AR-02-01', 'ATOMICIDAD DE TRANSACCIONES',
         '¿QUÉ GARANTIZA LA ATOMICIDAD DE UNA TRANSACCIÓN?', 'PROPIEDADES ACID'),
        ('FAC-AR-02', 'VERDADERO-FALSO', 'PREG-AR-02-02', 'PRUEBAS UNITARIAS AISLADAS',
         '¿LAS PRUEBAS UNITARIAS DEBEN AISLAR LAS DEPENDENCIAS EXTERNAS MEDIANTE DOBLES DE PRUEBA (MOCKS)?', 'TESTING'),
        ('FAC-AR-02', 'ESCALA-VALORACION-5', 'PREG-AR-02-03', 'EXPERIENCIA EN MODELADO RELACIONAL',
         '¿CÓMO CALIFICARÍAS TU EXPERIENCIA EN MODELADO RELACIONAL Y NORMALIZACIÓN?', 'AUTOEVALUACIÓN')
) AS v(fac_codigo, op_nombre, codigo, nombre, enunciado, descripcion)
JOIN factor f ON f.codigo = v.fac_codigo
JOIN opcion_respuesta o ON o.nombre = v.op_nombre;

-- ----------------------------------------------------------------------------
-- Ponderaciones: las de cada pregunta suman exactamente 10 y son 0 o positivas.
--   Preguntas de conocimiento: la opción correcta vale 10 y el resto 0.
--   Valoración y frecuencia: 0, 1, 2, 3 y 4 puntos de menor a mayor.
--   Frases de LEALTAD: más puntos a la frase que mejor expresa el factor.
-- Se identifica cada opción por su orden de visualización.
-- ----------------------------------------------------------------------------
INSERT INTO ponderacion_opcion_pregunta (pregunta_id, detalle_opcion_respuesta_id, ponderacion)
SELECT p.id, d.id, v.pond
FROM (
    VALUES
        ('PREG-GP-01-01', 1, 10.00), ('PREG-GP-01-01', 2, 0.00),
        ('PREG-GP-01-02', 1, 10.00), ('PREG-GP-01-02', 2, 0.00),
        ('PREG-GP-01-03', 1, 0.00), ('PREG-GP-01-03', 2, 1.00), ('PREG-GP-01-03', 3, 2.00), ('PREG-GP-01-03', 4, 3.00), ('PREG-GP-01-03', 5, 4.00),
        ('PREG-GP-02-01', 1, 10.00), ('PREG-GP-02-01', 2, 0.00), ('PREG-GP-02-01', 3, 0.00), ('PREG-GP-02-01', 4, 0.00),
        ('PREG-GP-02-02', 1, 10.00), ('PREG-GP-02-02', 2, 0.00),
        ('PREG-GP-02-03', 1, 10.00), ('PREG-GP-02-03', 2, 0.00), ('PREG-GP-02-03', 3, 0.00), ('PREG-GP-02-03', 4, 0.00),
        ('PREG-LE-01-01', 1, 4.00), ('PREG-LE-01-01', 2, 1.50), ('PREG-LE-01-01', 3, 1.00), ('PREG-LE-01-01', 4, 1.00), ('PREG-LE-01-01', 5, 2.50),
        ('PREG-LE-01-02', 1, 0.00), ('PREG-LE-01-02', 2, 1.00), ('PREG-LE-01-02', 3, 2.00), ('PREG-LE-01-02', 4, 3.00), ('PREG-LE-01-02', 5, 4.00),
        ('PREG-LE-01-03', 1, 0.00), ('PREG-LE-01-03', 2, 1.00), ('PREG-LE-01-03', 3, 2.00), ('PREG-LE-01-03', 4, 3.00), ('PREG-LE-01-03', 5, 4.00),
        ('PREG-LE-02-01', 1, 2.00), ('PREG-LE-02-01', 2, 3.00), ('PREG-LE-02-01', 3, 1.00), ('PREG-LE-02-01', 4, 1.00), ('PREG-LE-02-01', 5, 3.00),
        ('PREG-LE-02-02', 1, 0.00), ('PREG-LE-02-02', 2, 1.00), ('PREG-LE-02-02', 3, 2.00), ('PREG-LE-02-02', 4, 3.00), ('PREG-LE-02-02', 5, 4.00),
        ('PREG-LE-02-03', 1, 0.00), ('PREG-LE-02-03', 2, 1.00), ('PREG-LE-02-03', 3, 2.00), ('PREG-LE-02-03', 4, 3.00), ('PREG-LE-02-03', 5, 4.00),
        ('PREG-AR-01-01', 1, 10.00), ('PREG-AR-01-01', 2, 0.00), ('PREG-AR-01-01', 3, 0.00), ('PREG-AR-01-01', 4, 0.00),
        ('PREG-AR-01-02', 1, 10.00), ('PREG-AR-01-02', 2, 0.00),
        ('PREG-AR-01-03', 1, 0.00), ('PREG-AR-01-03', 2, 1.00), ('PREG-AR-01-03', 3, 2.00), ('PREG-AR-01-03', 4, 3.00), ('PREG-AR-01-03', 5, 4.00),
        ('PREG-AR-02-01', 1, 10.00), ('PREG-AR-02-01', 2, 0.00), ('PREG-AR-02-01', 3, 0.00), ('PREG-AR-02-01', 4, 0.00),
        ('PREG-AR-02-02', 1, 10.00), ('PREG-AR-02-02', 2, 0.00),
        ('PREG-AR-02-03', 1, 0.00), ('PREG-AR-02-03', 2, 1.00), ('PREG-AR-02-03', 3, 2.00), ('PREG-AR-02-03', 4, 3.00), ('PREG-AR-02-03', 5, 4.00)
) AS v(preg_codigo, orden, pond)
JOIN pregunta p ON p.codigo = v.preg_codigo
JOIN detalle_opcion_respuesta d ON d.opcion_respuesta_id = p.opcion_respuesta_id AND d.nro_orden = v.orden;

-- Puestos
INSERT INTO puesto (codigo, nombre, empresa, descripcion, activo) VALUES
    ('PUE-PM-01', 'PROJECT MANAGER TIC', 'TECNOLOGIA DEL SUR S.A.', 'LIDERAZGO DE PROYECTOS DE SOFTWARE Y METODOLOGIAS AGILES/TRADICIONALES', TRUE),
    ('PUE-SR-02', 'ARQUITECTO DE SOFTWARE SR', 'FINTECH LITORAL S.R.L.', 'DISEÑO DE ARQUITECTURAS DISTRIBUIDAS, APIS Y PERSISTENCIA', TRUE);

INSERT INTO puesto_competencia (puesto_id, competencia_id, ponderacion_minima)
SELECT p.id, c.id, v.pond_min
FROM (
    VALUES
        ('PUE-PM-01', 'COMP-GP', 3.00),
        ('PUE-PM-01', 'COMP-LE', 3.00),
        ('PUE-SR-02', 'COMP-AR', 3.50),
        ('PUE-SR-02', 'COMP-LE', 3.00)
) AS v(pue_codigo, comp_codigo, pond_min)
JOIN puesto p ON p.codigo = v.pue_codigo
JOIN competencia c ON c.codigo = v.comp_codigo;

-- ----------------------------------------------------------------------------
-- Evaluación EVA-001 del puesto PUE-PM-01, generada hace 2 días (CU025).
-- Al generarla se congela el modelo: se copian las preguntas activas de los
-- factores (con al menos 2 preguntas) de cada competencia del puesto, con la
-- ponderación de cada opción. Los cuestionarios se arman con esa copia.
-- ----------------------------------------------------------------------------
INSERT INTO evaluacion (puesto_id, nro_evaluacion, fecha_creacion)
SELECT id, 1, CURRENT_TIMESTAMP - INTERVAL '2 days'
FROM puesto
WHERE codigo = 'PUE-PM-01';

INSERT INTO pregunta_evaluacion (evaluacion_id, factor_id, nombre, enunciado)
SELECT e.id, f.id, p.nombre, p.enunciado
FROM evaluacion e
JOIN puesto_competencia pc ON pc.puesto_id = e.puesto_id
JOIN factor f ON f.competencia_id = pc.competencia_id AND f.activo
JOIN pregunta p ON p.factor_id = f.id AND p.activa
WHERE (SELECT COUNT(*) FROM pregunta p2 WHERE p2.factor_id = f.id AND p2.activa) >= 2
ORDER BY f.competencia_id, f.nro_orden, p.id;

INSERT INTO ponderacion_evaluacion (pregunta_evaluacion_id, detalle_opcion_respuesta_id, ponderacion)
SELECT pe.id, pop.detalle_opcion_respuesta_id, pop.ponderacion
FROM pregunta_evaluacion pe
JOIN pregunta p ON p.factor_id = pe.factor_id AND p.nombre = pe.nombre
JOIN ponderacion_opcion_pregunta pop ON pop.pregunta_id = p.id;

-- ----------------------------------------------------------------------------
-- Cuestionarios de la evaluación EVA-001:
--   CAND-001 PEREZ:     activo, para completarlo como candidato (clave A8K3M9P2).
--   CAND-002 SOSA:      completo. GP 5,75 / LE 3,75 -> 4,75 (alcanza los mínimos).
--   CAND-003 GUTIERREZ: completo. GP 5,25 / LE 1,25 -> 3,25 (no alcanza el de LEALTAD).
-- ----------------------------------------------------------------------------
INSERT INTO cuestionario (
    candidato_id, evaluacion_id, clave_acceso, estado, bloque_actual, cantidad_accesos,
    fecha_creacion, fecha_inicio, fecha_ultimo_acceso, fecha_fin, puntaje_total
)
SELECT
    cand.id,
    e.id,
    v.clave,
    v.estado,
    v.bloque_actual,
    v.accesos,
    e.fecha_creacion,
    v.f_inicio,
    v.f_ultimo,
    v.f_fin,
    v.puntaje
FROM (
    VALUES
        ('CAND-001', 'PUE-PM-01', 'A8K3M9P2', 'ACTIVO',   1, 0, NULL::TIMESTAMP, NULL::TIMESTAMP, NULL::TIMESTAMP, NULL::NUMERIC),
        ('CAND-002', 'PUE-PM-01', 'X4R7T2W9', 'COMPLETO', 4, 1, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '23 hours', CURRENT_TIMESTAMP - INTERVAL '23 hours', 4.75::NUMERIC),
        ('CAND-003', 'PUE-PM-01', 'B6N2Q8L5', 'COMPLETO', 4, 1, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '22 hours', CURRENT_TIMESTAMP - INTERVAL '22 hours', 3.25::NUMERIC)
) AS v(nro_cand, cod_pue, clave, estado, bloque_actual, accesos, f_inicio, f_ultimo, f_fin, puntaje)
JOIN candidato cand ON cand.nro_candidato = v.nro_cand
JOIN puesto pue ON pue.codigo = v.cod_pue
JOIN evaluacion e ON e.puesto_id = pue.id AND e.nro_evaluacion = 1;

-- Preguntas de los cuestionarios completos (tomadas del modelo de la evaluación): 2 por factor,
-- en bloques de 2 (preguntas_por_bloque = 2).
INSERT INTO pregunta_cuestionario (
    cuestionario_id, pregunta_evaluacion_id, nro_bloque, orden_visual, respondida
)
SELECT q.id, pe.id, v.nro_bloque, v.orden_visual, TRUE
FROM (
    VALUES
        ('CAND-002', 'PREG-GP-01-01', 1, 1), ('CAND-002', 'PREG-LE-02-01', 1, 2),
        ('CAND-002', 'PREG-GP-02-02', 2, 3), ('CAND-002', 'PREG-LE-01-01', 2, 4),
        ('CAND-002', 'PREG-GP-01-03', 3, 5), ('CAND-002', 'PREG-LE-02-03', 3, 6),
        ('CAND-002', 'PREG-GP-02-01', 4, 7), ('CAND-002', 'PREG-LE-01-02', 4, 8),
        ('CAND-003', 'PREG-LE-01-03', 1, 1), ('CAND-003', 'PREG-GP-02-01', 1, 2),
        ('CAND-003', 'PREG-LE-02-02', 2, 3), ('CAND-003', 'PREG-GP-01-02', 2, 4),
        ('CAND-003', 'PREG-GP-02-03', 3, 5), ('CAND-003', 'PREG-LE-01-01', 3, 6),
        ('CAND-003', 'PREG-LE-02-01', 4, 7), ('CAND-003', 'PREG-GP-01-03', 4, 8)
) AS v(nro_cand, preg_codigo, nro_bloque, orden_visual)
JOIN candidato cand ON cand.nro_candidato = v.nro_cand
JOIN cuestionario q ON q.candidato_id = cand.id AND q.estado = 'COMPLETO'
JOIN pregunta p ON p.codigo = v.preg_codigo
JOIN pregunta_evaluacion pe ON pe.evaluacion_id = q.evaluacion_id AND pe.factor_id = p.factor_id AND pe.nombre = p.nombre;

-- Respuestas elegidas, por orden de visualización de la opción. La ponderación obtenida es la del
-- modelo de la evaluación.
INSERT INTO respuesta_cuestionario (
    pregunta_cuestionario_id, detalle_opcion_respuesta_id, ponderacion_obtenida, respondida_en
)
SELECT pq.id, d.id, pv.ponderacion, q.fecha_fin
FROM (
    VALUES
        -- SOSA
        ('CAND-002', 'PREG-GP-01-01', 1),  -- VERDADERO                    10
        ('CAND-002', 'PREG-GP-01-03', 4),  -- MUY BUENO                     3  -> alcance 6,50
        ('CAND-002', 'PREG-GP-02-01', 1),  -- respuesta correcta           10
        ('CAND-002', 'PREG-GP-02-02', 2),  -- FALSO                         0  -> tiempo 5,00
        ('CAND-002', 'PREG-LE-01-01', 1),  -- "SOY DIGNO DE CONFIANZA..."   4
        ('CAND-002', 'PREG-LE-01-02', 5),  -- SIEMPRE                       4  -> credibilidad 4,00
        ('CAND-002', 'PREG-LE-02-01', 5),  -- "CUANDO ME ASIGNAN..."        3
        ('CAND-002', 'PREG-LE-02-03', 5),  -- SIEMPRE                       4  -> compromiso 3,50
        -- GUTIERREZ
        ('CAND-003', 'PREG-GP-01-02', 1),  -- VERDADERO                    10
        ('CAND-003', 'PREG-GP-01-03', 2),  -- REGULAR                       1  -> alcance 5,50
        ('CAND-003', 'PREG-GP-02-01', 1),  -- respuesta correcta           10
        ('CAND-003', 'PREG-GP-02-03', 3),  -- respuesta incorrecta          0  -> tiempo 5,00
        ('CAND-003', 'PREG-LE-01-01', 4),  -- "SOY SOLIDARIO..."            1
        ('CAND-003', 'PREG-LE-01-03', 3),  -- A VECES                       2  -> credibilidad 1,50
        ('CAND-003', 'PREG-LE-02-01', 3),  -- "LA INFORMACIÓN QUE..."       1
        ('CAND-003', 'PREG-LE-02-02', 2)   -- CASI NUNCA                    1  -> compromiso 1,00
) AS v(nro_cand, preg_codigo, orden)
JOIN candidato cand ON cand.nro_candidato = v.nro_cand
JOIN cuestionario q ON q.candidato_id = cand.id AND q.estado = 'COMPLETO'
JOIN pregunta p ON p.codigo = v.preg_codigo
JOIN pregunta_evaluacion pe ON pe.evaluacion_id = q.evaluacion_id AND pe.factor_id = p.factor_id AND pe.nombre = p.nombre
JOIN pregunta_cuestionario pq ON pq.cuestionario_id = q.id AND pq.pregunta_evaluacion_id = pe.id
JOIN detalle_opcion_respuesta d ON d.opcion_respuesta_id = p.opcion_respuesta_id AND d.nro_orden = v.orden
JOIN ponderacion_evaluacion pv ON pv.pregunta_evaluacion_id = pe.id AND pv.detalle_opcion_respuesta_id = d.id;
