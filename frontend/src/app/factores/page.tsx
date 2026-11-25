'use client';

import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { useBusqueda } from '@/components/catalogo/useBusqueda';
import { PantallaConsultor } from '@/components/shell/PantallaConsultor';
import { useSalir } from '@/components/shell/useSalir';
import { useVolver } from '@/components/shell/Volver';
import { Boton } from '@/components/ui/Boton';
import { CampoArea, CampoSeleccion, CampoTexto } from '@/components/ui/Campo';
import { useAvisos } from '@/components/ui/Dialogo';
import { Grilla } from '@/components/ui/Grilla';
import { AvisoErrores, BarraAcciones, Hoja, type ErroresFormulario } from '@/components/ui/Hoja';
import { ApiError, api } from '@/lib/api';
import type { Competencia, Factor } from '@/lib/tipos';
import { validar } from '@/lib/validacion';

type Filtros = { competenciaId: string; nombre: string };
const buscarFactores = (f: Filtros) => api.buscarFactores(f.competenciaId ? Number(f.competenciaId) : null, f.nombre);

export default function FactoresPage() {
  return <PantallaConsultor>{(sesion) => <Factores usuario={sesion.username} />}</PantallaConsultor>;
}

/** CU016 - Gestión de factores (invoca CU017, CU018 y CU019). */
function Factores({ usuario }: { usuario: string }) {
  const avisos = useAvisos();
  const salir = useSalir();
  const [competencias, setCompetencias] = useState<Competencia[]>([]);
  const [filtros, setFiltros] = useState<Filtros>({ competenciaId: '', nombre: '' });
  const [formulario, setFormulario] = useState<{ factor: Factor | null } | null>(null);
  const b = useBusqueda(buscarFactores);
  const { refrescar } = b;

  useEffect(() => {
    api.buscarCompetencias().then(setCompetencias).catch((e) => avisos.error(e, 'No se pudieron cargar las competencias'));
  }, [avisos]);

  // CU019 - Dar de baja factor
  async function eliminar() {
    const f = b.seleccionado;
    if (!f) return;
    const ok = await avisos.confirmar(
      'Baja de factor',
      `Los datos del factor ${f.nombre} serán eliminados del sistema, junto con sus preguntas.`,
      { aceptar: 'Eliminar', rechazar: 'Cancelar', peligro: true },
    );
    if (!ok) return;
    try {
      await api.bajaFactor(f.id, usuario);
      b.setSeleccion(null);
      await avisos.informar('Factor eliminado', `Los datos del factor ${f.nombre} han sido eliminados del sistema.`, { cualquierTecla: true });
      await refrescar();
    } catch (e) {
      await avisos.error(e, 'No se pudo eliminar el factor');
    }
  }

  const terminar = useCallback(
    (huboCambios: boolean) => {
      setFormulario(null);
      if (huboCambios) refrescar();
    },
    [refrescar],
  );

  if (formulario) {
    return (
      <FormularioFactor
        key={formulario.factor?.id ?? 'alta'}
        factor={formulario.factor}
        competencias={competencias}
        alTerminar={terminar}
      />
    );
  }

  return (
    <Hoja titulo="Factores" cu="CU016" bajada="Buscar factores por competencia o por nombre. La lista se ordena por nombre de factor.">
      <form
        onSubmit={(e: FormEvent) => {
          e.preventDefault();
          b.ejecutar(filtros);
        }}
        className="grid items-end gap-4 md:grid-cols-[1fr_1fr_auto]"
      >
        <CampoSeleccion
          etiqueta="Competencia"
          valor={filtros.competenciaId}
          alCambiar={(v) => setFiltros((f) => ({ ...f, competenciaId: v }))}
          opciones={competencias.map((c) => ({ valor: String(c.id), texto: c.nombre }))}
          textoVacio="Todas"
        />
        <CampoTexto
          etiqueta="Nombre de factor"
          valor={filtros.nombre}
          alCambiar={(v) => setFiltros((f) => ({ ...f, nombre: v }))}
          maxLength={100}
        />
        <Boton type="submit" ocupado={b.buscando} textoOcupado="Buscando…">
          Buscar
        </Boton>
      </form>

      <div className="mt-7">
        <Grilla
          etiqueta="Factores encontrados"
          columnas={[
            { titulo: 'Código', celda: (f: Factor) => f.codigo, dato: true, ancho: '10rem' },
            { titulo: 'Nombre de factor', celda: (f: Factor) => f.nombre, dato: true },
            { titulo: 'Competencia', celda: (f: Factor) => f.competencia.nombre, dato: true },
            { titulo: 'Orden', celda: (f: Factor) => f.nroOrden, cifras: true, alinear: 'derecha', ancho: '5rem' },
          ]}
          filas={b.resultados ?? []}
          clave={(f) => f.id}
          seleccion={{
            modo: 'unica',
            nombre: 'factor',
            seleccionadas: b.seleccion === null ? [] : [b.seleccion],
            alCambiar: (ids) => b.setSeleccion(Number(ids[0])),
            etiquetaFila: (f) => `Seleccionar el factor ${f.nombre}`,
          }}
          vacio={b.resultados === null ? 'Presione Buscar para ver los factores.' : 'No hay factores que coincidan.'}
        />
      </div>

      <BarraAcciones
        izquierda={<Boton onClick={salir}>Salir</Boton>}
        derecha={
          <>
            <Boton disabled={!b.seleccionado} onClick={() => b.seleccionado && setFormulario({ factor: b.seleccionado })}>
              Modificar
            </Boton>
            <Boton disabled={!b.seleccionado} onClick={eliminar}>
              Eliminar
            </Boton>
            <Boton variante="principal" onClick={() => setFormulario({ factor: null })}>
              Nuevo
            </Boton>
          </>
        }
      />
    </Hoja>
  );
}

/** CU017 - Dar de alta factor / CU018 - Modificar factor */
function FormularioFactor({
  factor,
  competencias,
  alTerminar,
}: {
  factor: Factor | null;
  competencias: Competencia[];
  alTerminar: (huboCambios: boolean) => void;
}) {
  const avisos = useAvisos();
  const esAlta = factor === null;
  const avisoRef = useRef<HTMLDivElement>(null);
  const [competenciaId, setCompetenciaId] = useState(factor ? String(factor.competencia.id) : '');
  const [codigo, setCodigo] = useState(factor?.codigo ?? '');
  const [nombre, setNombre] = useState(factor?.nombre ?? '');
  const [descripcion, setDescripcion] = useState(factor?.descripcion ?? '');
  const [nroOrden, setNroOrden] = useState(factor ? String(factor.nroOrden) : '');
  const [hermanos, setHermanos] = useState<Factor[]>([]);
  const [errores, setErrores] = useState<ErroresFormulario | null>(null);
  const [porCampo, setPorCampo] = useState<Record<string, string>>({});
  const [guardando, setGuardando] = useState(false);

  useVolver(useCallback(() => alTerminar(false), [alTerminar]));
  useEffect(() => {
    if (errores) avisoRef.current?.focus();
  }, [errores]);

  // Mecanismo para gestionar el orden: se muestran los factores de la competencia y los números libres.
  useEffect(() => {
    if (!competenciaId) return;
    let vigente = true;
    api
      .buscarFactores(Number(competenciaId))
      .then((lista) => {
        if (!vigente) return;
        const otros = lista.filter((f) => f.id !== factor?.id).sort((a, b) => a.nroOrden - b.nroOrden);
        setHermanos(otros);
        if (esAlta) {
          const usados = new Set(otros.map((f) => f.nroOrden));
          let libre = 1;
          while (usados.has(libre)) libre += 1;
          setNroOrden(String(libre));
        }
      })
      .catch((e) => avisos.error(e, 'No se pudieron cargar los factores de la competencia'));
    return () => {
      vigente = false;
    };
  }, [competenciaId, esAlta, factor?.id, avisos]);

  const ordenesLibres = useMemo(() => {
    const usados = new Set(hermanos.map((f) => f.nroOrden));
    const tope = Math.max(hermanos.length + 1, factor?.nroOrden ?? 0, ...hermanos.map((f) => f.nroOrden + 1));
    return Array.from({ length: tope }, (_, i) => i + 1).filter((n) => !usados.has(n));
  }, [hermanos, factor?.nroOrden]);

  async function aceptar(e: FormEvent) {
    e.preventDefault();
    const r = validar([
      { campo: 'Competencia', valor: competenciaId, obligatorio: true },
      { campo: 'Código', valor: codigo, obligatorio: true, maximo: 30 },
      { campo: 'Nombre', valor: nombre, obligatorio: true, maximo: 100 },
      { campo: 'Descripción', valor: descripcion, obligatorio: true, maximo: 255 },
      { campo: 'Nro de orden', valor: nroOrden, obligatorio: true },
    ]);
    setPorCampo(r.porCampo);
    if (r.mensajes.length) {
      setErrores({ titulo: 'No se puede guardar el factor', items: r.mensajes });
      return;
    }
    setErrores(null);
    setGuardando(true);
    try {
      if (esAlta) {
        await api.crearFactor({
          competenciaId: Number(competenciaId),
          codigo: codigo.trim(),
          nombre: nombre.trim(),
          descripcion: descripcion.trim(),
          nroOrden: Number(nroOrden),
        });
        const otro = await avisos.abrir({
          tipo: 'informativa',
          titulo: 'Factor guardado',
          mensaje: `El factor ${nombre.trim()} se ha creado correctamente. ¿Desea cargar otro?`,
          aceptar: 'Sí',
          rechazar: 'No',
        });
        if (otro) {
          setCodigo('');
          setNombre('');
          setDescripcion('');
          setCompetenciaId('');
          setNroOrden('');
          setHermanos([]);
        } else {
          alTerminar(true);
        }
      } else {
        await api.modificarFactor(factor.id, { nombre: nombre.trim(), descripcion: descripcion.trim(), nroOrden: Number(nroOrden) });
        await avisos.informar('Factor modificado', 'La operación ha culminado con éxito.');
        alTerminar(true);
      }
    } catch (err) {
      if (err instanceof ApiError && !err.sinConexion) setErrores({ titulo: err.message, items: err.detalles });
      else await avisos.error(err, 'No se pudo guardar el factor');
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Hoja
      titulo={esAlta ? 'Alta de factor' : 'Modificar factor'}
      cu={esAlta ? 'CU017' : 'CU018'}
      bajada={esAlta ? 'Los campos con * son obligatorios.' : 'El código y la competencia no se pueden modificar.'}
    >
      <form onSubmit={aceptar} noValidate>
        <AvisoErrores titulo={errores?.titulo ?? null} items={errores?.items} refAviso={avisoRef} />
        <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_18rem]">
          <div className="grid content-start gap-5 sm:grid-cols-2">
            <CampoTexto
              etiqueta="Código"
              obligatorio
              valor={codigo}
              alCambiar={setCodigo}
              maximo={30}
              error={porCampo['Código']}
              disabled={!esAlta}
            />
            <CampoTexto
              etiqueta="Nombre"
              obligatorio
              valor={nombre}
              alCambiar={setNombre}
              maximo={100}
              error={porCampo['Nombre']}
            />
            <CampoArea
              className="sm:col-span-2"
              etiqueta="Descripción"
              obligatorio
              valor={descripcion}
              alCambiar={setDescripcion}
              maximo={255}
              error={porCampo['Descripción']}
            />
            <CampoSeleccion
              etiqueta="Competencia"
              obligatorio
              valor={competenciaId}
              alCambiar={(v) => {
                setCompetenciaId(v);
                if (!v) {
                  setHermanos([]);
                  setNroOrden('');
                }
              }}
              opciones={competencias.map((c) => ({ valor: String(c.id), texto: c.nombre }))}
              error={porCampo['Competencia']}
              disabled={!esAlta}
            />
            <CampoSeleccion
              etiqueta="Nro de orden"
              obligatorio
              valor={nroOrden}
              alCambiar={setNroOrden}
              opciones={ordenesLibres.map((n) => ({ valor: String(n), texto: String(n) }))}
              textoVacio={competenciaId ? 'Seleccionar…' : 'Primero elija la competencia'}
              error={porCampo['Nro de orden']}
              disabled={!competenciaId}
              ayuda="Solo se ofrecen los números libres."
            />
          </div>

          <aside aria-label="Orden actual de los factores de la competencia" className="lg:border-l lg:border-linea lg:pl-6">
            <p className="text-chica font-bold text-grafito">Orden actual en la competencia</p>
            {!competenciaId ? (
              <p className="mt-2 text-chica text-grafito-claro">Elija una competencia para ver sus factores.</p>
            ) : (
              <ol className="mt-2 divide-y divide-linea border-y border-linea text-chica">
                {[...hermanos, { id: -1, nroOrden: Number(nroOrden) || 0, nombre: nombre.trim() || 'ESTE FACTOR' } as Pick<Factor, 'id' | 'nroOrden' | 'nombre'>]
                  .filter((f) => f.nroOrden > 0)
                  .sort((a, b) => a.nroOrden - b.nroOrden)
                  .map((f) => (
                    <li key={f.id} className={`flex gap-3 py-2 ${f.id === -1 ? 'font-bold text-tinta' : ''}`}>
                      <span className="cifras w-6 text-right">{f.nroOrden}</span>
                      <span className="dato">{f.nombre}</span>
                    </li>
                  ))}
              </ol>
            )}
          </aside>
        </div>

        <BarraAcciones
          izquierda={<Boton onClick={() => alTerminar(false)}>Cancelar</Boton>}
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
