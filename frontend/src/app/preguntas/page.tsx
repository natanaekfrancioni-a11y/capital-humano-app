'use client';

import { useEffect, useState } from 'react';
import { Modal } from '@/components/Modal';

interface Competencia {
  id: number;
  nombre: string;
}

interface Factor {
  id: number;
  nombre: string;
  competencia?: Competencia;
}

interface OpcionRespuesta {
  id: number;
  nombre: string;
  descripcion?: string;
}

interface Pregunta {
  id: number;
  codigo?: string;
  nombre: string;
  enunciado?: string;
  descripcion?: string;
  factor?: Factor;
  opcionRespuesta?: OpcionRespuesta;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

export default function PreguntasPage() {
  const [preguntas, setPreguntas] = useState<Pregunta[]>([]);
  const [competencias, setCompetencias] = useState<Competencia[]>([]);
  const [factores, setFactores] = useState<Factor[]>([]);
  const [opciones, setOpciones] = useState<OpcionRespuesta[]>([]);

  const [filtroCompetenciaId, setFiltroCompetenciaId] = useState('');
  const [filtroFactorId, setFiltroFactorId] = useState('');
  const [filtroNombre, setFiltroNombre] = useState('');
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Alta / Modificar
  const [modoFormulario, setModoFormulario] = useState<'none' | 'crear' | 'editar'>('none');
  const [formCompetenciaId, setFormCompetenciaId] = useState('');
  const [formFactorId, setFormFactorId] = useState('');
  const [formCodigo, setFormCodigo] = useState('');
  const [formNombre, setFormNombre] = useState('');
  const [formEnunciado, setFormEnunciado] = useState('');
  const [formDescripcion, setFormDescripcion] = useState('');
  const [formOpcionId, setFormOpcionId] = useState('');
  const [guardando, setGuardando] = useState(false);

  // Modales específicos de flujo
  const [modalBajaOpen, setModalBajaOpen] = useState(false);
  const [modalCargarOtraOpen, setModalCargarOtraOpen] = useState(false);
  const [alerta, setAlerta] = useState<{
    open: boolean;
    title: string;
    description: string;
    variant: 'default' | 'danger' | 'success';
  }>({ open: false, title: '', description: '', variant: 'default' });

  async function cargarCatalogosYPreguntas() {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      if (filtroCompetenciaId) params.set('competenciaId', filtroCompetenciaId);
      if (filtroFactorId) params.set('factorId', filtroFactorId);
      if (filtroNombre.trim()) params.set('nombre', filtroNombre.trim());

      const [resPreg, resComp, resFact, resOpc] = await Promise.all([
        fetch(`${API_URL}/api/catalogo/preguntas${params.toString() ? `?${params.toString()}` : ''}`),
        fetch(`${API_URL}/api/catalogo/competencias`),
        fetch(`${API_URL}/api/catalogo/factores`),
        fetch(`${API_URL}/api/catalogo/opciones-respuesta`),
      ]);

      if (!resPreg.ok) throw new Error(`Error al cargar preguntas (${resPreg.status})`);
      if (!resComp.ok) throw new Error(`Error al cargar competencias (${resComp.status})`);
      if (!resFact.ok) throw new Error(`Error al cargar factores (${resFact.status})`);
      if (!resOpc.ok) throw new Error(`Error al cargar opciones (${resOpc.status})`);

      const [dataPreg, dataComp, dataFact, dataOpc] = await Promise.all([
        resPreg.json(),
        resComp.json(),
        resFact.json(),
        resOpc.json(),
      ]);

      setPreguntas(dataPreg);
      setCompetencias(dataComp);
      setFactores(dataFact);
      setOpciones(dataOpc);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al consultar preguntas');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    cargarCatalogosYPreguntas();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const preguntaSeleccionada = preguntas.find((p) => p.id === selectedId) ?? null;

  function abrirNuevaPregunta() {
    const primeraComp = competencias[0]?.id ? String(competencias[0].id) : '';
    const primerFactor = factores[0]?.id ? String(factores[0].id) : '';
    const primeraOpc = opciones[0]?.id ? String(opciones[0].id) : '';

    setFormCompetenciaId(primeraComp);
    setFormFactorId(primerFactor);
    setFormCodigo(`PREG-${Date.now().toString().slice(-4)}`);
    setFormNombre('');
    setFormEnunciado('');
    setFormDescripcion('');
    setFormOpcionId(primeraOpc);
    setModoFormulario('crear');
  }

  function abrirModificarPregunta() {
    if (!preguntaSeleccionada) return;
    setFormCompetenciaId(
      preguntaSeleccionada.factor?.competencia?.id
        ? String(preguntaSeleccionada.factor.competencia.id)
        : ''
    );
    setFormFactorId(preguntaSeleccionada.factor?.id ? String(preguntaSeleccionada.factor.id) : '');
    setFormCodigo(preguntaSeleccionada.codigo ?? '');
    setFormNombre(preguntaSeleccionada.nombre ?? '');
    setFormEnunciado(preguntaSeleccionada.enunciado ?? '');
    setFormDescripcion(preguntaSeleccionada.descripcion ?? '');
    setFormOpcionId(
      preguntaSeleccionada.opcionRespuesta?.id ? String(preguntaSeleccionada.opcionRespuesta.id) : ''
    );
    setModoFormulario('editar');
  }

  function generarConIA() {
    const factorNombre =
      factores.find((f) => String(f.id) === formFactorId)?.nombre ?? 'el factor seleccionado';
    const baseNombre = formNombre.trim() || `Evaluación de ${factorNombre}`;
    setFormNombre(baseNombre);
    setFormEnunciado(
      `¿Cómo aplicás criterios de ${factorNombre.toLowerCase()} para resolver un escenario complejo en equipo?`
    );
    if (!formDescripcion.trim()) {
      setFormDescripcion(`Ítem generado para evaluar ${factorNombre.toLowerCase()}.`);
    }
  }

  async function guardarPregunta(e: React.FormEvent) {
    e.preventDefault();

    const faltantes: string[] = [];
    if (!formCompetenciaId) faltantes.push('Competencia (*)');
    if (!formFactorId) faltantes.push('Factor (*)');
    if (!formNombre.trim()) faltantes.push('Nombre de Pregunta (*)');
    if (!formEnunciado.trim()) faltantes.push('Pregunta (*)');
    if (!formOpcionId) faltantes.push('Opción de Respuesta (*)');
    if (modoFormulario === 'crear' && !formCodigo.trim()) faltantes.push('Código');

    if (faltantes.length > 0) {
      setAlerta({
        open: true,
        title: 'Omisión en Campos',
        description: `Los siguientes campos son obligatorios: [${faltantes.join(', ')}]. Es necesario completarlos para continuar.`,
        variant: 'danger',
      });
      return;
    }

    const excedidos: string[] = [];
    if (formCodigo.trim().length > 30) excedidos.push('Código (máx. 30)');
    if (formNombre.trim().length > 120) excedidos.push('Nombre de Pregunta (máx. 120)');
    if (formDescripcion.trim().length > 255) excedidos.push('Descripción (máx. 255)');

    if (excedidos.length > 0) {
      setAlerta({
        open: true,
        title: 'Longitud maxima superada',
        description: `Se ha superado el límite de caracteres en los siguientes campos: [${excedidos.join(', ')}]. Por favor, acorte el texto para continuar.`,
        variant: 'danger',
      });
      return;
    }

    setGuardando(true);
    try {
      const esEdicion = modoFormulario === 'editar' && preguntaSeleccionada;
      const url = esEdicion
        ? `${API_URL}/api/catalogo/preguntas/${preguntaSeleccionada.id}`
        : `${API_URL}/api/catalogo/preguntas`;

      const body = esEdicion
        ? {
            opcionRespuestaId: Number(formOpcionId),
            nombre: formNombre.trim(),
            enunciado: formEnunciado.trim(),
            descripcion: formDescripcion.trim() || null,
          }
        : {
            factorId: Number(formFactorId),
            opcionRespuestaId: Number(formOpcionId),
            codigo: formCodigo.trim(),
            nombre: formNombre.trim(),
            enunciado: formEnunciado.trim(),
            descripcion: formDescripcion.trim() || null,
          };

      const res = await fetch(url, {
        method: esEdicion ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });

      const data = await res.json().catch(() => null);
      if (!res.ok) {
        const msg =
          data?.message ||
          (Array.isArray(data?.errors) ? data.errors.join(' · ') : null) ||
          `Error al guardar pregunta (${res.status})`;
        const esDuplicada = String(msg).toLowerCase().includes('ya existe');
        setAlerta({
          open: true,
          title: esDuplicada ? 'Pregunta ya existente' : 'Error de validación',
          description: esDuplicada
            ? 'Ya se encuentra registrada una pregunta con el mismo nombre para la combinación de Competencia y Factor seleccionada. Por favor, verifique los datos para evitar duplicados.'
            : msg,
          variant: 'danger',
        });
        return;
      }

      await cargarCatalogosYPreguntas();

      if (!esEdicion) {
        setModalCargarOtraOpen(true);
      } else {
        setModoFormulario('none');
        setAlerta({
          open: true,
          title: 'Pregunta actualizada',
          description: `La pregunta "${formNombre.trim()}" se modificó correctamente.`,
          variant: 'success',
        });
      }
    } catch (err) {
      setAlerta({
        open: true,
        title: 'Error de conexión',
        description: err instanceof Error ? err.message : 'Error al comunicarse con la API',
        variant: 'danger',
      });
    } finally {
      setGuardando(false);
    }
  }

  async function confirmarBajaPregunta() {
    if (!preguntaSeleccionada) return;
    const nombreEliminado = preguntaSeleccionada.nombre;
    setModalBajaOpen(false);

    try {
      const res = await fetch(`${API_URL}/api/catalogo/preguntas/${preguntaSeleccionada.id}`, {
        method: 'DELETE',
      });
      if (!res.ok) {
        const data = await res.json().catch(() => null);
        throw new Error(data?.message || `Error al eliminar pregunta (${res.status})`);
      }

      setSelectedId(null);
      await cargarCatalogosYPreguntas();
      setAlerta({
        open: true,
        title: 'SE HA ELIMINADO CORRECTAMENTE',
        description: `Los datos de la pregunta "${nombreEliminado}" han sido eliminados del sistema.`,
        variant: 'success',
      });
    } catch (err) {
      setAlerta({
        open: true,
        title: 'No se pudo eliminar',
        description: err instanceof Error ? err.message : 'Error inesperado',
        variant: 'danger',
      });
    }
  }

  const factoresFiltrados = filtroCompetenciaId
    ? factores.filter((f) => String(f.competencia?.id) === filtroCompetenciaId)
    : factores;

  const factoresFormFiltrados = formCompetenciaId
    ? factores.filter((f) => String(f.competencia?.id) === formCompetenciaId)
    : factores;

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Preguntas</h1>
          <p className="mt-1 text-sm text-slate-500">
            Gestioná el banco de preguntas por competencia, factor y opción de respuesta.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={abrirNuevaPregunta}
            className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white shadow-xs transition hover:bg-slate-800"
          >
            Nuevo
          </button>
          <button
            type="button"
            disabled={!preguntaSeleccionada}
            onClick={abrirModificarPregunta}
            className="rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-45"
          >
            Modificar
          </button>
          <button
            type="button"
            disabled={!preguntaSeleccionada}
            onClick={() => setModalBajaOpen(true)}
            className="rounded-lg border border-red-200 bg-red-50 px-3.5 py-2 text-sm font-medium text-red-700 transition hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-45"
          >
            Eliminar
          </button>
        </div>
      </div>

      {modoFormulario !== 'none' && (
        <section className="rounded-xl border border-slate-200 bg-white p-6 shadow-xs">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-lg font-semibold text-slate-900">
              {modoFormulario === 'crear' ? 'Alta de Preguntas' : 'Modificar Pregunta'}
            </h2>
            <button
              type="button"
              onClick={generarConIA}
              className="rounded-lg border border-slate-300 bg-rose-50 px-3 py-1.5 text-xs font-semibold text-slate-800 transition hover:bg-rose-100"
            >
              Generar con IA
            </button>
          </div>

          <form onSubmit={guardarPregunta} className="space-y-4">
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div>
                <label className="mb-1 block text-xs font-semibold text-slate-700">
                  Competencia (*)
                </label>
                <select
                  value={formCompetenciaId}
                  disabled={modoFormulario === 'editar'}
                  onChange={(e) => {
                    const compId = e.target.value;
                    setFormCompetenciaId(compId);
                    const primerFact = factores.find((f) => String(f.competencia?.id) === compId);
                    setFormFactorId(primerFact ? String(primerFact.id) : '');
                  }}
                  className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 disabled:bg-slate-100"
                >
                  <option value="">Seleccionar competencia</option>
                  {competencias.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.nombre}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="mb-1 block text-xs font-semibold text-slate-700">
                  Factor (*)
                </label>
                <select
                  value={formFactorId}
                  disabled={modoFormulario === 'editar'}
                  onChange={(e) => setFormFactorId(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 disabled:bg-slate-100"
                >
                  <option value="">Seleccionar factor</option>
                  {factoresFormFiltrados.map((f) => (
                    <option key={f.id} value={f.id}>
                      {f.nombre}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="mb-1 block text-xs font-semibold text-slate-700">
                  Nombre de Pregunta (*)
                </label>
                <input
                  type="text"
                  placeholder="Nombre de Pregunta (*)"
                  value={formNombre}
                  onChange={(e) => setFormNombre(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div>
                <label className="mb-1 block text-xs font-semibold text-slate-700">
                  Pregunta (*)
                </label>
                <input
                  type="text"
                  placeholder="Enunciado de la pregunta (*)"
                  value={formEnunciado}
                  onChange={(e) => setFormEnunciado(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900"
                />
              </div>

              <div>
                <label className="mb-1 block text-xs font-semibold text-slate-700">
                  Descripción
                </label>
                <input
                  type="text"
                  placeholder="Descripción opcional"
                  value={formDescripcion}
                  onChange={(e) => setFormDescripcion(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900"
                />
              </div>

              <div>
                <label className="mb-1 block text-xs font-semibold text-slate-700">
                  Opción de Respuesta (*)
                </label>
                <select
                  value={formOpcionId}
                  onChange={(e) => setFormOpcionId(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900"
                >
                  <option value="">Seleccionar opción</option>
                  {opciones.map((o) => (
                    <option key={o.id} value={o.id}>
                      {o.nombre}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {modoFormulario === 'crear' && (
              <div className="max-w-xs">
                <label className="mb-1 block text-xs font-semibold text-slate-700">Código</label>
                <input
                  type="text"
                  value={formCodigo}
                  onChange={(e) => setFormCodigo(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-mono text-slate-900"
                />
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setModoFormulario('none')}
                className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
              >
                Cancelar
              </button>
              <button
                type="submit"
                disabled={guardando}
                className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
              >
                {guardando ? 'Guardando...' : 'Aceptar'}
              </button>
            </div>
          </form>
        </section>
      )}

      <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-xs">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            cargarCatalogosYPreguntas();
          }}
          className="grid grid-cols-1 gap-3 sm:grid-cols-4"
        >
          <select
            value={filtroCompetenciaId}
            onChange={(e) => {
              setFiltroCompetenciaId(e.target.value);
              setFiltroFactorId('');
            }}
            className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900"
          >
            <option value="">Competencia</option>
            {competencias.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nombre}
              </option>
            ))}
          </select>

          <select
            value={filtroFactorId}
            onChange={(e) => setFiltroFactorId(e.target.value)}
            className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900"
          >
            <option value="">Factor</option>
            {factoresFiltrados.map((f) => (
              <option key={f.id} value={f.id}>
                {f.nombre}
              </option>
            ))}
          </select>

          <input
            type="text"
            placeholder="Nombre de Pregunta"
            value={filtroNombre}
            onChange={(e) => setFiltroNombre(e.target.value)}
            className="rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-900 placeholder:text-slate-400"
          />

          <div className="flex gap-2">
            <button
              type="submit"
              className="flex-1 rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800"
            >
              Buscar
            </button>
            <button
              type="button"
              onClick={() => {
                setFiltroCompetenciaId('');
                setFiltroFactorId('');
                setFiltroNombre('');
                setTimeout(() => cargarCatalogosYPreguntas(), 0);
              }}
              className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50"
            >
              Limpiar
            </button>
          </div>
        </form>
      </section>

      {loading && (
        <div className="rounded-xl border border-slate-200 bg-white p-8 text-center text-sm text-slate-500 shadow-xs">
          Cargando preguntas...
        </div>
      )}

      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {error}
        </div>
      )}

      {!loading && !error && (
        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xs">
          <div className="flex items-center justify-between border-b border-slate-200 px-5 py-3.5">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Banco de Preguntas ({preguntas.length})
            </span>
            {preguntaSeleccionada && (
              <span className="text-xs font-medium text-slate-700">
                Seleccionada: {preguntaSeleccionada.nombre}
              </span>
            )}
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
              <thead className="bg-slate-50 text-xs font-semibold uppercase tracking-wider text-slate-500">
                <tr>
                  <th className="px-4 py-3 w-12">Sel.</th>
                  <th className="px-5 py-3">Competencia</th>
                  <th className="px-5 py-3">Factor</th>
                  <th className="px-5 py-3">Pregunta</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {preguntas.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="px-5 py-8 text-center text-slate-500">
                      No hay preguntas para los filtros actuales.
                    </td>
                  </tr>
                ) : (
                  preguntas.map((preg) => {
                    const checked = selectedId === preg.id;
                    return (
                      <tr
                        key={preg.id}
                        onClick={() => setSelectedId(checked ? null : preg.id)}
                        className={`cursor-pointer transition ${
                          checked ? 'bg-rose-50/80' : 'hover:bg-slate-50/80'
                        }`}
                      >
                        <td className="px-4 py-3.5">
                          <input
                            type="radio"
                            name="preguntaSeleccionada"
                            checked={checked}
                            onChange={() => setSelectedId(checked ? null : preg.id)}
                            onClick={(e) => e.stopPropagation()}
                            className="h-4 w-4 border-slate-300 text-slate-900"
                          />
                        </td>
                        <td className="px-5 py-3.5 text-slate-700">
                          {preg.factor?.competencia?.nombre ?? '-'}
                        </td>
                        <td className="px-5 py-3.5 text-slate-700">
                          {preg.factor?.nombre ?? '-'}
                        </td>
                        <td className="px-5 py-3.5">
                          <div className="font-medium text-slate-900">{preg.nombre}</div>
                          {preg.enunciado && (
                            <div className="text-xs text-slate-500">{preg.enunciado}</div>
                          )}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </section>
      )}

      <Modal
        open={modalBajaOpen}
        title="BAJA DE PREGUNTA"
        description={`Los datos de la pregunta "${preguntaSeleccionada?.nombre ?? ''}" serán eliminados del sistema. ¿Desea continuar?`}
        variant="danger"
        confirmLabel="Sí"
        cancelLabel="No"
        onClose={() => setModalBajaOpen(false)}
        onConfirm={confirmarBajaPregunta}
      />

      <Modal
        open={modalCargarOtraOpen}
        title="Pregunta Guardada"
        description={`La Pregunta "${formNombre.trim()}" se ha creado correctamente. ¿Desea cargar otra?`}
        variant="success"
        confirmLabel="Sí"
        cancelLabel="No"
        onClose={() => {
          setModalCargarOtraOpen(false);
          setModoFormulario('none');
        }}
        onConfirm={() => {
          setModalCargarOtraOpen(false);
          abrirNuevaPregunta();
        }}
      />

      <Modal
        open={alerta.open}
        title={alerta.title}
        description={alerta.description}
        variant={alerta.variant}
        onClose={() => setAlerta((prev) => ({ ...prev, open: false }))}
      />
    </div>
  );
}
