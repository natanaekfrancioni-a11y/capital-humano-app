'use client';

import { useEffect, useState } from 'react';
import { Modal } from '@/components/Modal';

interface Puesto {
  id: number;
  codigo: string;
  nombre: string;
  empresa: string;
}

interface FilaMerito {
  cuestionarioId: number;
  apellido: string;
  nombre: string;
  nroDocumento?: string;
  estado: string;
  puntajeObtenido?: number | null;
}

interface ReporteMerito {
  puestoCodigo: string;
  puestoNombre: string;
  empresa: string;
  ordenMerito: FilaMerito[];
  noAlcanzanMinimo: FilaMerito[];
}

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

export default function ReporteComparativoPage() {
  const [puestos, setPuestos] = useState<Puesto[]>([]);
  const [puestoId, setPuestoId] = useState('');
  const [reporte, setReporte] = useState<ReporteMerito | null>(null);
  const [cargando, setCargando] = useState(false);
  const [alerta, setAlerta] = useState<{
    open: boolean;
    title: string;
    description: string;
    variant: 'default' | 'danger' | 'success';
  }>({ open: false, title: '', description: '', variant: 'default' });

  useEffect(() => {
    fetch(`${API_URL}/api/catalogo/puestos`)
      .then((r) => (r.ok ? r.json() : []))
      .then((data: Puesto[]) => {
        setPuestos(data);
        if (data[0]?.id) {
          setPuestoId(String(data[0].id));
        }
      })
      .catch(() => {});
  }, []);

  async function cargarComparativo(e: React.FormEvent) {
    e.preventDefault();
    if (!puestoId) return;
    setCargando(true);
    try {
      const res = await fetch(`${API_URL}/api/evaluaciones/orden-merito?puestoId=${puestoId}`);
      const data = await res.json().catch(() => null);
      if (!res.ok) {
        setReporte(null);
        setAlerta({
          open: true,
          title: 'Sin datos para comparar',
          description: data?.message || data?.error || data?.detail || `Error (${res.status})`,
          variant: 'danger',
        });
        return;
      }
      setReporte(data);
    } catch (err) {
      setAlerta({
        open: true,
        title: 'Error de conexión',
        description: err instanceof Error ? err.message : 'Error al consultar el reporte',
        variant: 'danger',
      });
    } finally {
      setCargando(false);
    }
  }

  const todosEvaluados: FilaMerito[] = reporte
    ? [...(reporte.ordenMerito ?? []), ...(reporte.noAlcanzanMinimo ?? [])]
    : [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">Reporte Comparativo</h1>
        <p className="mt-1 text-sm text-slate-500">
          Compará los puntajes y el cumplimiento de mínimos de los candidatos evaluados para un puesto.
        </p>
      </div>

      <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-xs">
        <form onSubmit={cargarComparativo} className="flex flex-wrap items-end gap-3">
          <div className="min-w-[260px] flex-1">
            <label className="mb-1 block text-xs font-semibold text-slate-700">Puesto</label>
            <select
              value={puestoId}
              onChange={(e) => setPuestoId(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900"
            >
              {puestos.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.codigo} — {p.nombre} ({p.empresa})
                </option>
              ))}
            </select>
          </div>
          <button
            type="submit"
            disabled={cargando}
            className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-50"
          >
            {cargando ? 'Generando...' : 'Generar Comparativo'}
          </button>
        </form>
      </section>

      {reporte && (
        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xs">
          <div className="border-b border-slate-200 px-5 py-3.5 flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              {reporte.puestoNombre} · {reporte.empresa}
            </span>
            <span className="text-xs font-medium text-slate-600">
              Evaluados completos: {todosEvaluados.length}
            </span>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
              <thead className="bg-slate-50 text-xs font-semibold uppercase tracking-wider text-slate-500">
                <tr>
                  <th className="px-5 py-3">Candidato</th>
                  <th className="px-5 py-3">Documento</th>
                  <th className="px-5 py-3">Puntaje</th>
                  <th className="px-5 py-3">Condición</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {todosEvaluados.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="px-5 py-8 text-center text-slate-500">
                      Todavía no hay cuestionarios completos para comparar en este puesto.
                    </td>
                  </tr>
                ) : (
                  todosEvaluados.map((fila) => {
                    const cumple = (reporte.ordenMerito ?? []).some(
                      (m) => m.cuestionarioId === fila.cuestionarioId
                    );
                    return (
                      <tr key={fila.cuestionarioId} className="hover:bg-slate-50/80">
                        <td className="px-5 py-3.5 font-medium text-slate-900">
                          {fila.apellido}, {fila.nombre}
                        </td>
                        <td className="px-5 py-3.5 text-slate-600">{fila.nroDocumento ?? '-'}</td>
                        <td className="px-5 py-3.5 font-mono font-semibold text-slate-900">
                          {fila.puntajeObtenido ?? '-'}
                        </td>
                        <td className="px-5 py-3.5">
                          <span
                            className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                              cumple
                                ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                                : 'bg-rose-50 text-rose-800 border border-rose-200'
                            }`}
                          >
                            {cumple ? 'Cumple mínimos' : 'No alcanza mínimo'}
                          </span>
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
        open={alerta.open}
        title={alerta.title}
        description={alerta.description}
        variant={alerta.variant}
        onClose={() => setAlerta((prev) => ({ ...prev, open: false }))}
      />
    </div>
  );
}
