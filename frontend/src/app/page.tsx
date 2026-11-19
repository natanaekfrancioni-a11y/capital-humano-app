'use client';

import { useEffect, useState } from 'react';

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

  return (
    <main className="mx-auto max-w-5xl p-6 space-y-6">
      <header className="space-y-1">
        <h1 className="text-2xl font-bold">Capital Humano — Candidatos</h1>
        <p className="text-sm text-gray-500">Conectado a {API_URL}/api/evaluaciones/candidatos</p>
      </header>

      <form onSubmit={handleSubmit} className="grid grid-cols-1 gap-3 sm:grid-cols-4">
        <input
          type="text"
          placeholder="Apellido"
          value={apellido}
          onChange={(e) => setApellido(e.target.value)}
          className="rounded border px-3 py-2 text-sm"
        />
        <input
          type="text"
          placeholder="Nombre"
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          className="rounded border px-3 py-2 text-sm"
        />
        <input
          type="text"
          placeholder="Nro Candidato (ej. CAND-001)"
          value={nroCandidato}
          onChange={(e) => setNroCandidato(e.target.value)}
          className="rounded border px-3 py-2 text-sm"
        />
        <button
          type="submit"
          className="rounded bg-black px-4 py-2 text-sm font-medium text-white hover:bg-gray-800"
        >
          Buscar
        </button>
      </form>

      {loading && <p className="text-sm text-gray-500">Cargando candidatos...</p>}
      {error && <p className="text-sm text-red-600">Error: {error}</p>}

      {!loading && !error && (
        <div className="overflow-x-auto rounded border">
          <table className="min-w-full divide-y text-left text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-2">Nro</th>
                <th className="px-4 py-2">Apellido y Nombre</th>
                <th className="px-4 py-2">Documento</th>
                <th className="px-4 py-2">Escolaridad</th>
                <th className="px-4 py-2">Email</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {candidatos.map((c) => (
                <tr key={c.id}>
                  <td className="px-4 py-2 font-mono">{c.nroCandidato}</td>
                  <td className="px-4 py-2">{c.apellido}, {c.nombre}</td>
                  <td className="px-4 py-2">{c.tipoDocumento} {c.nroDocumento}</td>
                  <td className="px-4 py-2">{c.escolaridad ?? '-'}</td>
                  <td className="px-4 py-2">{c.email ?? '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </main>
  );
}
