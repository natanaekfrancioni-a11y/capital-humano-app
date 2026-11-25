'use client';

import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { Ponderaciones, sumaPonderaciones } from '@/components/catalogo/Ponderaciones';
import { PantallaConsultor } from '@/components/shell/PantallaConsultor';
import { useSalir } from '@/components/shell/useSalir';
import { useVolver } from '@/components/shell/Volver';
import { Boton } from '@/components/ui/Boton';
import { CampoArea, CampoSeleccion, CampoTexto } from '@/components/ui/Campo';
import { useAvisos } from '@/components/ui/Dialogo';
import { Grilla } from '@/components/ui/Grilla';
import { AvisoErrores, BarraAcciones, Hoja, type ErroresFormulario } from '@/components/ui/Hoja';
import { ApiError, api } from '@/lib/api';
import { aNumero, puntaje } from '@/lib/formato';
import type { Competencia, Factor, OpcionRespuesta, Pregunta } from '@/lib/tipos';
import { validar } from '@/lib/validacion';

type Modo = { tipo: 'gestion' } | { tipo: 'alta' } | { tipo: 'modificar'; pregunta: Pregunta };
type Filtros = { competenciaId: string; factorId: string; nombre: string };

const FILTROS_VACIOS: Filtros = { competenciaId: '', factorId: '', nombre: '' };

export default function PreguntasPage() {
  return <PantallaConsultor>{(sesion) => <Preguntas usuario={sesion.username} />}</PantallaConsultor>;
}

/** CU020 - Gestionar preguntas (invoca CU021, CU022 y CU023). */
function Preguntas({ usuario }: { usuario: string }) {
  const avisos = useAvisos();
  const salir = useSalir();
  const [modo, setModo] = useState<Modo>({ tipo: 'gestion' });
  const [competencias, setCompetencias] = useState<Competencia[]>([]);
  const [factores, setFactores] = useState<Factor[]>([]);
  const [opciones, setOpciones] = useState<OpcionRespuesta[]>([]);
  const [filtros, setFiltros] = useState<Filtros>(FILTROS_VACIOS);
  const [ultimaBusqueda, setUltimaBusqueda] = useState<Filtros | null>(null);
  const [resultados, setResultados] = useState<Pregunta[] | null>(null);
  const [seleccion, setSeleccion] = useState<number | null>(null);
  const [buscando, setBuscando] = useState(false);

  useEffect(() => {
    let vigente = true;
    Promise.all([api.buscarCompetencias(), api.buscarFactores(), api.buscarOpcionesRespuesta()])
      .then(([c, f, o]) => {
        if (!vigente) return;
        setCompetencias(c);
        setFactores(f);
        setOpciones(o);
      })
      .catch((e) => avisos.error(e, 'No se pudieron cargar los datos de la pantalla'));
    return () => {
      vigente = false;
    };
  }, [avisos]);

  const ejecutarBusqueda = useCallback(
    async (f: Filtros) => {
      setBuscando(true);
      try {
        const r = await api.buscarPreguntas({
          competenciaId: f.competenciaId ? Number(f.competenciaId) : null,
          factorId: f.factorId ? Number(f.factorId) : null,
          nombre: f.nombre,
        });
        setResultados(r);
        setUltimaBusqueda(f);
        setSeleccion((actual) => (r.some((p) => p.id === actual) ? actual : null));
      } catch (e) {
        await avisos.error(e, 'No se pudo realizar la búsqueda');
      } finally {
        setBuscando(false);
      }
    },
    [avisos],
  );

  function buscar(e: FormEvent) {
    e.preventDefault();
    if (!filtros.competenciaId && !filtros.factorId && !filtros.nombre.trim()) {
      avisos.validacion(
        'Falta un criterio de búsqueda',
        'Se debe ingresar al menos un criterio de búsqueda: competencia, factor o nombre de pregunta.',
      );
      return;
    }
    ejecutarBusqueda(filtros);
  }

  async function refrescar() {
    if (ultimaBusqueda) await ejecutarBusqueda(ultimaBusqueda);
  }

  const seleccionada = resultados?.find((p) => p.id === seleccion) ?? null;

  // CU023 - Dar de baja una pregunta
  async function eliminar() {
    if (!seleccionada) return;
    const confirmado = await avisos.confirmar(
      'Baja de pregunta',
      `Los datos de la pregunta ${seleccionada.nombre} serán eliminados del sistema.`,
      { aceptar: 'Eliminar', rechazar: 'Cancelar', peligro: true },
    );
    if (!confirmado) return;
    try {
      await api.bajaPregunta(seleccionada.id, usuario);
      setSeleccion(null);
      await avisos.informar('Pregunta eliminada', `Los datos de la pregunta ${seleccionada.nombre} han sido eliminados del sistema.`, {
        cualquierTecla: true,
      });
      await refrescar();
    } catch (e) {
      await avisos.error(e, 'No se pudo eliminar la pregunta');
    }
  }

  const terminarFormulario = useCallback(
    (huboCambios: boolean) => {
      setModo({ tipo: 'gestion' });
      if (huboCambios && ultimaBusqueda) ejecutarBusqueda(ultimaBusqueda);
    },
    [ejecutarBusqueda, ultimaBusqueda],
  );

  if (modo.tipo !== 'gestion') {
    return (
      <FormularioPregunta
        key={modo.tipo === 'modificar' ? modo.pregunta.id : 'alta'}
        pregunta={modo.tipo === 'modificar' ? modo.pregunta : null}
        competencias={competencias}
        opciones={opciones}
        alTerminar={terminarFormulario}
      />
    );
  }

  const factoresFiltro = filtros.competenciaId
    ? factores.filter((f) => f.competencia.id === Number(filtros.competenciaId))
    : factores;

  return (
    <Hoja titulo="Preguntas" cu="CU020" bajada="Buscar preguntas para modificarlas o darlas de baja, o crear una nueva.">
      <form onSubmit={buscar} className="grid items-end gap-4 md:grid-cols-[1fr_1fr_1.2fr_auto]">
        <CampoSeleccion
          etiqueta="Competencia"
          valor={filtros.competenciaId}
          alCambiar={(v) => setFiltros((f) => ({ ...f, competenciaId: v, factorId: '' }))}
          opciones={competencias.map((c) => ({ valor: String(c.id), texto: c.nombre }))}
          textoVacio="Todas"
        />
        <CampoSeleccion
          etiqueta="Factor"
          valor={filtros.factorId}
          alCambiar={(v) => setFiltros((f) => ({ ...f, factorId: v }))}
          opciones={factoresFiltro.map((f) => ({ valor: String(f.id), texto: f.nombre }))}
          textoVacio="Todos"
        />
        <CampoTexto
          etiqueta="Nombre de pregunta"
          valor={filtros.nombre}
          alCambiar={(v) => setFiltros((f) => ({ ...f, nombre: v }))}
          maxLength={100}
        />
        <Boton type="submit" variante="secundario" ocupado={buscando} textoOcupado="Buscando…">
          Buscar
        </Boton>
      </form>

      <div className="mt-7">
        <Grilla
          etiqueta="Preguntas encontradas"
          columnas={[
            { titulo: 'Competencia', celda: (p: Pregunta) => p.factor.competencia.nombre, dato: true, ancho: '22%' },
            { titulo: 'Factor', celda: (p: Pregunta) => p.factor.nombre, dato: true, ancho: '22%' },
            {
              titulo: 'Pregunta',
              celda: (p: Pregunta) => (
                <>
                  <span className="block font-bold">{p.nombre}</span>
                  <span className="mt-0.5 line-clamp-2 block text-grafito-claro">{p.enunciado}</span>
                </>
              ),
              dato: true,
            },
          ]}
          filas={resultados ?? []}
          clave={(p) => p.id}
          seleccion={{
            modo: 'unica',
            nombre: 'pregunta',
            seleccionadas: seleccion === null ? [] : [seleccion],
            alCambiar: (ids) => setSeleccion(Number(ids[0])),
            etiquetaFila: (p) => `Seleccionar la pregunta ${p.nombre}`,
          }}
          vacio={
            resultados === null
              ? 'Cargue al menos un criterio y presione Buscar.'
              : 'No hay preguntas que coincidan con la búsqueda.'
          }
          pie={resultados && resultados.length > 0 ? `${resultados.length} preguntas encontradas.` : undefined}
        />
      </div>

      <BarraAcciones
        izquierda={
          <Boton variante="secundario" onClick={salir}>
            Salir
          </Boton>
        }
        derecha={
          <>
            <Boton
              variante="secundario"
              disabled={!seleccionada}
              onClick={() => seleccionada && setModo({ tipo: 'modificar', pregunta: seleccionada })}
            >
              Modificar
            </Boton>
            <Boton variante="secundario" disabled={!seleccionada} onClick={eliminar}>
              Eliminar
            </Boton>
            <Boton variante="principal" onClick={() => setModo({ tipo: 'alta' })}>
              Nuevo
            </Boton>
          </>
        }
      />
    </Hoja>
  );
}

interface FormularioProps {
  pregunta: Pregunta | null;
  competencias: Competencia[];
  opciones: OpcionRespuesta[];
  alTerminar: (huboCambios: boolean) => void;
}

/** CU021 - Dar de alta pregunta / CU022 - Modificar pregunta */
function FormularioPregunta({ pregunta, competencias, opciones, alTerminar }: FormularioProps) {
  const avisos = useAvisos();
  const esAlta = pregunta === null;
  const avisoRef = useRef<HTMLDivElement>(null);

  const [competenciaId, setCompetenciaId] = useState(pregunta ? String(pregunta.factor.competencia.id) : '');
  const [factores, setFactores] = useState<Factor[]>(pregunta ? [pregunta.factor] : []);
  const [factorId, setFactorId] = useState(pregunta ? String(pregunta.factor.id) : '');
  const [nombre, setNombre] = useState(pregunta?.nombre ?? '');
  const [enunciado, setEnunciado] = useState(pregunta?.enunciado ?? '');
  const [descripcion, setDescripcion] = useState(pregunta?.descripcion ?? '');
  const [opcionId, setOpcionId] = useState(pregunta ? String(pregunta.opcionRespuesta.id) : '');
  const [ponderaciones, setPonderaciones] = useState<Record<number, string>>(() =>
    pregunta
      ? Object.fromEntries(
          Object.entries(pregunta.ponderacionesPorDetalle).map(([id, v]) => [Number(id), String(v).replace('.', ',')]),
        )
      : {},
  );
  const [errores, setErrores] = useState<ErroresFormulario | null>(null);
  const [porCampo, setPorCampo] = useState<Record<string, string>>({});
  const [guardando, setGuardando] = useState(false);

  useVolver(useCallback(() => alTerminar(false), [alTerminar]));

  // Al seleccionar una competencia se cargan sus factores (CU021, observaciones).
  useEffect(() => {
    if (!esAlta || !competenciaId) return;
    let vigente = true;
    api
      .buscarFactores(Number(competenciaId))
      .then((f) => vigente && setFactores([...f].sort((a, b) => a.nroOrden - b.nroOrden)))
      .catch((e) => avisos.error(e, 'No se pudieron cargar los factores'));
    return () => {
      vigente = false;
    };
  }, [competenciaId, esAlta, avisos]);

  useEffect(() => {
    if (errores) avisoRef.current?.focus();
  }, [errores]);

  const opcion = opciones.find((o) => o.id === Number(opcionId)) ?? (pregunta && pregunta.opcionRespuesta.id === Number(opcionId) ? pregunta.opcionRespuesta : undefined);

  function limpiar() {
    setCompetenciaId('');
    setFactores([]);
    setFactorId('');
    setNombre('');
    setEnunciado('');
    setDescripcion('');
    setOpcionId('');
    setPonderaciones({});
    setErrores(null);
    setPorCampo({});
  }

  function validarPonderaciones(): string | null {
    if (!opcion) return null;
    const valores = opcion.detalles.map((d) => ponderaciones[d.id] ?? '');
    if (valores.some((v) => v.trim() === '')) return 'Falta cargar la ponderación de alguna opción de respuesta.';
    if (valores.some((v) => Number.isNaN(aNumero(v)) || aNumero(v) < 0)) return 'Las ponderaciones deben ser 0 o valores positivos.';
    const suma = sumaPonderaciones(ponderaciones);
    if (Math.abs(suma - 10) > 0.005) return `Las ponderaciones de las opciones deben sumar 10 puntos (suman ${puntaje(suma)}).`;
    return null;
  }

  async function aceptar(e: FormEvent) {
    e.preventDefault();
    const r = validar([
      { campo: 'Competencia', valor: competenciaId, obligatorio: true },
      { campo: 'Factor', valor: factorId, obligatorio: true },
      { campo: 'Nombre de pregunta', valor: nombre, obligatorio: true, maximo: 100 },
      { campo: 'Pregunta', valor: enunciado, obligatorio: true, maximo: 500 },
      { campo: 'Descripción', valor: descripcion, maximo: 255 },
      { campo: 'Opción de respuesta', valor: opcionId, obligatorio: true },
    ]);
    const errorPonderacion = validarPonderaciones();
    const mensajes = errorPonderacion ? [...r.mensajes, errorPonderacion] : r.mensajes;
    if (mensajes.length) {
      setPorCampo(r.porCampo);
      setErrores({ titulo: 'No se puede guardar la pregunta', items: mensajes });
      return;
    }

    setErrores(null);
    setPorCampo({});
    setGuardando(true);
    const datos = {
      opcionRespuestaId: Number(opcionId),
      nombre: nombre.trim(),
      enunciado: enunciado.trim(),
      descripcion: descripcion.trim() || null,
      ponderaciones: Object.fromEntries((opcion?.detalles ?? []).map((d) => [d.id, aNumero(ponderaciones[d.id] ?? '0')])),
    };
    try {
      if (esAlta) {
        await api.crearPregunta({ factorId: Number(factorId), ...datos });
        const otra = await avisos.abrir({
          tipo: 'informativa',
          titulo: 'Pregunta guardada',
          mensaje: `La pregunta ${datos.nombre} se ha creado correctamente. ¿Desea cargar otra?`,
          aceptar: 'Sí',
          rechazar: 'No',
        });
        if (otra) limpiar();
        else alTerminar(true);
      } else {
        await api.modificarPregunta(pregunta.id, datos);
        await avisos.informar('Pregunta modificada', 'La operación ha culminado con éxito.');
        alTerminar(true);
      }
    } catch (err) {
      if (err instanceof ApiError && !err.sinConexion) {
        setErrores({ titulo: err.message, items: err.detalles });
      } else {
        await avisos.error(err, 'No se pudo guardar la pregunta');
      }
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Hoja
      titulo={esAlta ? 'Alta de pregunta' : 'Modificar pregunta'}
      cu={esAlta ? 'CU021' : 'CU022'}
      bajada={esAlta ? 'Los campos con * son obligatorios.' : `Código ${pregunta.codigo}. La competencia y el factor no se pueden cambiar.`}
    >
      <form onSubmit={aceptar} noValidate>
        <AvisoErrores titulo={errores?.titulo ?? null} items={errores?.items} refAviso={avisoRef} />

        <div className="grid gap-5 md:grid-cols-2">
          <CampoSeleccion
            etiqueta="Competencia"
            obligatorio
            valor={competenciaId}
            alCambiar={(v) => {
              setCompetenciaId(v);
              setFactorId('');
              if (!v) setFactores([]);
            }}
            opciones={competencias.map((c) => ({ valor: String(c.id), texto: c.nombre }))}
            error={porCampo['Competencia']}
            disabled={!esAlta}
          />
          <CampoSeleccion
            etiqueta="Factor"
            obligatorio
            valor={factorId}
            alCambiar={setFactorId}
            opciones={factores.map((f) => ({ valor: String(f.id), texto: f.nombre }))}
            textoVacio={competenciaId ? 'Seleccionar…' : 'Primero elija la competencia'}
            error={porCampo['Factor']}
            disabled={!esAlta || !competenciaId}
          />
          <CampoTexto
            className="md:col-span-2"
            etiqueta="Nombre de pregunta"
            obligatorio
            valor={nombre}
            alCambiar={setNombre}
            maximo={100}
            error={porCampo['Nombre de pregunta']}
          />
          <CampoArea
            className="md:col-span-2"
            etiqueta="Pregunta"
            obligatorio
            valor={enunciado}
            alCambiar={setEnunciado}
            maximo={500}
            rows={3}
            error={porCampo['Pregunta']}
          />
          <CampoArea
            className="md:col-span-2"
            etiqueta="Descripción"
            valor={descripcion}
            alCambiar={setDescripcion}
            maximo={255}
            rows={2}
            ayuda="Opcional."
            error={porCampo['Descripción']}
          />
          <CampoSeleccion
            etiqueta="Opción de respuesta"
            obligatorio
            valor={opcionId}
            alCambiar={(v) => {
              setOpcionId(v);
              setPonderaciones({});
            }}
            opciones={opciones.map((o) => ({ valor: String(o.id), texto: o.nombre }))}
            error={porCampo['Opción de respuesta']}
            ayuda={opcion?.descripcion}
          />
        </div>

        {opcion && (
          <div className="mt-6">
            <Ponderaciones detalles={opcion.detalles} valores={ponderaciones} alCambiar={setPonderaciones} />
          </div>
        )}

        <BarraAcciones
          izquierda={
            <>
              {esAlta && (
                <Boton variante="secundario" disabled title="Generar con IA (CU033) todavía no está disponible">
                  Generar con IA
                </Boton>
              )}
              <Boton variante="secundario" onClick={() => alTerminar(false)}>
                Cancelar
              </Boton>
            </>
          }
          derecha={
            <Boton type="submit" variante="principal" ocupado={guardando} textoOcupado="Guardando…">
              Aceptar
            </Boton>
          }
        />
      </form>
    </Hoja>
  );
}
