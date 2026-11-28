import type {
  Candidato,
  CandidatoClave,
  Competencia,
  CompetenciaPuesto,
  Configuracion,
  Cuestionario,
  EvaluacionesPuesto,
  Factor,
  OpcionRespuesta,
  Pregunta,
  PreguntaGenerada,
  Puesto,
  PuestoResumen,
  ReporteOrdenMerito,
  SesionCandidato,
  SesionConsultor,
} from './tipos';

export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

/** error de la API: mensaje es el texto principal y detalles la lista que arma el backend */
export class ApiError extends Error {
  readonly detalles: string[];
  readonly status: number;
  readonly sinConexion: boolean;

  constructor(mensaje: string, detalles: string[] = [], status = 0, sinConexion = false) {
    super(mensaje);
    this.name = 'ApiError';
    this.detalles = detalles;
    this.status = status;
    this.sinConexion = sinConexion;
  }
}

type Consulta = Record<string, string | number | null | undefined>;

async function pedir<T>(metodo: string, ruta: string, opciones: { cuerpo?: unknown; consulta?: Consulta } = {}): Promise<T> {
  const params = new URLSearchParams();
  for (const [clave, valor] of Object.entries(opciones.consulta ?? {})) {
    if (valor !== null && valor !== undefined && String(valor).trim() !== '') {
      params.set(clave, String(valor).trim());
    }
  }
  const consulta = params.toString();
  const url = `${API_URL}${ruta}${consulta ? `?${consulta}` : ''}`;

  let res: Response;
  try {
    res = await fetch(url, {
      method: metodo,
      headers: opciones.cuerpo !== undefined ? { 'Content-Type': 'application/json' } : undefined,
      body: opciones.cuerpo !== undefined ? JSON.stringify(opciones.cuerpo) : undefined,
    });
  } catch {
    throw new ApiError(
      'No se pudo conectar con el servidor.',
      [`Verificá que el backend esté corriendo en ${API_URL}.`],
      0,
      true,
    );
  }

  if (res.status === 204) return undefined as T;

  const datos: unknown = await res.json().catch(() => null);
  if (!res.ok) {
    const cuerpo = (datos ?? {}) as { error?: string; detalles?: string[] };
    const mensaje = cuerpo.error ?? `Error inesperado (${res.status}).`;
    const detalles = (cuerpo.detalles ?? []).filter((d) => d && d !== mensaje);
    throw new ApiError(mensaje, detalles, res.status);
  }
  return datos as T;
}

export const api = {
  // CU001
  ingresarConsultor: (username: string, password: string) =>
    pedir<SesionConsultor>('POST', '/api/auth/consultor', { cuerpo: { username, password } }),
  ingresarCandidato: (tipoDocumento: string, nroDocumento: string, clave: string) =>
    pedir<SesionCandidato>('POST', '/api/auth/candidato', { cuerpo: { tipoDocumento, nroDocumento, clave } }),

  // combos del CU020 y del CU021: competencias, factores y opciones de respuesta
  buscarCompetencias: (nombre?: string) => pedir<Competencia[]>('GET', '/api/catalogo/competencias', { consulta: { nombre } }),
  buscarFactores: (competenciaId?: number | null, nombre?: string) =>
    pedir<Factor[]>('GET', '/api/catalogo/factores', { consulta: { competenciaId, nombre } }),
  buscarOpcionesRespuesta: (nombre?: string) =>
    pedir<OpcionRespuesta[]>('GET', '/api/catalogo/opciones-respuesta', { consulta: { nombre } }),

  // puestos para elegir en el CU025
  buscarPuestos: (filtros: { codigo?: string; nombre?: string; empresa?: string } = {}) =>
    pedir<Puesto[]>('GET', '/api/catalogo/puestos', { consulta: filtros }),

  // CU020 a CU023
  buscarPreguntas: (filtros: { competenciaId?: number | null; factorId?: number | null; nombre?: string }) =>
    pedir<Pregunta[]>('GET', '/api/catalogo/preguntas', { consulta: filtros }),
  crearPregunta: (datos: {
    factorId: number;
    opcionRespuestaId: number;
    nombre: string;
    enunciado: string;
    descripcion: string | null;
    ponderaciones: Record<number, number>;
  }) => pedir<Pregunta>('POST', '/api/catalogo/preguntas', { cuerpo: datos }),
  modificarPregunta: (
    id: number,
    datos: {
      opcionRespuestaId: number;
      nombre: string;
      enunciado: string;
      descripcion: string | null;
      ponderaciones: Record<number, number>;
    },
  ) => pedir<Pregunta>('PUT', `/api/catalogo/preguntas/${id}`, { cuerpo: datos }),
  bajaPregunta: (id: number, usuario: string) =>
    pedir<void>('DELETE', `/api/catalogo/preguntas/${id}`, { consulta: { usuario } }),

  // CU033
  generarPreguntaIA: (competenciaId: number, factorId: number) =>
    pedir<PreguntaGenerada>('POST', '/api/catalogo/preguntas/generar-ia', { cuerpo: { competenciaId, factorId } }),

  // CU025
  buscarCandidatos: (filtros: { apellido?: string; nombre?: string; nroCandidato?: string }) =>
    pedir<Candidato[]>('GET', '/api/evaluaciones/candidatos', { consulta: filtros }),
  validarCandidatos: (candidatoIds: number[]) =>
    pedir<Candidato[]>('POST', '/api/evaluaciones/validar-candidatos', { cuerpo: { candidatoIds } }),
  competenciasPuesto: (puestoId: number) =>
    pedir<CompetenciaPuesto[]>('GET', `/api/evaluaciones/puestos/${puestoId}/competencias`),
  generarClaves: (candidatoIds: number[], puestoId: number) =>
    pedir<CandidatoClave[]>('POST', '/api/evaluaciones/claves', { cuerpo: { candidatoIds, puestoId } }),
  guardarEvaluacion: (candidatoIds: number[], puestoId: number, claves: Record<number, string>) =>
    pedir<Cuestionario[]>('POST', '/api/evaluaciones/generar', { cuerpo: { candidatoIds, puestoId, claves } }),

  // CU026
  configuracion: () => pedir<Configuracion>('GET', '/api/evaluaciones/configuracion'),
  iniciarCuestionario: (cuestionarioId: number) =>
    pedir<Cuestionario>('POST', `/api/evaluaciones/cuestionarios/${cuestionarioId}/iniciar`),
  responderBloque: (
    cuestionarioId: number,
    nroBloque: number,
    respuestas: { preguntaCuestionarioId: number; detalleOpcionRespuestaId: number }[],
  ) =>
    pedir<Cuestionario>('POST', `/api/evaluaciones/cuestionarios/${cuestionarioId}/bloques/${nroBloque}/responder`, {
      cuerpo: { respuestas },
    }),

  // CU028
  buscarPuestosConEvaluaciones: (filtros: { codigo?: string; nombre?: string; empresa?: string }) =>
    pedir<PuestoResumen[]>('GET', '/api/reportes/puestos', { consulta: filtros }),
  evaluacionesPuesto: (puestoId: number) =>
    pedir<EvaluacionesPuesto>('GET', `/api/reportes/puestos/${puestoId}/evaluaciones`),
  ordenDeMerito: (puestoId: number, evaluacion: string, emitidoPor: string) =>
    pedir<ReporteOrdenMerito>('GET', '/api/reportes/orden-merito', { consulta: { puestoId, evaluacion, emitidoPor } }),
};
