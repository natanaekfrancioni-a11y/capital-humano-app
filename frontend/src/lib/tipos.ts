// Tipos de los JSON que devuelve la API (Spring Boot + Jackson).
// Las fechas llegan como texto ISO sin zona horaria ("2026-11-24T21:03:57.123").

export type EstadoCuestionario = 'ACTIVO' | 'EN_PROCESO' | 'COMPLETO' | 'INCOMPLETO' | 'SIN_CONTESTAR';

export interface Candidato {
  id: number;
  nroCandidato: string;
  apellido: string;
  nombre: string;
  tipoDocumento: string;
  nroDocumento: string;
  fechaNacimiento?: string;
  genero?: string;
  nacionalidad?: string;
  escolaridad?: string;
  email?: string;
  eliminado?: boolean;
}

export interface Competencia {
  id: number;
  codigo: string;
  nombre: string;
  descripcion: string;
  tipo: string;
  activa: boolean;
}

export interface Factor {
  id: number;
  competencia: Competencia;
  codigo: string;
  nombre: string;
  descripcion: string;
  nroOrden: number;
  activo: boolean;
}

export interface DetalleOpcion {
  id: number;
  nroOrden: number;
  descripcion: string;
}

export interface OpcionRespuesta {
  id: number;
  nombre: string;
  descripcion: string;
  activa: boolean;
  detalles: DetalleOpcion[];
}

export interface Pregunta {
  id: number;
  factor: Factor;
  opcionRespuesta: OpcionRespuesta;
  codigo: string;
  nombre: string;
  enunciado: string;
  descripcion: string | null;
  activa: boolean;
  /** clave: id del ítem de la opción de respuesta; valor: ponderación */
  ponderacionesPorDetalle: Record<string, number>;
}

/** CU033: datos que propone la IA para completar el alta de una pregunta. */
export interface PreguntaGenerada {
  competenciaId: number;
  factorId: number;
  nombre: string;
  enunciado: string;
  descripcion: string | null;
  opcionRespuestaId: number;
  /** clave: id del ítem de la opción de respuesta. Vacío si la IA no propuso ponderaciones válidas. */
  ponderaciones: Record<string, number>;
}

export interface PuestoCompetencia {
  id: number;
  competencia: Competencia;
  ponderacionMinima: number;
}

export interface Puesto {
  id: number;
  codigo: string;
  nombre: string;
  empresa: string;
  descripcion: string;
  activo: boolean;
  competencias: PuestoCompetencia[];
}

/**
 * CU026: pregunta del cuestionario tal como la ve el candidato. Es la copia que guardó la evaluación al
 * generarse (modelo congelado del CU025): texto y opciones de ese momento, sin las ponderaciones.
 */
export interface PreguntaDelCuestionario {
  id: number;
  nombre: string;
  enunciado: string;
  opcionRespuesta: { detalles: DetalleOpcion[] };
}

export interface PreguntaCuestionario {
  id: number;
  pregunta: PreguntaDelCuestionario;
  nroBloque: number;
  ordenVisual: number;
  respondida: boolean;
}

/** CU025: evaluación a la que pertenece el cuestionario (EVA-001, EVA-002, ... dentro del puesto). */
export interface EvaluacionDelCuestionario {
  id: number;
  nroEvaluacion: number;
  codigo: string;
  fechaCreacion: string;
}

export interface Cuestionario {
  id: number;
  candidato: Candidato;
  evaluacion: EvaluacionDelCuestionario;
  puesto: Puesto;
  claveAcceso: string;
  estado: EstadoCuestionario;
  bloqueActual: number;
  cantidadAccesos: number;
  fechaCreacion: string;
  fechaInicio: string | null;
  fechaUltimoAcceso: string | null;
  fechaFin: string | null;
  puntajeTotal: number | null;
  preguntas: PreguntaCuestionario[];
}

export interface Configuracion {
  preguntasPorBloque: number;
  maxAccesos: number;
  tiempoMaxMinutos: number;
  diasVencimientoActivo: number;
  instrucciones: string;
}

// CU025
export interface CompetenciaPuesto {
  competenciaId: number;
  codigo: string;
  nombre: string;
  ponderacionMinima: number;
  evaluable: boolean;
}

export interface CandidatoClave {
  candidatoId: number;
  nroCandidato: string;
  apellido: string;
  nombre: string;
  tipoDocumento: string;
  nroDocumento: string;
  claveIngreso: string;
}

// CU028
export interface PuestoResumen {
  id: number;
  codigo: string;
  nombre: string;
  empresa: string;
  candidatos: number;
  evaluacionesCompletas: number;
}

export interface Evaluacion {
  codigo: string;
  fechaGeneracion: string;
  candidatos: number;
  completos: number;
}

export interface EvaluacionesPuesto {
  puestoId: number;
  puestoCodigo: string;
  puestoNombre: string;
  totalCandidatos: number;
  totalCompletos: number;
  evaluaciones: Evaluacion[];
}

export interface FilaOrdenMerito {
  posicion: number | null;
  cuestionarioId: number;
  tipoDocumento: string;
  nroDocumento: string;
  apellido: string;
  nombre: string;
  estado: EstadoCuestionario;
  puntaje: number | null;
  fechaInicio: string | null;
  fechaFin: string | null;
  fechaUltimoAcceso: string | null;
  cantidadAccesos: number;
}

export interface ReporteOrdenMerito {
  puestoCodigo: string;
  puestoNombre: string;
  empresa: string;
  evaluacion: string;
  fechaEmision: string;
  emitidoPor: string;
  competencias: { nombre: string; ponderacionMinima: number }[];
  ordenMerito: FilaOrdenMerito[];
  noAlcanzanMinimo: FilaOrdenMerito[];
  restantes: FilaOrdenMerito[];
}

// CU001
export interface SesionConsultor {
  perfil: 'CONSULTOR';
  username: string;
  nombreCompleto: string;
}

export interface SesionCandidato {
  perfil: 'CANDIDATO';
  candidatoId: number;
  candidatoNombre: string;
  cuestionarioId: number;
  estado: EstadoCuestionario;
}
