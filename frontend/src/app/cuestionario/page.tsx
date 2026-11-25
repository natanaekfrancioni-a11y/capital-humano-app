'use client';

import { useState } from 'react';
import { Modal } from '@/components/Modal';

interface DetalleOpcion {
  id: number;
  nroOrden?: number;
  descripcion: string;
}

interface PreguntaItem {
  id: number;
  nroBloque: number;
  ordenVisual: number;
  respondida?: boolean;
  competencia?: { nombre: string };
  factor?: { nombre: string };
  pregunta?: {
    id: number;
    nombre: string;
    enunciado: string;
    opcionRespuesta?: {
      nombre: string;
      detalles?: DetalleOpcion[];
    };
  };
}

interface CuestionarioEstado {
  id: number;
  estado: string;
  bloqueActual: number;
  puntajeTotal?: number | null;
  candidato?: { apellido: string; nombre: string };
  puesto?: { nombre: string; empresa: string };
  preguntas?: PreguntaItem[];
}

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

export default function CuestionarioPage() {
  const [cuestionarioIdInput, setCuestionarioIdInput] = useState('1');
  const [cuestionario, setCuestionario] = useState<CuestionarioEstado | null>(null);
  const [selecciones, setSelecciones] = useState<Record<number, number>>({});
  const [cargando, setCargando] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [alerta, setAlerta] = useState<{
    open: boolean;
    title: string;
    description: string;
    variant: 'default' | 'danger' | 'success';
  }>({ open: false, title: '', description: '', variant: 'default' });

  async function iniciarCuestionario(e: React.FormEvent) {
    e.preventDefault();
    const idNum = Number(cuestionarioIdInput);
    if (!idNum) return;

    setCargando(true);
    try {
      const res = await fetch(`${API_URL}/api/evaluaciones/cuestionarios/${idNum}/iniciar`, {
        method: 'POST',
      });
      const data = await res.json().catch(() => null);
      if (!res.ok) {
        setAlerta({
          open: true,
          title: 'No se pudo iniciar el cuestionario',
          description: data?.message || `Error (${res.status})`,
          variant: 'danger',
        });
        return;
      }
      setCuestionario(data);
      setSelecciones({});
    } catch (err) {
      setAlerta({
        open: true,
        title: 'Error de conexión',
        description: err instanceof Error ? err.message : 'Error al iniciar cuestionario',
        variant: 'danger',
      });
    } finally {
      setCargando(false);
    }
  }

  async function enviarBloqueActual(e: React.FormEvent) {
    e.preventDefault();
    if (!cuestionario) return;

    const bloquePreguntas = (cuestionario.preguntas ?? []).filter(
      (p) => p.nroBloque === cuestionario.bloqueActual
    );

    const respuestasPayload = bloquePreguntas.map((pq) => ({
      preguntaCuestionarioId: pq.id,
      detalleOpcionRespuestaId: selecciones[pq.id] ?? null,
    }));

    setEnviando(true);
    try {
      const res = await fetch(
        `${API_URL}/api/evaluaciones/cuestionarios/${cuestionario.id}/bloques/${cuestionario.bloqueActual}/responder`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ respuestas: respuestasPayload }),
        }
      );

      const data = await res.json().catch(() => null);
      if (!res.ok) {
        setAlerta({
          open: true,
          title: 'Revisá las respuestas del bloque',
          description: data?.message || `Error al responder bloque (${res.status})`,
          variant: 'danger',
        });
        return;
      }

      setCuestionario(data);
      setSelecciones({});
      if (data?.estado === 'COMPLETO') {
        setAlerta({
          open: true,
          title: 'Cuestionario completado',
          description: `Puntaje final obtenido: ${data.puntajeTotal ?? 'calculado'}. Ya figura disponible en el Orden de Méritos.`,
          variant: 'success',
        });
      }
    } catch (err) {
      setAlerta({
        open: true,
        title: 'Error al enviar bloque',
        description: err instanceof Error ? err.message : 'Error de red',
        variant: 'danger',
      });
    } finally {
      setEnviando(false);
    }
  }

  const preguntasDelBloque =
    cuestionario && cuestionario.estado === 'EN_PROCESO'
      ? (cuestionario.preguntas ?? [])
          .filter((p) => p.nroBloque === cuestionario.bloqueActual)
          .sort((a, b) => a.ordenVisual - b.ordenVisual)
      : [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">Completar Cuestionario</h1>
        <p className="mt-1 text-sm text-slate-500">
          Iniciá o sorteá un cuestionario activo por ID y respondé bloque a bloque.
        </p>
      </div>

      <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-xs">
        <form onSubmit={iniciarCuestionario} className="flex flex-wrap items-end gap-3">
          <div>
            <label className="mb-1 block text-xs font-semibold text-slate-700">
              ID de Cuestionario
            </label>
            <input
              type="number"
              min="1"
              value={cuestionarioIdInput}
              onChange={(e) => setCuestionarioIdInput(e.target.value)}
              className="w-40 rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm font-mono text-slate-900"
            />
          </div>
          <button
            type="submit"
            disabled={cargando}
            className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-50"
          >
            {cargando ? 'Cargando...' : 'Iniciar / Continuar'}
          </button>
        </form>
      </section>

      {cuestionario && (
        <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-xs space-y-2">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                Cuestionario #{cuestionario.id}
              </span>
              <h2 className="text-lg font-semibold text-slate-900">
                {cuestionario.puesto?.nombre ?? 'Evaluación'} —{' '}
                {cuestionario.candidato
                  ? `${cuestionario.candidato.apellido}, ${cuestionario.candidato.nombre}`
                  : ''}
              </h2>
            </div>
            <div className="flex items-center gap-2 text-xs font-semibold">
              <span className="rounded-full bg-rose-50 border border-rose-200 px-3 py-1 text-slate-800">
                Estado: {cuestionario.estado}
              </span>
              {cuestionario.estado === 'EN_PROCESO' && (
                <span className="rounded-full bg-slate-100 px-3 py-1 text-slate-700">
                  Bloque {cuestionario.bloqueActual}
                </span>
              )}
              {cuestionario.puntajeTotal != null && (
                <span className="rounded-full bg-emerald-50 border border-emerald-200 px-3 py-1 text-emerald-800">
                  Puntaje: {cuestionario.puntajeTotal}
                </span>
              )}
            </div>
          </div>
        </section>
      )}

      {cuestionario && cuestionario.estado === 'EN_PROCESO' && (
        <form onSubmit={enviarBloqueActual} className="space-y-4">
          {preguntasDelBloque.map((pq, idx) => {
            const detalles = pq.pregunta?.opcionRespuesta?.detalles ?? [];
            return (
              <div
                key={pq.id}
                className="rounded-xl border border-slate-200 bg-white p-5 shadow-xs space-y-3"
              >
                <div className="flex flex-wrap items-center gap-2 text-xs text-slate-500">
                  <span className="font-semibold text-slate-800">Pregunta {idx + 1}</span>
                  <span>·</span>
                  <span>{pq.competencia?.nombre ?? 'Competencia'}</span>
                  <span>·</span>
                  <span>{pq.factor?.nombre ?? 'Factor'}</span>
                </div>
                <p className="text-sm font-medium text-slate-900">
                  {pq.pregunta?.enunciado ?? pq.pregunta?.nombre}
                </p>
                <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                  {detalles.map((det) => {
                    const seleccionada = selecciones[pq.id] === det.id;
                    return (
                      <label
                        key={det.id}
                        className={`flex cursor-pointer items-center gap-2.5 rounded-lg border px-3.5 py-2.5 text-sm transition ${
                          seleccionada
                            ? 'border-slate-900 bg-rose-50/80 font-medium text-slate-900'
                            : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                        }`}
                      >
                        <input
                          type="radio"
                          name={`preg-${pq.id}`}
                          checked={seleccionada}
                          onChange={() =>
                            setSelecciones((prev) => ({ ...prev, [pq.id]: det.id }))
                          }
                          className="h-4 w-4 text-slate-900"
                        />
                        <span>{det.descripcion}</span>
                      </label>
                    );
                  })}
                </div>
              </div>
            );
          })}

          <div className="flex justify-end">
            <button
              type="submit"
              disabled={enviando}
              className="rounded-lg bg-slate-900 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-50"
            >
              {enviando ? 'Guardando bloque...' : 'Guardar y Continuar'}
            </button>
          </div>
        </form>
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
