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

export interface PreguntaCuestionario {
  id: number;
  pregunta: Pregunta;
  factor: Factor;
  competencia: Competencia;
  nroBloque: number;
  ordenVisual: number;
  respondida: boolean;
}

export interface Cuestionario {
  id: number;
  candidato: Candidato;
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

export interface EnUso {
  enUso: boolean;
  mensaje: string;
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

// CU027
export interface CandidatoEvaluado {
  cuestionarioId: number;
  candidatoId: number;
  nroCandidato: string;
  apellido: string;
  nombre: string;
  tipoDocumento: string;
  nroDocumento: string;
  fechaFin: string | null;
}

export interface ItemComparativo {
  competencia: string;
  requerido: number;
  obtenido: number;
  diferencia: number;
  cumple: boolean;
}

export interface ReporteComparativo {
  puestoCodigo: string;
  puestoNombre: string;
  empresa: string;
  apellido: string;
  nombre: string;
  tipoDocumento: string;
  nroDocumento: string;
  nroCandidato: string;
  fechaFin: string | null;
  puntajeTotal: number;
  competencias: ItemComparativo[];
}

// CU006
export interface ResumenImportacion {
  creados: number;
  actualizados: number;
  eliminados: number;
  totalProcesados: number;
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

// CU007 - Exportar resultados
export interface PuestoExportable {
  id: number;
  codigo: string;
  nombre: string;
  empresa: string;
  /** Candidatos evaluados (cuestionario completo), dentro del período si se indicó. */
  evaluados: number;
}

export interface CandidatoExportable {
  cuestionarioId: number;
  nroCandidato: string;
  apellido: string;
  nombre: string;
  tipoDocumento: string;
  nroDocumento: string;
  fechaFin: string;
}

export interface ArchivoExportado {
  nombreArchivo: string;
  contenido: string;
  filas: number;
}
