'use client';

import { useRouter } from 'next/navigation';
import { useState, type FormEvent } from 'react';
import { GuardarArchivo } from '@/components/evaluar/GuardarArchivo';
import { PantallaConsultor } from '@/components/shell/PantallaConsultor';
import { Boton } from '@/components/ui/Boton';
import { CampoSeleccion, CampoTexto } from '@/components/ui/Campo';
import { useAvisos } from '@/components/ui/Dialogo';
import { Grilla } from '@/components/ui/Grilla';
import { BarraAcciones, Hoja, Seccion } from '@/components/ui/Hoja';
import { Pasos } from '@/components/ui/Pasos';
import { api } from '@/lib/api';
import { descargar, guardarEnCarpeta, libroClaves, nombreArchivoClaves, type ManejadorCarpeta } from '@/lib/excel';
import { nombreCompleto, puntaje } from '@/lib/formato';
import type { Candidato, CandidatoClave, CompetenciaPuesto, Puesto } from '@/lib/tipos';

const PASOS = ['Candidatos', 'Puesto', 'Claves de ingreso'];

export default function EvaluarPage() {
  return <PantallaConsultor>{() => <EvaluarCandidatos />}</PantallaConsultor>;
}

/** CU025 - Evaluar a candidatos */
function EvaluarCandidatos() {
  const router = useRouter();
  const avisos = useAvisos();
  const [paso, setPaso] = useState(0);

  // Paso 1: búsqueda y selección de candidatos
  const [filtros, setFiltros] = useState({ nombre: '', apellido: '', nroCandidato: '' });
  const [resultados, setResultados] = useState<Candidato[] | null>(null);
  const [conVigente, setConVigente] = useState<Set<number>>(new Set());
  const [marcadosResultado, setMarcadosResultado] = useState<number[]>([]);
  const [aEvaluar, setAEvaluar] = useState<Candidato[]>([]);
  const [marcadosEvaluar, setMarcadosEvaluar] = useState<number[]>([]);
  const [buscando, setBuscando] = useState(false);
  const [validando, setValidando] = useState(false);

  // Paso 2: puesto
  const [puestos, setPuestos] = useState<Puesto[] | null>(null);
  const [puestoId, setPuestoId] = useState('');
  const [competencias, setCompetencias] = useState<CompetenciaPuesto[] | null>(null);
  const [generando, setGenerando] = useState(false);

  // Paso 3: claves
  const [claves, setClaves] = useState<CandidatoClave[]>([]);
  const [finalizando, setFinalizando] = useState(false);
  const [guardarAbierto, setGuardarAbierto] = useState(false);

  const hayFiltro = Object.values(filtros).some((v) => v.trim() !== '');
  const puesto = puestos?.find((p) => p.id === Number(puestoId)) ?? null;
  const irAlInicio = () => router.push('/inicio');

  async function buscar(e: FormEvent) {
    e.preventDefault();
    if (!hayFiltro) return;
    setBuscando(true);
    try {
      const r = await api.buscarCandidatos(filtros);
      setResultados(r);
      setMarcadosResultado([]);
      const vigentes = r.length ? await api.validarCandidatos(r.map((c) => c.id)) : [];
      setConVigente(new Set(vigentes.map((c) => c.id)));
    } catch (err) {
      await avisos.error(err, 'No se pudo realizar la búsqueda');
    } finally {
      setBuscando(false);
    }
  }

  // 4 / 4.A: agregar a la grilla "Candidatos a evaluar"
  function agregar() {
    if (marcadosResultado.length === 0) {
      avisos.validacion('Selección requerida', 'Se debe seleccionar uno o más candidatos.');
      return;
    }
    const nuevos = (resultados ?? []).filter((c) => marcadosResultado.includes(c.id) && !aEvaluar.some((x) => x.id === c.id));
    setAEvaluar((lista) => [...lista, ...nuevos]);
    setMarcadosResultado([]);
  }

  // 5.A: quitar de la grilla "Candidatos a evaluar"
  function quitar() {
    if (marcadosEvaluar.length === 0) {
      avisos.validacion('Selección requerida', 'Se debe seleccionar uno o más candidatos de la lista a evaluar.');
      return;
    }
    setAEvaluar((lista) => lista.filter((c) => !marcadosEvaluar.includes(c.id)));
    setMarcadosEvaluar([]);
  }

  // 6 / 6.A: candidatos con cuestionarios activos o en proceso no pueden evaluarse
  async function siguienteACandidatos() {
    if (aEvaluar.length === 0) {
      await avisos.validacion('Selección requerida', 'Se debe agregar uno o más candidatos a la lista de candidatos a evaluar.');
      return;
    }
    setValidando(true);
    try {
      const vigentes = await api.validarCandidatos(aEvaluar.map((c) => c.id));
      if (vigentes.length) {
        await avisos.abrir({
          tipo: 'validacion',
          titulo: 'Cuestionarios en proceso',
          mensaje: 'Los siguientes candidatos poseen cuestionarios activos o en proceso y no pueden ser evaluados nuevamente:',
          detalles: vigentes.map((c) => nombreCompleto(c.apellido, c.nombre)),
        });
        setMarcadosEvaluar(vigentes.map((c) => c.id));
        return;
      }
      if (puestos === null) setPuestos(await api.buscarPuestos());
      setPaso(1);
    } catch (err) {
      await avisos.error(err, 'No se pudo validar a los candidatos');
    } finally {
      setValidando(false);
    }
  }

  // 8 / 9 / 9.A: al elegir el puesto se muestran sus competencias; si alguna no es evaluable, termina el CU.
  async function elegirPuesto(id: string) {
    setPuestoId(id);
    setCompetencias(null);
    if (!id) return;
    try {
      const lista = await api.competenciasPuesto(Number(id));
      setCompetencias(lista);
      const noEvaluables = lista.filter((c) => !c.evaluable);
      if (noEvaluables.length) {
        await avisos.abrir({
          tipo: 'error',
          titulo: 'Competencias no evaluables',
          mensaje: 'Las siguientes competencias no poseen factores que cumplan las condiciones para ser evaluadas:',
          detalles: noEvaluables.map((c) => c.nombre),
        });
        irAlInicio();
      }
    } catch (err) {
      await avisos.error(err, 'No se pudieron cargar las competencias del puesto');
    }
  }

  // 10 / 10.A / 11
  async function siguienteAPuesto() {
    if (!puestoId) {
      await avisos.validacion('Función no seleccionada', 'Se debe seleccionar una función o puesto para realizar la evaluación.');
      return;
    }
    setGenerando(true);
    try {
      setClaves(await api.generarClaves(aEvaluar.map((c) => c.id), Number(puestoId)));
      setPaso(2);
    } catch (err) {
      await avisos.error(err, 'No se pudieron generar las claves');
    } finally {
      setGenerando(false);
    }
  }

  // 12.A
  async function cancelarEvaluacion() {
    const ok = await avisos.confirmar('Cancelar la evaluación', 'La evaluación no se guardará y las claves generadas se descartan.', {
      aceptar: 'Sí, cancelar',
      rechazar: 'No',
    });
    if (ok) irAlInicio();
  }

  // 12 / 13 / 14: se guarda la evaluación y se ofrece exportar las claves.
  async function finalizar() {
    if (!puesto) return;
    setFinalizando(true);
    try {
      await api.guardarEvaluacion(
        claves.map((c) => c.candidatoId),
        puesto.id,
        Object.fromEntries(claves.map((c) => [c.candidatoId, c.claveIngreso])),
      );
    } catch (err) {
      setFinalizando(false);
      await avisos.error(err, 'No se pudo guardar la evaluación');
      return;
    }
    setFinalizando(false);
    await preguntarExportacion();
  }

  async function preguntarExportacion() {
    const exportar = await avisos.abrir({
      tipo: 'confirmacion',
      titulo: 'Información guardada',
      mensaje: `Se generaron ${claves.length} cuestionarios en estado Activo. ¿Desea exportar la lista de candidatos con sus claves a un archivo Excel?`,
      aceptar: 'Sí, exportar',
      rechazar: 'No',
    });
    if (exportar) setGuardarAbierto(true);
    else irAlInicio();
  }

  // 16.A: cancelar en la pantalla de carpeta vuelve a preguntar si desea exportar.
  async function cancelarGuardado() {
    setGuardarAbierto(false);
    await preguntarExportacion();
  }

  // 17 / 18
  async function guardarArchivo(carpeta: ManejadorCarpeta | null, nombre: string) {
    if (!puesto) return;
    const archivo = nombre.toLowerCase().endsWith('.xls') ? nombre : `${nombre}.xls`;
    const datos = await libroClaves(claves, puesto);
    if (carpeta) await guardarEnCarpeta(carpeta, archivo, datos);
    else descargar(archivo, datos);
    setGuardarAbierto(false);
    await avisos.informar(
      'Archivo guardado',
      carpeta ? `Se guardó ${archivo} en la carpeta ${carpeta.name}.` : `Se descargó ${archivo}.`,
    );
    irAlInicio();
  }

  return (
    <Hoja titulo={paso === 0 ? 'Evaluar a candidatos' : 'Evaluación de candidatos'} cu="CU025">
      <Pasos pasos={PASOS} actual={paso} />

      {paso === 0 && (
        <>
          <form onSubmit={buscar} className="grid items-end gap-4 md:grid-cols-[1fr_1fr_0.8fr_auto]">
            <CampoTexto etiqueta="Nombre" valor={filtros.nombre} alCambiar={(v) => setFiltros((f) => ({ ...f, nombre: v }))} maxLength={100} />
            <CampoTexto etiqueta="Apellido" valor={filtros.apellido} alCambiar={(v) => setFiltros((f) => ({ ...f, apellido: v }))} maxLength={100} />
            <CampoTexto
              etiqueta="Número de candidato"
              valor={filtros.nroCandidato}
              alCambiar={(v) => setFiltros((f) => ({ ...f, nroCandidato: v }))}
              maxLength={50}
            />
            <Boton type="submit" disabled={!hayFiltro} ocupado={buscando} textoOcupado="Buscando…">
              Buscar
            </Boton>
          </form>
          {!hayFiltro && <p className="mt-2 text-chica text-grafito-claro">Complete al menos un filtro para buscar.</p>}

          <div className="mt-8 grid gap-9">
            <Seccion
              titulo="Candidatos"
              accion={
                <Boton onClick={agregar} className="h-9 px-3.5 text-chica">
                  Agregar a evaluar
                </Boton>
              }
            >
              <Grilla
                etiqueta="Candidatos encontrados"
                columnas={[
                  { titulo: 'Apellido', celda: (c: Candidato) => c.apellido, dato: true },
                  { titulo: 'Nombre', celda: (c: Candidato) => c.nombre, dato: true },
                  { titulo: 'Nro candidato', celda: (c: Candidato) => c.nroCandidato, cifras: true },
                  {
                    titulo: 'Estado',
                    celda: (c: Candidato) =>
                      conVigente.has(c.id) ? (
                        <span className="font-bold text-tinta">Cuestionario vigente</span>
                      ) : (
                        <span className="text-grafito-claro">Disponible</span>
                      ),
                  },
                ]}
                filas={resultados ?? []}
                clave={(c) => c.id}
                seleccion={{
                  modo: 'multiple',
                  nombre: 'resultado',
                  seleccionadas: marcadosResultado,
                  alCambiar: (ids) => setMarcadosResultado(ids.map(Number)),
                  etiquetaFila: (c) => `Seleccionar a ${nombreCompleto(c.apellido, c.nombre)}`,
                }}
                vacio={resultados === null ? 'Busque candidatos con los filtros de arriba.' : 'No hay candidatos que coincidan.'}
                altoMaximo="24rem"
              />
            </Seccion>

            <Seccion
              titulo="Candidatos a evaluar"
              accion={
                <Boton onClick={quitar} className="h-9 px-3.5 text-chica">
                  Quitar
                </Boton>
              }
            >
              <Grilla
                etiqueta="Candidatos a evaluar"
                columnas={[
                  { titulo: 'Apellido', celda: (c: Candidato) => c.apellido, dato: true },
                  { titulo: 'Nombre', celda: (c: Candidato) => c.nombre, dato: true },
                  { titulo: 'Nro candidato', celda: (c: Candidato) => c.nroCandidato, cifras: true },
                ]}
                filas={aEvaluar}
                clave={(c) => c.id}
                seleccion={{
                  modo: 'multiple',
                  nombre: 'a-evaluar',
                  seleccionadas: marcadosEvaluar,
                  alCambiar: (ids) => setMarcadosEvaluar(ids.map(Number)),
                  etiquetaFila: (c) => `Seleccionar a ${nombreCompleto(c.apellido, c.nombre)} para quitarlo`,
                }}
                vacio="Todavía no se agregaron candidatos."
                pie={aEvaluar.length ? `${aEvaluar.length} candidatos a evaluar.` : undefined}
                altoMaximo="24rem"
              />
            </Seccion>
          </div>

          <BarraAcciones
            izquierda={<Boton onClick={irAlInicio}>Cancelar</Boton>}
            derecha={
              <Boton variante="principal" onClick={siguienteACandidatos} ocupado={validando} textoOcupado="Validando…">
                Siguiente
              </Boton>
            }
          />
        </>
      )}

      {paso === 1 && (
        <>
          <div className="grid gap-8 lg:grid-cols-[minmax(0,27rem)_1fr]">
            <div className="grid content-start gap-5">
              <CampoSeleccion
                etiqueta="Función o puesto"
                obligatorio
                valor={puestoId}
                alCambiar={elegirPuesto}
                opciones={(puestos ?? []).map((p) => ({ valor: String(p.id), texto: `${p.nombre} (${p.empresa})` }))}
                textoVacio={puestos === null ? 'Cargando puestos…' : 'Seleccionar…'}
              />
              <dl className="grid gap-1 text-chica">
                <dt className="font-bold">Estado inicial de los cuestionarios</dt>
                <dd className="flex items-center gap-2">
                  <span className="inline-block h-2.5 w-2.5 rounded-full bg-grafito" aria-hidden="true" />
                  Activo
                </dd>
                <dt className="mt-3 font-bold">Candidatos a evaluar</dt>
                <dd className="cifras">{aEvaluar.length}</dd>
              </dl>
              <p className="text-chica text-grafito-claro">
                Se generará un cuestionario por candidato con el mismo modelo de evaluación: las mismas competencias, factores,
                preguntas y opciones de respuesta.
              </p>
            </div>

            <Seccion titulo="Competencias asociadas al puesto">
              <Grilla
                etiqueta="Competencias del puesto y ponderación requerida"
                columnas={[
                  { titulo: 'Competencia', celda: (c: CompetenciaPuesto) => c.nombre, dato: true },
                  {
                    titulo: 'Ponderación requerida',
                    celda: (c: CompetenciaPuesto) => puntaje(c.ponderacionMinima),
                    cifras: true,
                    alinear: 'derecha',
                    ancho: '12rem',
                  },
                  {
                    titulo: 'Evaluable',
                    celda: (c: CompetenciaPuesto) =>
                      c.evaluable ? <span>Sí</span> : <span className="font-bold text-error">No</span>,
                    alinear: 'centro',
                    ancho: '7rem',
                  },
                ]}
                filas={competencias ?? []}
                clave={(c) => c.competenciaId}
                vacio={puestoId ? 'Cargando competencias…' : 'Elija la función o puesto para ver sus competencias.'}
              />
            </Seccion>
          </div>

          <BarraAcciones
            izquierda={
              <>
                <Boton onClick={irAlInicio}>Cancelar</Boton>
                <Boton onClick={() => setPaso(0)}>Volver a candidatos</Boton>
              </>
            }
            derecha={
              <Boton variante="principal" onClick={siguienteAPuesto} ocupado={generando} textoOcupado="Generando claves…">
                Siguiente
              </Boton>
            }
          />
        </>
      )}

      {paso === 2 && puesto && (
        <>
          <dl className="mb-6 grid gap-x-10 gap-y-1 text-cuerpo sm:grid-cols-[auto_1fr]">
            <dt className="font-bold">Función</dt>
            <dd className="dato">{puesto.nombre}</dd>
            <dt className="font-bold">Empresa</dt>
            <dd className="dato">{puesto.empresa}</dd>
          </dl>
          <Grilla
            etiqueta="Candidatos a evaluar con su clave de ingreso"
            columnas={[
              { titulo: 'Apellido', celda: (c: CandidatoClave) => c.apellido, dato: true },
              { titulo: 'Nombre', celda: (c: CandidatoClave) => c.nombre, dato: true },
              { titulo: 'Tipo documento', celda: (c: CandidatoClave) => c.tipoDocumento },
              { titulo: 'Nro documento', celda: (c: CandidatoClave) => c.nroDocumento, cifras: true },
              {
                titulo: 'Clave de ingreso',
                celda: (c: CandidatoClave) => (
                  <span className="cifras text-cuerpo font-extrabold tracking-[0.12em] text-grafito">{c.claveIngreso}</span>
                ),
              },
            ]}
            filas={claves}
            clave={(c) => c.candidatoId}
          />
          <p className="mt-3 text-chica text-grafito-claro">
            Las claves se guardan recién al presionar Finalizar. Después se puede exportar la lista a Excel.
          </p>

          <BarraAcciones
            izquierda={<Boton onClick={cancelarEvaluacion}>Cancelar</Boton>}
            derecha={
              <Boton variante="principal" onClick={finalizar} ocupado={finalizando} textoOcupado="Guardando…">
                Finalizar
              </Boton>
            }
          />

          <GuardarArchivo
            abierto={guardarAbierto}
            nombreInicial={nombreArchivoClaves(puesto.nombre)}
            alCancelar={cancelarGuardado}
            alGuardar={guardarArchivo}
          />
        </>
      )}
    </Hoja>
  );
}
