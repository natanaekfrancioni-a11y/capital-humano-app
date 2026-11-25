'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';

interface Candidato {
  id: number;
  nroCandidato: string;
  apellido: string;
  nombre: string;
  tipoDocumento: string;
  nroDocumento: string;
  escolaridad?: string;
  email?: string;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

export default function HomePage() {
  const [candidatos, setCandidatos] = useState<Candidato[]>([]);
  const [apellido, setApellido] = useState('');
  const [nombre, setNombre] = useState('');
  const [nroCandidato, setNroCandidato] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function cargarCandidatos() {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      if (apellido.trim()) params.set('apellido', apellido.trim());
      if (nombre.trim()) params.set('nombre', nombre.trim());
      if (nroCandidato.trim()) params.set('nroCandidato', nroCandidato.trim());

      const qs = params.toString();
      const url = `${API_URL}/api/evaluaciones/candidatos${qs ? `?${qs}` : ''}`;
      const res = await fetch(url);
      if (!res.ok) {
        throw new Error(`Error HTTP ${res.status}`);
      }
      const data: Candidato[] = await res.json();
      setCandidatos(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al cargar candidatos');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    cargarCandidatos();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    cargarCandidatos();
  }

  function handleLimpiar() {
    setApellido('');
    setNombre('');
    setNroCandidato('');
    setTimeout(() => cargarCandidatos(), 0);
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Candidatos</h1>
          <p className="mt-1 text-sm text-slate-500">
            Consultá y filtrá los candidatos registrados para procesos de evaluación.
          </p>
        </div>
        <Link
          href="/evaluaciones"
          className="inline-flex items-center justify-center rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white shadow-xs transition hover:bg-slate-800"
        >
          Ir a Evaluar Candidatos
        </Link>
      </div>

      <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-xs">
        <form onSubmit={handleSubmit} className="grid grid-cols-1 gap-3 sm:grid-cols-4">
          <input
            type="text"
            placeholder="Apellido"
            value={apellido}
            onChange={(e) => setApellido(e.target.value)}
            className="rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-slate-500 focus:outline-none"
          />
          <input
            type="text"
            placeholder="Nombre"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            className="rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-slate-500 focus:outline-none"
          />
          <input
            type="text"
            placeholder="Nro Candidato (ej. CAND-001)"
            value={nroCandidato}
            onChange={(e) => setNroCandidato(e.target.value)}
            className="rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-slate-500 focus:outline-none"
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
              onClick={handleLimpiar}
              className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-50"
            >
              Limpiar
            </button>
          </div>
        </form>
      </section>

      {loading && (
        <div className="rounded-xl border border-slate-200 bg-white p-8 text-center text-sm text-slate-500 shadow-xs">
          Cargando candidatos...
        </div>
      )}

      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          Error al conectar con la API ({API_URL}): {error}
        </div>
      )}

      {!loading && !error && (
        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xs">
          <div className="flex items-center justify-between border-b border-slate-200 px-5 py-3.5">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Resultados ({candidatos.length})
            </span>
            <span className="text-xs text-slate-400">{API_URL}</span>
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
              <thead className="bg-slate-50 text-xs font-semibold uppercase tracking-wider text-slate-500">
                <tr>
                  <th className="px-5 py-3">Nro</th>
                  <th className="px-5 py-3">Apellido y Nombre</th>
                  <th className="px-5 py-3">Documento</th>
                  <th className="px-5 py-3">Escolaridad</th>
                  <th className="px-5 py-3">Email</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {candidatos.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-5 py-8 text-center text-slate-500">
                      No se encontraron candidatos con esos filtros.
                    </td>
                  </tr>
                ) : (
                  candidatos.map((c) => (
                    <tr key={c.id} className="transition hover:bg-slate-50/80">
                      <td className="px-5 py-3.5 font-mono text-xs font-medium text-slate-700">
                        {c.nroCandidato}
                      </td>
                      <td className="px-5 py-3.5 font-medium text-slate-900">
                        {c.apellido}, {c.nombre}
                      </td>
                      <td className="px-5 py-3.5 text-slate-600">
                        {c.tipoDocumento} {c.nroDocumento}
                      </td>
                      <td className="px-5 py-3.5">
                        <span className="inline-flex rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-700">
                          {c.escolaridad ?? '-'}
                        </span>
                      </td>
                      <td className="px-5 py-3.5 text-slate-600">{c.email ?? '-'}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  );
}
