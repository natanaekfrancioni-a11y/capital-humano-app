'use client';

import { useEffect, useState } from 'react';
import { Modal } from '@/components/Modal';

interface Candidato {
  id: number;
  nroCandidato?: string;
  apellido: string;
  nombre: string;
  tipoDocumento?: string;
  nroDocumento?: string;
  escolaridad?: string;
  email?: string;
}

interface Puesto {
  id: number;
  codigo: string;
  nombre: string;
  empresa: string;
}

interface CuestionarioCreado {
  id: number;
  claveAcceso: string;
  estado: string;
  candidato?: Candidato;
  puesto?: Puesto;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

export default function EvaluarCandidatosPage() {
  const [candidatos, setCandidatos] = useState<Candidato[]>([]);
  const [puestos, setPuestos] = useState<Puesto[]>([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [puestoId, setPuestoId] = useState('');
  const [apellido, setApellido] = useState('');
  const [nombre, setNombre] = useState('');
  const [nroCandidato, setNroCandidato] = useState('');
  const [loading, setLoading] = useState(true);
  const [generando, setGenerando] = useState(false);
  const [creados, setCreados] = useState<CuestionarioCreado[]>([]);
  const [alerta, setAlerta] = useState<{
    open: boolean;
    title: string;
    description: string;
    variant: 'default' | 'danger' | 'success';
  }>({ open: false, title: '', description: '', variant: 'default' });

  async function cargarDatos() {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (apellido.trim()) params.set('apellido', apellido.trim());
      if (nombre.trim()) params.set('nombre', nombre.trim());
      if (nroCandidato.trim()) params.set('nroCandidato', nroCandidato.trim());

      const [resCand, resPuestos] = await Promise.all([
        fetch(`${API_URL}/api/evaluaciones/candidatos${params.toString() ? `?${params.toString()}` : ''}`),
        fetch(`${API_URL}/api/catalogo/puestos`),
      ]);

      if (!resCand.ok) throw new Error(`Error al cargar candidatos (${resCand.status})`);
      if (!resPuestos.ok) throw new Error(`Error al cargar puestos (${resPuestos.status})`);

      const dataCand: Candidato[] = await resCand.json();
      const dataPuestos: Puesto[] = await resPuestos.json();
      setCandidatos(dataCand);
      setPuestos(dataPuestos);
      if (!puestoId && dataPuestos[0]?.id) {
        setPuestoId(String(dataPuestos[0].id));
      }
    } catch (err) {
      setAlerta({
        open: true,
        title: 'Error al cargar datos',
        description: err instanceof Error ? err.message : 'No se pudo consultar la API',
        variant: 'danger',
      });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    cargarDatos();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function toggleCandidato(id: number) {
    setSelectedIds((prev) => (prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]));
  }

  async function generarEvaluaciones(e: React.FormEvent) {
    e.preventDefault();
    if (selectedIds.length === 0) {
      setAlerta({
        open: true,
        title: 'Selección requerida',
        description: 'Seleccioná al menos un candidato para generar su evaluación.',
        variant: 'danger',
      });
      return;
    }
    if (!puestoId) {
      setAlerta({
        open: true,
        title: 'Puesto requerido',
        description: 'Seleccioná un puesto o función para evaluar.',
        variant: 'danger',
      });
      return;
    }

    setGenerando(true);
    try {
      const res = await fetch(`${API_URL}/api/evaluaciones/generar`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          candidatoIds: selectedIds,
          puestoId: Number(puestoId),
        }),
      });

      const data = await res.json().catch(() => null);
      if (!res.ok) {
        const msg =
          data?.message || data?.error || data?.detail ||
          (Array.isArray(data?.errors) ? data.errors.join(' · ') : null) ||
          `Error al generar evaluaciones (${res.status})`;
        setAlerta({
          open: true,
          title: 'No se pudo generar la evaluación',
          description: msg,
          variant: 'danger',
        });
        return;
      }

      const lista: CuestionarioCreado[] = Array.isArray(data) ? data : [];
      setCreados(lista);
      setSelectedIds([]);
      setAlerta({
        open: true,
        title: 'Evaluaciones generadas',
        description: `Se generaron ${lista.length} cuestionario(s) con clave de acceso lista para rendir.`,
        variant: 'success',
      });
    } catch (err) {
      setAlerta({
        open: true,
        title: 'Error de conexión',
        description: err instanceof Error ? err.message : 'Error al comunicarse con el servidor',
        variant: 'danger',
      });
    } finally {
      setGenerando(false);
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">Evaluar Candidatos</h1>
        <p className="mt-1 text-sm text-slate-500">
          Seleccioná candidatos y un puesto para generar cuestionarios activos con clave de acceso.
        </p>
      </div>

      <form onSubmit={generarEvaluaciones} className="space-y-5">
        <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-xs">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-4">
            <div className="sm:col-span-2">
              <label className="mb-1 block text-xs font-semibold text-slate-700">
                Puesto a evaluar (*)
              </label>
              <select
                value={puestoId}
                onChange={(e) => setPuestoId(e.target.value)}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900"
              >
                <option value="">Seleccionar puesto</option>
                {puestos.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.codigo} — {p.nombre} ({p.empresa})
                  </option>
                ))}
              </select>
            </div>

            <div className="sm:col-span-2 flex items-end justify-end">
              <button
                type="submit"
                disabled={generando}
                className="w-full sm:w-auto rounded-lg bg-slate-900 px-5 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-50"
              >
                {generando ? 'Generando...' : `Generar Evaluaciones (${selectedIds.length})`}
              </button>
            </div>
          </div>
        </section>

        <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-xs">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-4">
            <input
              type="text"
              placeholder="Apellido"
              value={apellido}
              onChange={(e) => setApellido(e.target.value)}
              className="rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-900"
            />
            <input
              type="text"
              placeholder="Nombre"
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              className="rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-900"
            />
            <input
              type="text"
              placeholder="Nro Candidato"
              value={nroCandidato}
              onChange={(e) => setNroCandidato(e.target.value)}
              className="rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-900"
            />
            <button
              type="button"
              onClick={cargarDatos}
              className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-800 hover:bg-slate-50"
            >
              Filtrar candidatos
            </button>
          </div>
        </section>

        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xs">
          <div className="border-b border-slate-200 px-5 py-3.5 text-xs font-semibold uppercase tracking-wider text-slate-500">
            Candidatos disponibles ({loading ? '...' : candidatos.length})
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
              <thead className="bg-slate-50 text-xs font-semibold uppercase tracking-wider text-slate-500">
                <tr>
                  <th className="px-4 py-3 w-12">Sel.</th>
                  <th className="px-5 py-3">Nro</th>
                  <th className="px-5 py-3">Apellido y Nombre</th>
                  <th className="px-5 py-3">Documento</th>
                  <th className="px-5 py-3">Escolaridad</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {candidatos.map((c) => {
                  const checked = selectedIds.includes(c.id);
                  return (
                    <tr
                      key={c.id}
                      onClick={() => toggleCandidato(c.id)}
                      className={`cursor-pointer transition ${
                        checked ? 'bg-rose-50/80' : 'hover:bg-slate-50/80'
                      }`}
                    >
                      <td className="px-4 py-3.5">
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={() => toggleCandidato(c.id)}
                          onClick={(e) => e.stopPropagation()}
                          className="h-4 w-4 rounded border-slate-300 text-slate-900"
                        />
                      </td>
                      <td className="px-5 py-3.5 font-mono text-xs text-slate-700">
                        {c.nroCandidato ?? `CAND-${c.id}`}
                      </td>
                      <td className="px-5 py-3.5 font-medium text-slate-900">
                        {c.apellido}, {c.nombre}
                      </td>
                      <td className="px-5 py-3.5 text-slate-600">
                        {c.tipoDocumento ? `${c.tipoDocumento} ` : ''}
                        {c.nroDocumento ?? '-'}
                      </td>
                      <td className="px-5 py-3.5 text-slate-600">{c.escolaridad ?? '-'}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </section>
      </form>

      {creados.length > 0 && (
        <section className="rounded-xl border border-emerald-200 bg-white p-5 shadow-xs">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-emerald-800">
            Cuestionarios generados recientemente
          </h2>
          <div className="mt-3 divide-y divide-slate-100 text-sm">
            {creados.map((q) => (
              <div key={q.id} className="flex flex-wrap items-center justify-between gap-2 py-2.5">
                <div>
                  <span className="font-mono text-xs text-slate-500">ID #{q.id}</span> ·{' '}
                  <span className="font-medium text-slate-900">
                    {q.candidato ? `${q.candidato.apellido}, ${q.candidato.nombre}` : 'Candidato'}
                  </span>
                </div>
                <div className="flex items-center gap-3">
                  <span className="rounded-md bg-rose-50 px-2.5 py-1 font-mono text-xs font-semibold text-slate-900 border border-rose-200">
                    Clave: {q.claveAcceso}
                  </span>
                  <span className="text-xs font-medium text-slate-600">{q.estado}</span>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

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
