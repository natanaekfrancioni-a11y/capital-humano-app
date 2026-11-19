-- ============================================================================
-- Capit@l Humano - Datos Iniciales de Prueba (Seed)
-- ============================================================================

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
    'LEA ATENTAMENTE CADA PREGUNTA Y SELECCIONE UNA UNICA OPCION. LAS PREGUNTAS SE PRESENTAN EN BLOQUES; UNA VEZ AVANZADO UN BLOQUE NO PODRA VOLVER A MODIFICARLO NI REVISAR SUS RESPUESTAS ANTERIORES.'
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

-- Competencias
INSERT INTO competencia (codigo, nombre, descripcion, tipo, activa) VALUES
    ('COMP-GP', 'GESTION DE PROYECTOS', 'CAPACIDAD PARA PLANIFICAR, EJECUTAR Y CONTROLAR PROYECTOS TIC', 'TECNICA', TRUE),
    ('COMP-LE', 'LEALTAD', 'COMPROMISO, INTEGRIDAD Y RESGUARDO DE INTERESES ORGANIZACIONALES', 'ACTITUDINAL', TRUE),
    ('COMP-AR', 'ARQUITECTURA DE SOFTWARE', 'DISENO DE SISTEMAS ESCALABLES, MANTENIBLES Y SEGURIDAD', 'TECNICA', TRUE);

-- Factores (>= 2 por competencia para poder evaluar)
INSERT INTO factor (competencia_id, codigo, nombre, descripcion, nro_orden, activo)
SELECT c.id, v.codigo, v.nombre, v.descripcion, v.nro_orden, TRUE
FROM (
    VALUES
        ('COMP-GP', 'FAC-GP-01', 'GESTION DEL ALCANCE', 'DEFINICION DE WBS Y LINEA BASE DE REQUERIMIENTOS', 1),
        ('COMP-GP', 'FAC-GP-02', 'GESTION DEL TIEMPO', 'CRONOGRAMAS, DEPENDENCIAS Y VALOR GANADO', 2),
        ('COMP-LE', 'FAC-LE-01', 'CREDIBILIDAD', 'COHERENCIA ENTRE ACCIONES Y COMPROMISOS PROFESIONALES', 1),
        ('COMP-LE', 'FAC-LE-02', 'COMPROMISO', 'ORIENTACION AL LOGRO COLECTIVO Y RESPONSABILIDAD', 2),
        ('COMP-AR', 'FAC-AR-01', 'DISENO EN CAPAS', 'SEPARACION DE RESPONSABILIDADES Y BAJO ACOPLAMIENTO', 1),
        ('COMP-AR', 'FAC-AR-02', 'PERSISTENCIA Y CALIDAD', 'MODELADO RELACIONAL, TRANSACCIONES Y PRUEBAS', 2)
) AS v(comp_codigo, codigo, nombre, descripcion, nro_orden)
JOIN competencia c ON c.codigo = v.comp_codigo;

-- Escalas / Opciones de respuesta
INSERT INTO opcion_respuesta (nombre, descripcion, activa) VALUES
    ('ESCALA-VALORACION-5', 'ESCALA DE CINCO NIVELES DE VALORACION', TRUE),
    ('VERDADERO-FALSO', 'RESPUESTA BINARIA VERDADERO O FALSO', TRUE);

INSERT INTO detalle_opcion_respuesta (opcion_respuesta_id, nro_orden, descripcion)
SELECT o.id, v.nro_orden, v.descripcion
FROM (
    VALUES
        ('ESCALA-VALORACION-5', 1, 'MALO'),
        ('ESCALA-VALORACION-5', 2, 'REGULAR'),
        ('ESCALA-VALORACION-5', 3, 'BUENO'),
        ('ESCALA-VALORACION-5', 4, 'MUY BUENO'),
        ('ESCALA-VALORACION-5', 5, 'EXCELENTE'),
        ('VERDADERO-FALSO', 1, 'VERDADERO'),
        ('VERDADERO-FALSO', 2, 'FALSO')
) AS v(op_nombre, nro_orden, descripcion)
JOIN opcion_respuesta o ON o.nombre = v.op_nombre;

-- Preguntas (>= 2 por factor para que el sorteo aleatorio de 2 preguntas por factor funcione siempre)
INSERT INTO pregunta (factor_id, opcion_respuesta_id, codigo, nombre, enunciado, descripcion, activa)
SELECT f.id, o.id, v.codigo, v.nombre, v.enunciado, v.descripcion, v.activa
FROM (
    VALUES
        ('FAC-GP-01', 'ESCALA-VALORACION-5', 'PREG-GP-01', 'WBS Y ALCANCE', 'LA DESCOMPOSICION DE ESTRUCTURA DE TRABAJO (WBS) FORMA PARTE DE LA DEFINICION DEL ALCANCE?', 'ALCANCE DE PROYECTO', TRUE),
        ('FAC-GP-01', 'VERDADERO-FALSO', 'PREG-GP-02', 'LINEA BASE REQUERIMIENTOS', 'CUANDO SE DEFINE UN ALCANCE, DEBE DEFINIRSE UNA LINEA BASE DE LOS REQUERIMIENTOS?', 'CONTROL DE CAMBIOS', TRUE),
        ('FAC-GP-02', 'ESCALA-VALORACION-5', 'PREG-GP-03', 'RELACION FIN A COMIENZO', 'EN EL CRONOGRAMA DE UN PROYECTO, QUE SIGNIFICA QUE UNA ACTIVIDAD SEA DEL TIPO FIN A COMIENZO?', 'DEPENDENCIAS', TRUE),
        ('FAC-GP-02', 'VERDADERO-FALSO', 'PREG-GP-04', 'METODOLOGIA VALOR GANADO', 'LA METODOLOGIA DE VALOR GANADO PERMITE PREDECIR CUANTO DINERO SE GASTARA EN UN PROYECTO SEGUN SUS AVANCES?', 'EVM', TRUE),
        ('FAC-LE-01', 'ESCALA-VALORACION-5', 'PREG-LE-01', 'CONFIANZA POR HECHOS', 'CON CUAL DE ESTOS NIVELES REPRESENTA MEJOR SU CONFIABILIDAD PROFESIONAL MEDIANTE HECHOS CONCRETOS?', 'CREDIBILIDAD', TRUE),
        ('FAC-LE-01', 'ESCALA-VALORACION-5', 'PREG-LE-02', 'INTEGRIDAD LABORAL', 'COMO EVALUA SU CUMPLIMIENTO INTEGRO DE LO SOLICITADO EN EL AMBITO LABORAL?', 'INTEGRIDAD', TRUE),
        ('FAC-LE-02', 'ESCALA-VALORACION-5', 'PREG-LE-03', 'OBJETIVOS COLECTIVOS', 'COMO CALIFICA SU APORTE AL LOGRO DE OBJETIVOS COLECTIVOS DEL EQUIPO?', 'TRABAJO EN EQUIPO', TRUE),
        ('FAC-LE-02', 'VERDADERO-FALSO', 'PREG-LE-04', 'RESGUARDO DE INFORMACION', 'RESGUARDA LA INFORMACION GENERADA EN SU PUESTO EXCLUSIVAMENTE PARA EL USO LEGITIMO DE LA EMPRESA?', 'CONFIDENCIALIDAD', TRUE),
        ('FAC-AR-01', 'ESCALA-VALORACION-5', 'PREG-AR-01', 'SEPARACION DE CAPAS', 'QUE NIVEL DE DOMINIO TIENE SOBRE LA SEPARACION EN CAPAS Y RESPONSABILIDADES UNICAS?', 'ARQUITECTURA', TRUE),
        ('FAC-AR-01', 'VERDADERO-FALSO', 'PREG-AR-02', 'INVERSION DE DEPENDENCIAS', 'EL PRINCIPIO DE INVERSION DE DEPENDENCIAS PERMITE DESACOPLAR MODULOS DE ALTO NIVEL DE DETALLES?', 'SOLID', TRUE),
        ('FAC-AR-02', 'ESCALA-VALORACION-5', 'PREG-AR-03', 'INTEGRIDAD REFERENCIAL', 'COMO CALIFICA SU MANEJO DE RESTRICCIONES RELACIONALES Y TRANSACCIONES ACID?', 'BASE DE DATOS', TRUE),
        ('FAC-AR-02', 'VERDADERO-FALSO', 'PREG-AR-04', 'PRUEBAS UNITARIAS AISLADAS', 'LAS PRUEBAS UNITARIAS DEBEN AISLAR DEPENDENCIAS EXTERNAS MEDIANTE DOBLES DE PRUEBA O MOCKS?', 'TESTING', TRUE)
) AS v(fac_codigo, op_nombre, codigo, nombre, enunciado, descripcion, activa)
JOIN factor f ON f.codigo = v.fac_codigo
JOIN opcion_respuesta o ON o.nombre = v.op_nombre;

-- Ponderaciones por pregunta (cada pregunta suma exactamente 10.00 puntos entre sus opciones)
INSERT INTO ponderacion_opcion_pregunta (pregunta_id, detalle_opcion_respuesta_id, ponderacion)
SELECT p.id, d.id, v.pond
FROM (
    VALUES
        ('PREG-GP-01', 'MALO', 0.00), ('PREG-GP-01', 'REGULAR', 1.00), ('PREG-GP-01', 'BUENO', 2.00), ('PREG-GP-01', 'MUY BUENO', 3.00), ('PREG-GP-01', 'EXCELENTE', 4.00),
        ('PREG-GP-02', 'VERDADERO', 7.00), ('PREG-GP-02', 'FALSO', 3.00),
        ('PREG-GP-03', 'MALO', 0.00), ('PREG-GP-03', 'REGULAR', 1.00), ('PREG-GP-03', 'BUENO', 2.00), ('PREG-GP-03', 'MUY BUENO', 3.00), ('PREG-GP-03', 'EXCELENTE', 4.00),
        ('PREG-GP-04', 'VERDADERO', 8.00), ('PREG-GP-04', 'FALSO', 2.00),
        ('PREG-LE-01', 'MALO', 0.00), ('PREG-LE-01', 'REGULAR', 1.00), ('PREG-LE-01', 'BUENO', 2.00), ('PREG-LE-01', 'MUY BUENO', 3.00), ('PREG-LE-01', 'EXCELENTE', 4.00),
        ('PREG-LE-02', 'MALO', 0.00), ('PREG-LE-02', 'REGULAR', 1.00), ('PREG-LE-02', 'BUENO', 2.00), ('PREG-LE-02', 'MUY BUENO', 3.00), ('PREG-LE-02', 'EXCELENTE', 4.00),
        ('PREG-LE-03', 'MALO', 0.00), ('PREG-LE-03', 'REGULAR', 1.00), ('PREG-LE-03', 'BUENO', 2.00), ('PREG-LE-03', 'MUY BUENO', 3.00), ('PREG-LE-03', 'EXCELENTE', 4.00),
        ('PREG-LE-04', 'VERDADERO', 9.00), ('PREG-LE-04', 'FALSO', 1.00),
        ('PREG-AR-01', 'MALO', 0.00), ('PREG-AR-01', 'REGULAR', 1.00), ('PREG-AR-01', 'BUENO', 2.00), ('PREG-AR-01', 'MUY BUENO', 3.00), ('PREG-AR-01', 'EXCELENTE', 4.00),
        ('PREG-AR-02', 'VERDADERO', 7.50), ('PREG-AR-02', 'FALSO', 2.50),
        ('PREG-AR-03', 'MALO', 0.00), ('PREG-AR-03', 'REGULAR', 1.00), ('PREG-AR-03', 'BUENO', 2.00), ('PREG-AR-03', 'MUY BUENO', 3.00), ('PREG-AR-03', 'EXCELENTE', 4.00),
        ('PREG-AR-04', 'VERDADERO', 8.00), ('PREG-AR-04', 'FALSO', 2.00)
) AS v(preg_codigo, op_desc, pond)
JOIN pregunta p ON p.codigo = v.preg_codigo
JOIN opcion_respuesta o ON p.opcion_respuesta_id = o.id
JOIN detalle_opcion_respuesta d ON d.opcion_respuesta_id = o.id AND d.descripcion = v.op_desc;

-- Puestos
INSERT INTO puesto (codigo, nombre, empresa, descripcion, activo) VALUES
    ('PUE-PM-01', 'PROJECT MANAGER TIC', 'TECNOLOGIA DEL SUR S.A.', 'LIDERAZGO DE PROYECTOS DE SOFTWARE Y METODOLOGIAS AGILES/TRADICIONALES', TRUE),
    ('PUE-SR-02', 'ARQUITECTO DE SOFTWARE SR', 'FINTECH LITORAL S.R.L.', 'DISENO DE ARQUITECTURAS DISTRIBUIDAS, APIS Y PERSISTENCIA', TRUE);

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

-- Cuestionarios de ejemplo
INSERT INTO cuestionario (
    candidato_id, puesto_id, clave_acceso, estado, bloque_actual, cantidad_accesos,
    fecha_creacion, fecha_inicio, fecha_ultimo_acceso, fecha_fin, puntaje_total
)
SELECT
    cand.id,
    pue.id,
    v.clave,
    v.estado,
    v.bloque_actual,
    v.accesos,
    CURRENT_TIMESTAMP - INTERVAL '2 days',
    v.f_inicio,
    v.f_ultimo,
    v.f_fin,
    v.puntaje
FROM (
    VALUES
        ('CAND-001', 'PUE-PM-01', 'A8K3M9P2', 'ACTIVO',   1, 0, NULL::TIMESTAMP, NULL::TIMESTAMP, NULL::TIMESTAMP, NULL::NUMERIC),
        ('CAND-002', 'PUE-PM-01', 'X4R7T2W9', 'COMPLETO', 3, 1, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '23 hours', CURRENT_TIMESTAMP - INTERVAL '23 hours', 4.25::NUMERIC),
        ('CAND-003', 'PUE-PM-01', 'B6N2Q8L5', 'COMPLETO', 3, 1, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '22 hours', CURRENT_TIMESTAMP - INTERVAL '22 hours', 2.40::NUMERIC)
) AS v(nro_cand, cod_pue, clave, estado, bloque_actual, accesos, f_inicio, f_ultimo, f_fin, puntaje)
JOIN candidato cand ON cand.nro_candidato = v.nro_cand
JOIN puesto pue ON pue.codigo = v.cod_pue;
