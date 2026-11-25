'use client';

import { useEffect, useState } from 'react';
import { Modal } from '@/components/Modal';

interface Competencia {
  id: number;
  codigo?: string;
  nombre: string;
}

interface CompetenciaAsociadaForm {
  competenciaId: string;
  ponderacionMinima: string;
}

interface Puesto {
  id: number;
  codigo: string;
  nombre: string;
  empresa: string;
  descripcion?: string;
  competencias?: Array<{
    id?: number;
    ponderacionMinima?: number;
    competencia?: Competencia;
  }>;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

export default function PuestosPage() {
  const [puestos, setPuestos] = useState<Puesto[]>([]);
  const [competencias, setCompetencias] = useState<Competencia[]>([]);
  const [filtroCodigo, setFiltroCodigo] = useState('');
  const [filtroNombre, setFiltroNombre] = useState('');
  const [filtroEmpresa, setFiltroEmpresa] = useState('');
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modal alta puesto
  const [modalAbierto, setModalAbierto] = useState(false);
  const [codigo, setCodigo] = useState('');
  const [nombre, setNombre] = useState('');
  const [empresa, setEmpresa] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [itemsCompetencia, setItemsCompetencia] = useState<CompetenciaAsociadaForm[]>([
    { competenciaId: '', ponderacionMinima: '5' },
  ]);
  const [guardando, setGuardando] = useState(false);

  // Modal alerta / error
  const [alerta, setAlerta] = useState<{
    open: boolean;
    title: string;
    description: string;
    variant: 'default' | 'danger' | 'success';
  }>({ open: false, title: '', description: '', variant: 'default' });

  async function cargarDatos() {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      if (filtroCodigo.trim()) params.set('codigo', filtroCodigo.trim());
      if (filtroNombre.trim()) params.set('nombre', filtroNombre.trim());
      if (filtroEmpresa.trim()) params.set('empresa', filtroEmpresa.trim());

      const [resPuestos, resComp] = await Promise.all([
        fetch(`${API_URL}/api/catalogo/puestos${params.toString() ? `?${params.toString()}` : ''}`),
        fetch(`${API_URL}/api/catalogo/competencias`),
      ]);

      if (!resPuestos.ok) throw new Error(`Error al cargar puestos (${resPuestos.status})`);
      if (!resComp.ok) throw new Error(`Error al cargar competencias (${resComp.status})`);

      const dataPuestos: Puesto[] = await resPuestos.json();
      const dataComp: Competencia[] = await resComp.json();
      setPuestos(dataPuestos);
      setCompetencias(dataComp);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al consultar el catálogo');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    cargarDatos();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function abrirAlta() {
    setCodigo('');
    setNombre('');
    setEmpresa('');
    setDescripcion('');
    setItemsCompetencia([
      { competenciaId: competencias[0]?.id ? String(competencias[0].id) : '', ponderacionMinima: '5' },
    ]);
    setModalAbierto(true);
  }

  function agregarFilaCompetencia() {
    setItemsCompetencia((prev) => [
      ...prev,
      { competenciaId: competencias[0]?.id ? String(competencias[0].id) : '', ponderacionMinima: '5' },
    ]);
  }

  function actualizarFilaCompetencia(
    index: number,
    campo: keyof CompetenciaAsociadaForm,
    valor: string
  ) {
    setItemsCompetencia((prev) =>
      prev.map((fila, idx) => (idx === index ? { ...fila, [campo]: valor } : fila))
    );
  }

  function quitarFilaCompetencia(index: number) {
    setItemsCompetencia((prev) => (prev.length > 1 ? prev.filter((_, idx) => idx !== index) : prev));
  }

  async function guardarPuesto(e: React.FormEvent) {
    e.preventDefault();

    const faltantes: string[] = [];
    if (!codigo.trim()) faltantes.push('Código');
    if (!nombre.trim()) faltantes.push('Nombre (*)');
    if (!empresa.trim()) faltantes.push('Empresa (*)');
    if (!descripcion.trim()) faltantes.push('Descripción');

    const competenciasValidas = itemsCompetencia
      .filter((c) => c.competenciaId && c.ponderacionMinima !== '')
      .map((c) => ({
        competenciaId: Number(c.competenciaId),
        ponderacionMinima: Number(c.ponderacionMinima),
      }));

    if (competenciasValidas.length === 0) {
      faltantes.push('Competencias asociadas');
    }

    if (faltantes.length > 0) {
      setAlerta({
        open: true,
        title: 'Omisión en Campos',
        description: `Los siguientes campos son obligatorios: [${faltantes.join(', ')}]. Es necesario completarlos para continuar.`,
        variant: 'danger',
      });
      return;
    }

    setGuardando(true);
    try {
      const res = await fetch(`${API_URL}/api/catalogo/puestos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          codigo: codigo.trim(),
          nombre: nombre.trim(),
          empresa: empresa.trim(),
          descripcion: descripcion.trim(),
          competencias: competenciasValidas,
        }),
      });

      const data = await res.json().catch(() => null);
      if (!res.ok) {
        const msg =
          data?.message ||
          (Array.isArray(data?.errors) ? data.errors.join(' · ') : null) ||
          `Error al guardar el puesto (${res.status})`;
        setAlerta({
          open: true,
          title: 'No se pudo guardar el puesto',
          description: msg,
          variant: 'danger',
        });
        return;
      }

      setModalAbierto(false);
      setAlerta({
        open: true,
        title: 'Puesto creado',
        description: `El puesto "${data?.nombre ?? nombre.trim()}" se registró correctamente.`,
        variant: 'success',
      });
      await cargarDatos();
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

  const puestoSeleccionado = puestos.find((p) => p.id === selectedId) ?? null;

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Puestos</h1>
          <p className="mt-1 text-sm text-slate-500">
            Administrá los puestos por empresa y sus competencias mínimas requeridas.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={abrirAlta}
            className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white shadow-xs transition hover:bg-slate-800"
          >
            Nuevo
          </button>
          <button
            type="button"
            disabled={!puestoSeleccionado}
            onClick={() =>
              setAlerta({
                open: true,
                title: 'Edición de puesto',
                description: `Seleccionaste "${puestoSeleccionado?.nombre}". La edición directa de puestos se conectará en el próximo paso.`,
                variant: 'default',
              })
            }
            className="rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-45"
          >
            Modificar
          </button>
          <button
            type="button"
            disabled={!puestoSeleccionado}
            onClick={() =>
              setAlerta({
                open: true,
                title: 'Baja de puesto',
                description: `Seleccionaste "${puestoSeleccionado?.nombre}". Podés confirmar la baja lógica en el siguiente bloque.`,
                variant: 'danger',
              })
            }
            className="rounded-lg border border-red-200 bg-red-50 px-3.5 py-2 text-sm font-medium text-red-700 transition hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-45"
          >
            Eliminar
          </button>
        </div>
      </div>

      <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-xs">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            cargarDatos();
          }}
          className="grid grid-cols-1 gap-3 sm:grid-cols-4"
        >
          <input
            type="text"
            placeholder="Código"
            value={filtroCodigo}
            onChange={(e) => setFiltroCodigo(e.target.value)}
            className="rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-slate-500 focus:outline-none"
          />
          <input
            type="text"
            placeholder="Nombre puesto"
            value={filtroNombre}
            onChange={(e) => setFiltroNombre(e.target.value)}
            className="rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-slate-500 focus:outline-none"
          />
          <input
            type="text"
            placeholder="Empresa"
            value={filtroEmpresa}
            onChange={(e) => setFiltroEmpresa(e.target.value)}
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
              onClick={() => {
                setFiltroCodigo('');
                setFiltroNombre('');
                setFiltroEmpresa('');
                setTimeout(() => cargarDatos(), 0);
              }}
              className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-50"
            >
              Limpiar
            </button>
          </div>
        </form>
      </section>

      {loading && (
        <div className="rounded-xl border border-slate-200 bg-white p-8 text-center text-sm text-slate-500 shadow-xs">
          Cargando puestos...
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
              Resultados búsqueda ({puestos.length})
            </span>
            {puestoSeleccionado && (
              <span className="text-xs font-medium text-slate-700">
                Seleccionado: {puestoSeleccionado.codigo} — {puestoSeleccionado.nombre}
              </span>
            )}
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
              <thead className="bg-slate-50 text-xs font-semibold uppercase tracking-wider text-slate-500">
                <tr>
                  <th className="px-4 py-3 w-12">Sel.</th>
                  <th className="px-5 py-3">Código</th>
                  <th className="px-5 py-3">Nombre Puesto</th>
                  <th className="px-5 py-3">Empresa</th>
                  <th className="px-5 py-3">Competencias</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {puestos.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-5 py-8 text-center text-slate-500">
                      No hay puestos registrados con esos filtros. Podés crear uno con &ldquo;Nuevo&rdquo;.
                    </td>
                  </tr>
                ) : (
                  puestos.map((p) => {
                    const checked = selectedId === p.id;
                    return (
                      <tr
                        key={p.id}
                        onClick={() => setSelectedId(checked ? null : p.id)}
                        className={`cursor-pointer transition ${
                          checked ? 'bg-rose-50/80' : 'hover:bg-slate-50/80'
                        }`}
                      >
                        <td className="px-4 py-3.5">
                          <input
                            type="checkbox"
                            checked={checked}
                            onChange={() => setSelectedId(checked ? null : p.id)}
                            onClick={(e) => e.stopPropagation()}
                            className="h-4 w-4 rounded border-slate-300 text-slate-900 focus:ring-slate-500"
                          />
                        </td>
                        <td className="px-5 py-3.5 font-mono text-xs font-medium text-slate-700">
                          {p.codigo}
                        </td>
                        <td className="px-5 py-3.5 font-medium text-slate-900">{p.nombre}</td>
                        <td className="px-5 py-3.5 text-slate-600">{p.empresa}</td>
                        <td className="px-5 py-3.5">
                          <div className="flex flex-wrap gap-1.5">
                            {(p.competencias ?? []).length === 0 ? (
                              <span className="text-xs text-slate-400">Sin detalle</span>
                            ) : (
                              (p.competencias ?? []).map((comp, i) => (
                                <span
                                  key={comp.id ?? i}
                                  className="inline-flex rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-700"
                                >
                                  {comp.competencia?.nombre ?? 'Competencia'}
                                  {comp.ponderacionMinima !== undefined
                                    ? ` (${comp.ponderacionMinima})`
                                    : ''}
                                </span>
                              ))
                            )}
                          </div>
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
        open={modalAbierto}
        title="ALTA DE PUESTO"
        description="Completá los datos del puesto y asigná al menos una competencia con ponderación mínima."
        confirmLabel={guardando ? 'Guardando...' : 'Aceptar'}
        cancelLabel="Cancelar"
        onClose={() => setModalAbierto(false)}
        onConfirm={() => {
          const form = document.getElementById('form-alta-puesto') as HTMLFormElement | null;
          form?.requestSubmit();
        }}
      >
        <form id="form-alta-puesto" onSubmit={guardarPuesto} className="space-y-4">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label className="mb-1 block text-xs font-semibold text-slate-700">Código</label>
              <input
                type="text"
                placeholder="Ej. PUE-0125"
                value={codigo}
                onChange={(e) => setCodigo(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-slate-700">Empresa (*)</label>
              <input
                type="text"
                placeholder="Ej. Empresa A"
                value={empresa}
                onChange={(e) => setEmpresa(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
              />
            </div>
          </div>

          <div>
            <label className="mb-1 block text-xs font-semibold text-slate-700">Nombre (*)</label>
            <input
              type="text"
              placeholder="Ej. Desarrollador Backend Senior"
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="mb-1 block text-xs font-semibold text-slate-700">Descripción</label>
            <textarea
              rows={2}
              placeholder="Responsabilidades principales del puesto..."
              value={descripcion}
              onChange={(e) => setDescripcion(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
            />
          </div>

          <div className="space-y-2 rounded-lg border border-slate-200 bg-slate-50/70 p-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-700">Competencias asociadas</span>
              <button
                type="button"
                onClick={agregarFilaCompetencia}
                className="rounded-md bg-slate-900 px-2.5 py-1 text-xs font-medium text-white hover:bg-slate-800"
              >
                + Agregar
              </button>
            </div>

            {itemsCompetencia.map((fila, idx) => (
              <div key={idx} className="grid grid-cols-12 gap-2">
                <select
                  value={fila.competenciaId}
                  onChange={(e) => actualizarFilaCompetencia(idx, 'competenciaId', e.target.value)}
                  className="col-span-7 rounded-lg border border-slate-300 bg-white px-2.5 py-1.5 text-xs text-slate-900"
                >
                  <option value="">Seleccionar competencia</option>
                  {competencias.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.nombre}
                    </option>
                  ))}
                </select>
                <input
                  type="number"
                  step="0.5"
                  min="0"
                  max="10"
                  placeholder="Pond."
                  value={fila.ponderacionMinima}
                  onChange={(e) => actualizarFilaCompetencia(idx, 'ponderacionMinima', e.target.value)}
                  className="col-span-3 rounded-lg border border-slate-300 bg-white px-2.5 py-1.5 text-xs text-slate-900"
                />
                <button
                  type="button"
                  onClick={() => quitarFilaCompetencia(idx)}
                  className="col-span-2 rounded-lg border border-slate-300 bg-white text-xs font-semibold text-slate-600 hover:bg-slate-100"
                >
                  ×
                </button>
              </div>
            ))}
          </div>
        </form>
      </Modal>

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
