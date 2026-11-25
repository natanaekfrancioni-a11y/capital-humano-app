'use client';

import { useCallback, useEffect, useId, useRef, useState, type FormEvent } from 'react';
import { useBusqueda } from '@/components/catalogo/useBusqueda';
import { PantallaConsultor } from '@/components/shell/PantallaConsultor';
import { useSalir } from '@/components/shell/useSalir';
import { useVolver } from '@/components/shell/Volver';
import { Boton } from '@/components/ui/Boton';
import { CampoArea, CampoSeleccion, CampoTexto } from '@/components/ui/Campo';
import { useAvisos } from '@/components/ui/Dialogo';
import { Grilla } from '@/components/ui/Grilla';
import { AvisoErrores, BarraAcciones, Hoja, Seccion, type ErroresFormulario } from '@/components/ui/Hoja';
import { ApiError, api } from '@/lib/api';
import { aNumero, puntaje } from '@/lib/formato';
import type { Competencia, Puesto } from '@/lib/tipos';
import { validar } from '@/lib/validacion';

type Filtros = { codigo: string; nombre: string; empresa: string };
const buscarPuestos = (f: Filtros) => api.buscarPuestos(f);

export default function PuestosPage() {
  return <PantallaConsultor>{(sesion) => <Puestos usuario={sesion.username} />}</PantallaConsultor>;
}

/** CU008 - Gestionar puestos o funciones (invoca CU009, CU010 y CU011). */
function Puestos({ usuario }: { usuario: string }) {
  const avisos = useAvisos();
  const salir = useSalir();
  const [filtros, setFiltros] = useState<Filtros>({ codigo: '', nombre: '', empresa: '' });
  const [formulario, setFormulario] = useState<{ puesto: Puesto | null } | null>(null);
  const b = useBusqueda(buscarPuestos);
  const { refrescar } = b;
  const hayFiltro = Object.values(filtros).some((v) => v.trim() !== '');

  // CU011 - Dar de baja un puesto: primero se constata que no tenga evaluaciones.
  async function eliminar() {
    const p = b.seleccionado;
    if (!p) return;
    try {
      const uso = await api.puestoEnUso(p.id);
      if (uso.enUso) {
        await avisos.abrir({ tipo: 'error', titulo: 'No se puede eliminar', mensaje: uso.mensaje, cualquierTecla: true });
        return;
      }
      const ok = await avisos.confirmar('Baja de puesto', uso.mensaje, { aceptar: 'Eliminar', rechazar: 'Cancelar', peligro: true });
      if (!ok) return;
      await api.bajaPuesto(p.id, usuario);
      b.setSeleccion(null);
      await avisos.informar('Puesto eliminado', `Los datos del puesto ${p.nombre} han sido eliminados del sistema.`, { cualquierTecla: true });
      await refrescar();
    } catch (e) {
      await avisos.error(e, 'No se pudo eliminar el puesto');
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
    return <FormularioPuesto key={formulario.puesto?.id ?? 'alta'} puesto={formulario.puesto} alTerminar={terminar} />;
  }

  return (
    <Hoja titulo="Puestos" cu="CU008" bajada="Buscar puestos o funciones para modificarlos o darlos de baja, o crear uno nuevo.">
      <form
        onSubmit={(e: FormEvent) => {
          e.preventDefault();
          if (hayFiltro) b.ejecutar(filtros);
        }}
        className="grid items-end gap-4 md:grid-cols-[0.7fr_1.2fr_1fr_auto]"
      >
        <CampoTexto etiqueta="Código" valor={filtros.codigo} alCambiar={(v) => setFiltros((f) => ({ ...f, codigo: v }))} maxLength={20} />
        <CampoTexto
          etiqueta="Nombre del puesto"
          valor={filtros.nombre}
          alCambiar={(v) => setFiltros((f) => ({ ...f, nombre: v }))}
          maxLength={100}
        />
        <CampoTexto etiqueta="Empresa" valor={filtros.empresa} alCambiar={(v) => setFiltros((f) => ({ ...f, empresa: v }))} maxLength={100} />
        <Boton type="submit" disabled={!hayFiltro} ocupado={b.buscando} textoOcupado="Buscando…" title={hayFiltro ? undefined : 'Complete al menos un criterio'}>
          Buscar
        </Boton>
      </form>
      {!hayFiltro && <p className="mt-2 text-chica text-grafito-claro">Complete al menos un criterio para buscar.</p>}

      <div className="mt-7">
        <Grilla
          etiqueta="Puestos encontrados"
          columnas={[
            { titulo: 'Código', celda: (p: Puesto) => p.codigo, dato: true, ancho: '10rem' },
            { titulo: 'Nombre del puesto', celda: (p: Puesto) => p.nombre, dato: true },
            { titulo: 'Empresa', celda: (p: Puesto) => p.empresa, dato: true },
          ]}
          filas={b.resultados ?? []}
          clave={(p) => p.id}
          seleccion={{
            modo: 'unica',
            nombre: 'puesto',
            seleccionadas: b.seleccion === null ? [] : [b.seleccion],
            alCambiar: (ids) => b.setSeleccion(Number(ids[0])),
            etiquetaFila: (p) => `Seleccionar el puesto ${p.nombre}`,
          }}
          vacio={b.resultados === null ? 'Complete un criterio y presione Buscar.' : 'No hay puestos que coincidan.'}
        />
      </div>

      <BarraAcciones
        izquierda={<Boton onClick={salir}>Salir</Boton>}
        derecha={
          <>
            <Boton disabled={!b.seleccionado} onClick={() => b.seleccionado && setFormulario({ puesto: b.seleccionado })}>
              Modificar
            </Boton>
            <Boton disabled={!b.seleccionado} onClick={eliminar}>
              Eliminar
            </Boton>
            <Boton variante="principal" onClick={() => setFormulario({ puesto: null })}>
              Nuevo
            </Boton>
          </>
        }
      />
    </Hoja>
  );
}

interface FilaCompetencia {
  competenciaId: number;
  nombre: string;
  ponderacion: number;
}

/** CU009 - Dar de alta puesto / CU010 - Modificar puesto */
function FormularioPuesto({ puesto, alTerminar }: { puesto: Puesto | null; alTerminar: (huboCambios: boolean) => void }) {
  const avisos = useAvisos();
  const esAlta = puesto === null;
  const avisoRef = useRef<HTMLDivElement>(null);
  const empresasId = useId();
  const [competencias, setCompetencias] = useState<Competencia[]>([]);
  const [empresas, setEmpresas] = useState<string[]>([]);
  const [codigo, setCodigo] = useState(puesto?.codigo ?? '');
  const [nombre, setNombre] = useState(puesto?.nombre ?? '');
  const [empresa, setEmpresa] = useState(puesto?.empresa ?? '');
  const [descripcion, setDescripcion] = useState(puesto?.descripcion ?? '');
  const [filas, setFilas] = useState<FilaCompetencia[]>(
    () =>
      puesto?.competencias.map((pc) => ({
        competenciaId: pc.competencia.id,
        nombre: pc.competencia.nombre,
        ponderacion: pc.ponderacionMinima,
      })) ?? [],
  );
  const [nuevaCompetencia, setNuevaCompetencia] = useState('');
  const [nuevaPonderacion, setNuevaPonderacion] = useState('');
  const [errorAgregar, setErrorAgregar] = useState<string | null>(null);
  const [errores, setErrores] = useState<ErroresFormulario | null>(null);
  const [porCampo, setPorCampo] = useState<Record<string, string>>({});
  const [guardando, setGuardando] = useState(false);

  const cancelar = useCallback(async () => {
    const cambios = esAlta && (codigo || nombre || empresa || descripcion || filas.length > 0);
    if (cambios) {
      const ok = await avisos.confirmar('Cancelar el alta', '¿Desea cancelar el alta? Los datos ingresados se perderán.', {
        aceptar: 'Sí, cancelar',
        rechazar: 'No',
      });
      if (!ok) return;
    }
    alTerminar(false);
  }, [esAlta, codigo, nombre, empresa, descripcion, filas.length, avisos, alTerminar]);

  useVolver(cancelar);
  useEffect(() => {
    if (errores) avisoRef.current?.focus();
  }, [errores]);

  useEffect(() => {
    Promise.all([api.buscarCompetencias(), api.buscarPuestos()])
      .then(([c, p]) => {
        setCompetencias(c);
        setEmpresas([...new Set(p.map((x) => x.empresa))].sort());
      })
      .catch((e) => avisos.error(e, 'No se pudieron cargar las competencias'));
  }, [avisos]);

  // Botón "+": valida competencia y ponderación y las agrega a la grilla.
  function agregar() {
    const comp = competencias.find((c) => c.id === Number(nuevaCompetencia));
    const pond = aNumero(nuevaPonderacion);
    if (!comp || nuevaPonderacion.trim() === '') {
      setErrorAgregar('Para agregar, elija una competencia y cargue su ponderación.');
      return;
    }
    if (Number.isNaN(pond) || pond < 0 || pond > 10) {
      setErrorAgregar('La ponderación es un valor entre 0 y 10.');
      return;
    }
    if (filas.some((f) => f.competenciaId === comp.id)) {
      setErrorAgregar('Cada competencia puede estar una sola vez en el puesto.');
      return;
    }
    setFilas((f) => [...f, { competenciaId: comp.id, nombre: comp.nombre, ponderacion: pond }]);
    setNuevaCompetencia('');
    setNuevaPonderacion('');
    setErrorAgregar(null);
  }

  async function aceptar(e: FormEvent) {
    e.preventDefault();
    const r = validar([
      { campo: 'Código', valor: codigo, obligatorio: true, maximo: 30 },
      { campo: 'Nombre del puesto', valor: nombre, obligatorio: true, maximo: 100 },
      { campo: 'Empresa', valor: empresa, obligatorio: true, maximo: 100 },
      { campo: 'Descripción', valor: descripcion, obligatorio: true, maximo: 300 },
    ]);
    const mensajes = [...r.mensajes];
    if (filas.length === 0) mensajes.push('Debe asociar al menos una competencia con su ponderación (botón Agregar).');
    setPorCampo(r.porCampo);
    if (mensajes.length) {
      setErrores({ titulo: 'No se puede guardar el puesto', items: mensajes });
      return;
    }
    setErrores(null);
    setGuardando(true);
    const datos = {
      nombre: nombre.trim(),
      empresa: empresa.trim(),
      descripcion: descripcion.trim(),
      competencias: filas.map((f) => ({ competenciaId: f.competenciaId, ponderacionMinima: f.ponderacion })),
    };
    try {
      if (esAlta) {
        await api.crearPuesto({ codigo: codigo.trim(), ...datos });
        const otro = await avisos.abrir({
          tipo: 'informativa',
          titulo: 'Puesto guardado',
          mensaje: `El puesto ${datos.nombre} se ha creado correctamente. ¿Desea cargar otro?`,
          aceptar: 'Sí',
          rechazar: 'No',
        });
        if (otro) {
          setCodigo('');
          setNombre('');
          setEmpresa('');
          setDescripcion('');
          setFilas([]);
        } else {
          alTerminar(true);
        }
      } else {
        await api.modificarPuesto(puesto.id, datos);
        await avisos.informar('Puesto modificado', 'La operación ha culminado con éxito.');
        alTerminar(true);
      }
    } catch (err) {
      if (err instanceof ApiError && !err.sinConexion) setErrores({ titulo: err.message, items: err.detalles });
      else await avisos.error(err, 'No se pudo guardar el puesto');
    } finally {
      setGuardando(false);
    }
  }

  const disponibles = competencias.filter((c) => !filas.some((f) => f.competenciaId === c.id));

  return (
    <Hoja
      titulo={esAlta ? 'Alta de puesto' : 'Modificar puesto'}
      cu={esAlta ? 'CU009' : 'CU010'}
      bajada={esAlta ? 'Los campos con * son obligatorios.' : 'El código no se puede modificar.'}
    >
      <form onSubmit={aceptar} noValidate>
        <AvisoErrores titulo={errores?.titulo ?? null} items={errores?.items} refAviso={avisoRef} />
        <div className="grid gap-5 md:grid-cols-[0.6fr_1.4fr_1fr]">
          <CampoTexto etiqueta="Código" obligatorio valor={codigo} alCambiar={setCodigo} maximo={30} error={porCampo['Código']} disabled={!esAlta} />
          <CampoTexto
            etiqueta="Nombre del puesto"
            obligatorio
            valor={nombre}
            alCambiar={setNombre}
            maximo={100}
            error={porCampo['Nombre del puesto']}
          />
          <CampoTexto
            etiqueta="Empresa"
            obligatorio
            valor={empresa}
            alCambiar={setEmpresa}
            maximo={100}
            list={empresasId}
            error={porCampo['Empresa']}
            ayuda="Puede elegir una empresa existente o escribir una nueva."
          />
          <datalist id={empresasId}>
            {empresas.map((e) => (
              <option key={e} value={e} />
            ))}
          </datalist>
          <CampoArea
            className="md:col-span-3"
            etiqueta="Descripción"
            obligatorio
            valor={descripcion}
            alCambiar={setDescripcion}
            maximo={300}
            error={porCampo['Descripción']}
          />
        </div>

        <div className="mt-8">
          <Seccion titulo="Competencias del puesto">
            <div className="grid items-end gap-4 sm:grid-cols-[1fr_9rem_auto]">
              <CampoSeleccion
                etiqueta="Competencia"
                valor={nuevaCompetencia}
                alCambiar={setNuevaCompetencia}
                opciones={disponibles.map((c) => ({ valor: String(c.id), texto: c.nombre }))}
              />
              <CampoTexto
                etiqueta="Ponderación (0 a 10)"
                valor={nuevaPonderacion}
                alCambiar={(v) => setNuevaPonderacion(v.replace(/[^\d.,]/g, ''))}
                inputMode="decimal"
                mayusculas={false}
              />
              <Boton onClick={agregar}>Agregar</Boton>
            </div>
            {errorAgregar && <p className="mt-2 text-chica font-bold text-error" role="alert">{errorAgregar}</p>}

            <div className="mt-5">
              <Grilla
                etiqueta="Competencias y ponderación mínima requerida"
                columnas={[
                  { titulo: 'Competencia', celda: (f: FilaCompetencia) => f.nombre, dato: true },
                  {
                    titulo: 'Ponderación',
                    celda: (f: FilaCompetencia) => puntaje(f.ponderacion),
                    cifras: true,
                    alinear: 'derecha',
                    ancho: '9rem',
                  },
                  {
                    titulo: 'Quitar',
                    celda: (f: FilaCompetencia) => (
                      <button
                        type="button"
                        onClick={() => setFilas((l) => l.filter((x) => x.competenciaId !== f.competenciaId))}
                        className="h-8 rounded-[4px] px-2 text-chica font-bold underline decoration-linea underline-offset-4 hover:decoration-tinta"
                        aria-label={`Quitar ${f.nombre}`}
                      >
                        Quitar
                      </button>
                    ),
                    alinear: 'derecha',
                    ancho: '7rem',
                  },
                ]}
                filas={filas}
                clave={(f) => f.competenciaId}
                vacio="Todavía no se agregaron competencias."
              />
            </div>
          </Seccion>
        </div>

        <BarraAcciones
          izquierda={<Boton onClick={cancelar}>Cancelar</Boton>}
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
