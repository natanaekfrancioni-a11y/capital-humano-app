'use client';

import { useEffect, useRef, useState, type FormEvent } from 'react';
import { BloqueDatos } from '@/components/reportes/Encabezado';
import { PantallaConsultor } from '@/components/shell/PantallaConsultor';
import { useSalir } from '@/components/shell/useSalir';
import { useVolver } from '@/components/shell/Volver';
import { Boton } from '@/components/ui/Boton';
import { CampoTexto } from '@/components/ui/Campo';
import { useAvisos } from '@/components/ui/Dialogo';
import { Grilla, type Columna } from '@/components/ui/Grilla';
import { BarraAcciones, Hoja, Seccion } from '@/components/ui/Hoja';
import { ApiError, api } from '@/lib/api';
import { estado, fecha, fechaHora, puntaje } from '@/lib/formato';
import type { EvaluacionesPuesto, FilaOrdenMerito, PuestoResumen, ReporteOrdenMerito, SesionConsultor } from '@/lib/tipos';

export default function OrdenMeritoPage() {
  return <PantallaConsultor>{(sesion) => <OrdenMerito sesion={sesion} />}</PantallaConsultor>;
}

/** CU028 - Emitir orden de mérito */
function OrdenMerito({ sesion }: { sesion: SesionConsultor }) {
  const avisos = useAvisos();
  const salir = useSalir();
  const [filtros, setFiltros] = useState({ codigo: '', nombre: '', empresa: '' });
  const [puestos, setPuestos] = useState<PuestoResumen[] | null>(null);
  const [seleccion, setSeleccion] = useState<number | null>(null);
  const [buscando, setBuscando] = useState(false);
  const [evaluaciones, setEvaluaciones] = useState<EvaluacionesPuesto | null>(null);
  const [reporte, setReporte] = useState<ReporteOrdenMerito | null>(null);
  const [emitiendo, setEmitiendo] = useState(false);

  const puesto = puestos?.find((p) => p.id === seleccion) ?? null;

  async function buscar(e: FormEvent) {
    e.preventDefault();
    if (!Object.values(filtros).some((v) => v.trim())) {
      await avisos.validacion('Falta un criterio de búsqueda', 'Se debe ingresar al menos un criterio: código, nombre del puesto o empresa.');
      return;
    }
    setBuscando(true);
    try {
      const r = await api.buscarPuestosConEvaluaciones(filtros);
      setPuestos(r);
      setSeleccion((s) => (r.some((p) => p.id === s) ? s : null));
    } catch (err) {
      await avisos.error(err, 'No se pudo realizar la búsqueda');
    } finally {
      setBuscando(false);
    }
  }

  // se piden las evaluaciones del puesto, si no tiene se avisa y se sigue en la lista de puestos
  async function emitir() {
    if (!puesto) {
      await avisos.validacion('Selección obligatoria', 'Debe seleccionar un puesto de la lista para emitir el orden de mérito.');
      return;
    }
    try {
      setEvaluaciones(await api.evaluacionesPuesto(puesto.id));
    } catch (err) {
      if (err instanceof ApiError && !err.sinConexion) {
        await avisos.informar('Puesto sin evaluaciones', err.message);
      } else {
        await avisos.error(err, 'No se pudieron obtener las evaluaciones del puesto');
      }
    }
  }

  async function generar(evaluacion: string) {
    if (!puesto) return;
    setEmitiendo(true);
    try {
      setReporte(await api.ordenDeMerito(puesto.id, evaluacion, sesion.nombreCompleto));
      setEvaluaciones(null);
    } catch (err) {
      await avisos.error(err, 'No se pudo emitir el orden de mérito');
    } finally {
      setEmitiendo(false);
    }
  }

  async function cancelar() {
    if (seleccion !== null) {
      const ok = await avisos.confirmar('Confirmación de salida', '¿Está seguro que desea cancelar la operación? Se perderá la selección actual.', {
        aceptar: 'Sí, salir',
        rechazar: 'No',
      });
      if (!ok) return;
    }
    salir();
  }

  if (reporte) {
    return <Reporte reporte={reporte} alCerrar={salir} alVolver={() => setReporte(null)} />;
  }

  return (
    <Hoja titulo="Orden de mérito" cu="CU028" bajada="Buscar el puesto o función para el que se quiere emitir el orden de mérito.">
      <form onSubmit={buscar} className="grid items-end gap-4 md:grid-cols-[0.7fr_1.2fr_1fr_auto]">
        <CampoTexto etiqueta="Código" valor={filtros.codigo} alCambiar={(v) => setFiltros((f) => ({ ...f, codigo: v }))} maxLength={30} />
        <CampoTexto etiqueta="Nombre del puesto" valor={filtros.nombre} alCambiar={(v) => setFiltros((f) => ({ ...f, nombre: v }))} maxLength={50} />
        <CampoTexto etiqueta="Empresa" valor={filtros.empresa} alCambiar={(v) => setFiltros((f) => ({ ...f, empresa: v }))} maxLength={50} />
        <Boton type="submit" ocupado={buscando} textoOcupado="Buscando…">
          Buscar
        </Boton>
      </form>

      <div className="mt-7">
        <Grilla
          etiqueta="Puestos encontrados"
          columnas={[
            { titulo: 'Código', celda: (p: PuestoResumen) => p.codigo, dato: true, ancho: '9rem' },
            { titulo: 'Nombre', celda: (p: PuestoResumen) => p.nombre, dato: true },
            { titulo: 'Empresa', celda: (p: PuestoResumen) => p.empresa, dato: true },
            { titulo: 'Candidatos', celda: (p: PuestoResumen) => p.candidatos, cifras: true, alinear: 'derecha', ancho: '7rem' },
            {
              titulo: 'Evaluaciones completas',
              celda: (p: PuestoResumen) => p.evaluacionesCompletas,
              cifras: true,
              alinear: 'derecha',
              ancho: '11rem',
            },
          ]}
          filas={puestos ?? []}
          clave={(p) => p.id}
          seleccion={{
            modo: 'unica',
            nombre: 'puesto',
            seleccionadas: seleccion === null ? [] : [seleccion],
            alCambiar: (ids) => setSeleccion(Number(ids[0])),
            etiquetaFila: (p) => `Seleccionar el puesto ${p.nombre}`,
          }}
          vacio={puestos === null ? 'Cargue al menos un criterio y presione Buscar.' : 'No hay puestos que coincidan.'}
        />
      </div>

      <BarraAcciones
        izquierda={<Boton onClick={cancelar}>Cancelar</Boton>}
        derecha={
          <Boton variante="principal" disabled={!puesto} onClick={emitir}>
            Emitir orden de mérito
          </Boton>
        }
      />

      {evaluaciones && puesto && (
        <SeleccionarEvaluacion
          datos={evaluaciones}
          ocupado={emitiendo}
          alCancelar={() => setEvaluaciones(null)}
          alAceptar={generar}
        />
      )}
    </Hoja>
  );
}

/** pantalla 2: elegir para qué evaluaciones se emite el orden de mérito, o todas (I6) */
function SeleccionarEvaluacion({
  datos,
  ocupado,
  alCancelar,
  alAceptar,
}: {
  datos: EvaluacionesPuesto;
  ocupado: boolean;
  alCancelar: () => void;
  alAceptar: (evaluacion: string) => void;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const todosLosCodigos = datos.evaluaciones.map((e) => e.codigo);
  const [elegidas, setElegidas] = useState<string[]>(todosLosCodigos);
  const [errorSeleccion, setErrorSeleccion] = useState<string | null>(null);

  const todas = todosLosCodigos.length > 0 && elegidas.length === todosLosCodigos.length;

  useEffect(() => {
    const d = ref.current;
    if (d && !d.open) d.showModal();
    return () => d?.close();
  }, []);

  function alternarTodas(marcado: boolean) {
    setErrorSeleccion(null);
    setElegidas(marcado ? todosLosCodigos : []);
  }

  function alternarEvaluacion(codigo: string) {
    setErrorSeleccion(null);
    setElegidas((actuales) =>
      actuales.includes(codigo) ? actuales.filter((c) => c !== codigo) : [...actuales, codigo],
    );
  }

  function confirmar(e: FormEvent) {
    e.preventDefault();
    if (elegidas.length === 0) {
      setErrorSeleccion('Debe seleccionar al menos una evaluación.');
      return;
    }
    if (todas) {
      alAceptar('TODAS');
      return;
    }
    const ordenadas = todosLosCodigos.filter((cod) => elegidas.includes(cod));
    alAceptar(ordenadas.join(','));
  }

  return (
    <dialog
      ref={ref}
      aria-labelledby="sel-evaluacion"
      onCancel={(e) => {
        e.preventDefault();
        alCancelar();
      }}
      className="m-auto w-[min(94vw,34rem)] rounded-[6px] border border-linea bg-papel p-0 text-grafito shadow-[0_18px_50px_rgba(37,39,45,0.22)] backdrop:bg-grafito/45"
    >
      <form onSubmit={confirmar} className="px-6 pb-5 pt-5">
        <h2 id="sel-evaluacion" className="regla-doble pb-2 text-subtitulo font-extrabold">
          Seleccionar evaluación
        </h2>
        <p className="mt-3 text-chica text-grafito-claro">Puesto seleccionado</p>
        <p className="dato font-bold">
          {datos.puestoCodigo} {datos.puestoNombre}
        </p>
        <fieldset className="mt-4">
          <legend className="sr-only">Evaluaciones del puesto</legend>
          <div className="max-h-[50vh] divide-y divide-linea overflow-y-auto border-y border-linea">
            <label className="flex cursor-pointer items-start gap-3 px-1 py-3 hover:bg-rosa-suave">
              <input
                type="checkbox"
                className="burbuja mt-0.5"
                checked={todas}
                onChange={(e) => alternarTodas(e.target.checked)}
              />
              <span>
                <span className="block font-bold">Todas las evaluaciones</span>
                <span className="cifras block text-chica text-grafito-claro">
                  {datos.totalCandidatos} candidatos, {datos.totalCompletos} cuestionarios completos
                </span>
              </span>
            </label>

            {datos.evaluaciones.map((e) => (
              <label key={e.codigo} className="flex cursor-pointer items-start gap-3 px-1 py-3 hover:bg-rosa-suave">
                <input
                  type="checkbox"
                  className="burbuja mt-0.5"
                  checked={elegidas.includes(e.codigo)}
                  onChange={() => alternarEvaluacion(e.codigo)}
                />
                <span>
                  <span className="block font-bold">Evaluación {e.codigo}</span>
                  <span className="cifras block text-chica text-grafito-claro">
                    Generada el {fecha(e.fechaGeneracion)}, {e.candidatos} candidatos, {e.completos} completos
                  </span>
                </span>
              </label>
            ))}
          </div>
          {errorSeleccion && (
            <p role="alert" className="mt-2 text-chica font-bold text-error">
              {errorSeleccion}
            </p>
          )}
        </fieldset>
        <div className="mt-6 flex justify-end gap-2.5">
          <Boton onClick={alCancelar}>Cancelar</Boton>
          <Boton type="submit" variante="principal" ocupado={ocupado} textoOcupado="Emitiendo…">
            Aceptar
          </Boton>
        </div>
      </form>
    </dialog>
  );
}

const documento = (f: FilaOrdenMerito) => `${f.tipoDocumento} ${f.nroDocumento}`;
const apellidoNombre = (f: FilaOrdenMerito) => `${f.apellido}, ${f.nombre}`;
const guion = (v: string | null) => (v ? fecha(v) : '-');

/** pantalla 3: reporte de orden de mérito listo para imprimir */
function Reporte({ reporte, alCerrar, alVolver }: { reporte: ReporteOrdenMerito; alCerrar: () => void; alVolver: () => void }) {
  useVolver(alVolver);

  const comunes: Columna<FilaOrdenMerito>[] = [
    { titulo: 'Documento', celda: documento, cifras: true, ancho: '9.5rem' },
    { titulo: 'Apellido y nombre', celda: apellidoNombre, dato: true },
  ];
  const tiempos: Columna<FilaOrdenMerito>[] = [
    { titulo: 'Inicio', celda: (f) => guion(f.fechaInicio), cifras: true, ancho: '7rem' },
    { titulo: 'Fin', celda: (f) => guion(f.fechaFin), cifras: true, ancho: '7rem' },
    { titulo: 'Accesos', celda: (f) => f.cantidadAccesos, cifras: true, alinear: 'derecha', ancho: '5rem' },
  ];
  const puntajeCol: Columna<FilaOrdenMerito> = {
    titulo: 'Puntaje',
    celda: (f) => <span className="font-bold">{puntaje(f.puntaje)}</span>,
    cifras: true,
    alinear: 'derecha',
    ancho: '6rem',
  };

  return (
    <Hoja titulo="Orden de mérito" cu="CU028">
      <div className="grid gap-6 sm:grid-cols-2">
        <BloqueDatos
          titulo="Función"
          filas={[
            ['Empresa', reporte.empresa],
            ['Puesto', `${reporte.puestoCodigo} ${reporte.puestoNombre}`],
          ]}
        />
        <BloqueDatos
          titulo="Evaluación"
          filas={[
            ['Evaluación', reporte.evaluacion === 'TODAS' ? 'Todas' : reporte.evaluacion],
            ['Fecha de emisión', <span key="f" className="cifras">{fechaHora(reporte.fechaEmision)}</span>],
            ['Emitido por', reporte.emitidoPor],
          ]}
        />
      </div>
      <p className="mt-4 text-chica text-grafito-claro">
        Ponderación mínima requerida:{' '}
        {reporte.competencias.map((c, i) => (
          <span key={c.nombre}>
            {i > 0 && '; '}
            <span className="dato">{c.nombre}</span> <span className="cifras">{puntaje(c.ponderacionMinima)}</span>
          </span>
        ))}
        .
      </p>

      <div className="mt-8 grid gap-9">
        <Seccion titulo="Cumplen las ponderaciones mínimas">
          <Grilla
            etiqueta="Orden de mérito: candidatos con cuestionario completo que alcanzan las ponderaciones mínimas, ordenados por puntaje"
            columnas={[
              { titulo: '#', celda: (f) => f.posicion, cifras: true, alinear: 'derecha', ancho: '3rem' },
              ...comunes,
              puntajeCol,
              ...tiempos,
            ]}
            filas={reporte.ordenMerito}
            clave={(f) => f.cuestionarioId}
            vacio="Ningún candidato alcanzó las ponderaciones mínimas del puesto."
          />
        </Seccion>

        <Seccion titulo="No alcanzan las ponderaciones mínimas">
          <Grilla
            etiqueta="Candidatos con cuestionario completo que no alcanzan las ponderaciones mínimas"
            columnas={[...comunes, puntajeCol, ...tiempos]}
            filas={reporte.noAlcanzanMinimo}
            clave={(f) => f.cuestionarioId}
            vacio="No hay candidatos en esta situación."
          />
        </Seccion>

        <Seccion titulo="Cuestionarios no completados">
          <Grilla
            etiqueta="Candidatos con cuestionarios activos, en proceso, incompletos o sin contestar, agrupados por estado"
            columnas={[
              { titulo: 'Estado', celda: (f) => <span className="font-bold">{estado(f.estado)}</span>, ancho: '8rem' },
              ...comunes,
              { titulo: 'Inicio', celda: (f) => guion(f.fechaInicio), cifras: true, ancho: '7rem' },
              { titulo: 'Último ingreso', celda: (f) => guion(f.fechaUltimoAcceso), cifras: true, ancho: '8rem' },
              { titulo: 'Accesos', celda: (f) => f.cantidadAccesos, cifras: true, alinear: 'derecha', ancho: '5rem' },
            ]}
            filas={reporte.restantes}
            clave={(f) => f.cuestionarioId}
            vacio="Todos los cuestionarios están completos."
          />
        </Seccion>
      </div>

      <BarraAcciones
        izquierda={
          <>
            <Boton onClick={alCerrar}>Cancelar</Boton>
            <Boton onClick={alVolver}>Elegir otro puesto</Boton>
          </>
        }
        derecha={
          <Boton variante="principal" onClick={() => window.print()}>
            Imprimir
          </Boton>
        }
      />
    </Hoja>
  );
}
