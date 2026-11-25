'use client';

import { useCallback, useEffect, useId, useMemo, useState, type FormEvent } from 'react';
import { BloqueDatos } from '@/components/reportes/Encabezado';
import { GraficoComparativo } from '@/components/reportes/GraficoComparativo';
import { PantallaConsultor } from '@/components/shell/PantallaConsultor';
import { useSalir } from '@/components/shell/useSalir';
import { useVolver } from '@/components/shell/Volver';
import { Boton } from '@/components/ui/Boton';
import { CampoTexto } from '@/components/ui/Campo';
import { useAvisos } from '@/components/ui/Dialogo';
import { Grilla } from '@/components/ui/Grilla';
import { BarraAcciones, Hoja, Seccion } from '@/components/ui/Hoja';
import { ApiError, api } from '@/lib/api';
import { diferencia, fecha, puntaje } from '@/lib/formato';
import type { CandidatoEvaluado, ItemComparativo, Puesto, ReporteComparativo as Reporte } from '@/lib/tipos';

export default function ReporteComparativoPage() {
  return <PantallaConsultor>{() => <ReporteComparativo />}</PantallaConsultor>;
}

const contiene = (valor: string, filtro: string) => !filtro.trim() || valor.toUpperCase().includes(filtro.trim().toUpperCase());

/** CU027 - Emitir reporte comparativo */
function ReporteComparativo() {
  const avisos = useAvisos();
  const salir = useSalir();
  const empresasId = useId();
  const puestosId = useId();
  const [todos, setTodos] = useState<Puesto[]>([]);
  const [empresa, setEmpresa] = useState('');
  const [nombrePuesto, setNombrePuesto] = useState('');
  const [funciones, setFunciones] = useState<Puesto[] | null>(null);
  const [puestoId, setPuestoId] = useState<number | null>(null);
  const [candidatos, setCandidatos] = useState<CandidatoEvaluado[] | null>(null);
  const [cuestionarioId, setCuestionarioId] = useState<number | null>(null);
  const [reporte, setReporte] = useState<Reporte | null>(null);
  const [comparando, setComparando] = useState(false);

  useEffect(() => {
    api.buscarPuestos().then(setTodos).catch((e) => avisos.error(e, 'No se pudieron cargar los puestos'));
  }, [avisos]);

  const empresas = useMemo(() => [...new Set(todos.map((p) => p.empresa))].sort(), [todos]);
  const puestosDeEmpresa = useMemo(() => todos.filter((p) => contiene(p.empresa, empresa)), [todos, empresa]);

  // Pasos 2 a 4: búsqueda de funciones por nombre y empresa ("contiene").
  function buscar(e: FormEvent) {
    e.preventDefault();
    const r = todos.filter((p) => contiene(p.empresa, empresa) && contiene(p.nombre, nombrePuesto));
    setFunciones(r);
    setPuestoId(null);
    setCandidatos(null);
    setCuestionarioId(null);
  }

  // Paso 5: candidatos evaluados (cuestionario completo) en la función seleccionada.
  async function elegirFuncion(id: number) {
    setPuestoId(id);
    setCandidatos(null);
    setCuestionarioId(null);
    try {
      setCandidatos(await api.candidatosEvaluados(id));
    } catch (err) {
      setCandidatos([]);
      if (err instanceof ApiError && !err.sinConexion) await avisos.informar('Sin candidatos evaluados', err.message);
      else await avisos.error(err, 'No se pudieron obtener los candidatos evaluados');
    }
  }

  // Paso 6 / 6.A
  async function comparar() {
    if (cuestionarioId === null) {
      await avisos.validacion('Selección obligatoria', 'Debe seleccionar un candidato de la lista para generar el reporte comparativo.');
      return;
    }
    setComparando(true);
    try {
      setReporte(await api.reporteComparativo(cuestionarioId));
    } catch (err) {
      await avisos.error(err, 'No se pudo generar el reporte comparativo');
    } finally {
      setComparando(false);
    }
  }

  // 6.B
  async function cancelar() {
    if (puestoId !== null || cuestionarioId !== null) {
      const ok = await avisos.confirmar('Confirmación de salida', '¿Está seguro que desea cancelar la operación? Se perderá la selección actual.', {
        aceptar: 'Sí, salir',
        rechazar: 'No',
      });
      if (!ok) return;
    }
    salir();
  }

  const volverAlListado = useCallback(() => setReporte(null), []);

  if (reporte) {
    return <HojaReporte reporte={reporte} alVolver={volverAlListado} />;
  }

  return (
    <Hoja
      titulo="Reporte comparativo"
      cu="CU027"
      bajada="Compara lo que exige un puesto en cada competencia con lo que obtuvo un candidato evaluado."
    >
      <form onSubmit={buscar} className="grid items-end gap-4 md:grid-cols-[1fr_1fr_auto]">
        <CampoTexto etiqueta="Empresa" valor={empresa} alCambiar={setEmpresa} list={empresasId} maxLength={50} ayuda="Elija de la lista o escriba parte del nombre." />
        <datalist id={empresasId}>
          {empresas.map((e) => (
            <option key={e} value={e} />
          ))}
        </datalist>
        <CampoTexto
          etiqueta="Nombre del puesto"
          valor={nombrePuesto}
          alCambiar={setNombrePuesto}
          list={puestosId}
          maxLength={50}
          disabled={!empresa.trim()}
          ayuda={empresa.trim() ? 'Puestos de la empresa elegida.' : 'Primero complete la empresa.'}
        />
        <datalist id={puestosId}>
          {puestosDeEmpresa.map((p) => (
            <option key={p.id} value={p.nombre} />
          ))}
        </datalist>
        <Boton type="submit" className="md:mb-[1.6rem]">
          Buscar
        </Boton>
      </form>

      <div className="mt-8 grid gap-8 xl:grid-cols-[0.9fr_1.1fr]">
        <Seccion titulo="Funciones">
          <Grilla
            etiqueta="Funciones que coinciden con la búsqueda"
            columnas={[
              { titulo: 'Puesto', celda: (p: Puesto) => p.nombre, dato: true },
              { titulo: 'Empresa', celda: (p: Puesto) => p.empresa, dato: true },
            ]}
            filas={funciones ?? []}
            clave={(p) => p.id}
            seleccion={{
              modo: 'unica',
              nombre: 'funcion',
              seleccionadas: puestoId === null ? [] : [puestoId],
              alCambiar: (ids) => elegirFuncion(Number(ids[0])),
              etiquetaFila: (p) => `Seleccionar la función ${p.nombre}`,
            }}
            vacio={funciones === null ? 'Busque por empresa y nombre del puesto.' : 'No hay funciones que coincidan.'}
          />
        </Seccion>

        <Seccion titulo="Candidatos evaluados">
          <Grilla
            etiqueta="Candidatos con cuestionario completo en la función seleccionada"
            columnas={[
              { titulo: 'Apellido', celda: (c: CandidatoEvaluado) => c.apellido, dato: true },
              { titulo: 'Nombre', celda: (c: CandidatoEvaluado) => c.nombre, dato: true },
              { titulo: 'Documento', celda: (c: CandidatoEvaluado) => `${c.tipoDocumento} ${c.nroDocumento}`, cifras: true },
              { titulo: 'Nro candidato', celda: (c: CandidatoEvaluado) => c.nroCandidato, cifras: true },
              { titulo: 'Finalizó', celda: (c: CandidatoEvaluado) => fecha(c.fechaFin), cifras: true },
            ]}
            filas={candidatos ?? []}
            clave={(c) => c.cuestionarioId}
            seleccion={{
              modo: 'unica',
              nombre: 'candidato',
              seleccionadas: cuestionarioId === null ? [] : [cuestionarioId],
              alCambiar: (ids) => setCuestionarioId(Number(ids[0])),
              etiquetaFila: (c) => `Seleccionar a ${c.apellido}, ${c.nombre}`,
            }}
            vacio={puestoId === null ? 'Elija una función para ver sus candidatos evaluados.' : 'No hay candidatos evaluados en esta función.'}
          />
        </Seccion>
      </div>

      <BarraAcciones
        izquierda={<Boton onClick={cancelar}>Cancelar</Boton>}
        derecha={
          <Boton variante="principal" onClick={comparar} ocupado={comparando} textoOcupado="Comparando…" disabled={puestoId === null}>
            Comparar
          </Boton>
        }
      />
    </Hoja>
  );
}

/** Pantalla 2: tabla y gráfico, listos para imprimir. */
function HojaReporte({ reporte, alVolver }: { reporte: Reporte; alVolver: () => void }) {
  useVolver(alVolver);
  const cumplidas = reporte.competencias.filter((c) => c.cumple).length;

  return (
    <Hoja titulo="Reporte comparativo" cu="CU027">
      <div className="grid gap-6 sm:grid-cols-2">
        <BloqueDatos
          titulo="Función"
          filas={[
            ['Empresa', reporte.empresa],
            ['Puesto', reporte.puestoNombre],
          ]}
        />
        <BloqueDatos
          titulo="Candidato"
          filas={[
            ['Apellido y nombre', `${reporte.apellido}, ${reporte.nombre}`],
            [reporte.tipoDocumento, <span key="d" className="cifras">{reporte.nroDocumento}</span>],
            ['Nro candidato', <span key="n" className="cifras">{reporte.nroCandidato}</span>],
          ]}
        />
      </div>
      <p className="mt-5 text-cuerpo">
        Cumple <strong className="cifras">{cumplidas}</strong> de <strong className="cifras">{reporte.competencias.length}</strong>{' '}
        competencias del puesto. Puntaje total <strong className="cifras">{puntaje(reporte.puntajeTotal)}</strong>.
      </p>

      <div className="mt-6 grid gap-9">
        <Grilla
          etiqueta="Ponderación requerida y obtenida por competencia"
          columnas={[
            { titulo: 'Competencia', celda: (c: ItemComparativo) => c.competencia, dato: true },
            { titulo: 'Requerido', celda: (c: ItemComparativo) => puntaje(c.requerido), cifras: true, alinear: 'derecha', ancho: '7rem' },
            {
              titulo: 'Obtenido',
              celda: (c: ItemComparativo) => <span className="font-bold">{puntaje(c.obtenido)}</span>,
              cifras: true,
              alinear: 'derecha',
              ancho: '7rem',
            },
            {
              titulo: 'Diferencia',
              celda: (c: ItemComparativo) => (
                <span className={c.diferencia < 0 ? 'text-error' : 'text-cumple'}>{diferencia(c.diferencia)}</span>
              ),
              cifras: true,
              alinear: 'derecha',
              ancho: '7rem',
            },
            {
              titulo: 'Estado',
              celda: (c: ItemComparativo) => (
                <span className={`font-bold ${c.cumple ? 'text-cumple' : 'text-error'}`}>{c.cumple ? 'Cumple' : 'No cumple'}</span>
              ),
              ancho: '7rem',
            },
          ]}
          filas={reporte.competencias}
          clave={(c) => c.competencia}
        />
        <GraficoComparativo items={reporte.competencias} />
      </div>

      <BarraAcciones
        izquierda={<Boton onClick={alVolver}>Cancelar</Boton>}
        derecha={
          <Boton variante="principal" onClick={() => window.print()}>
            Imprimir
          </Boton>
        }
      />
    </Hoja>
  );
}
