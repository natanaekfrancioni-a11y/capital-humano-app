'use client';

import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { useBusqueda } from '@/components/catalogo/useBusqueda';
import { PantallaConsultor } from '@/components/shell/PantallaConsultor';
import { useSalir } from '@/components/shell/useSalir';
import { useVolver } from '@/components/shell/Volver';
import { Boton } from '@/components/ui/Boton';
import { CampoTexto } from '@/components/ui/Campo';
import { useAvisos } from '@/components/ui/Dialogo';
import { Grilla } from '@/components/ui/Grilla';
import { AvisoErrores, BarraAcciones, Hoja, Seccion, type ErroresFormulario } from '@/components/ui/Hoja';
import { ApiError, api } from '@/lib/api';
import type { OpcionRespuesta } from '@/lib/tipos';
import { validar } from '@/lib/validacion';

const buscarOpciones = (nombre: string) => api.buscarOpcionesRespuesta(nombre);

export default function OpcionesRespuestaPage() {
  return <PantallaConsultor>{(sesion) => <OpcionesRespuesta usuario={sesion.username} />}</PantallaConsultor>;
}

/** CU024 - Gestionar opciones de respuesta (invoca CU029, CU030 y CU031). */
function OpcionesRespuesta({ usuario }: { usuario: string }) {
  const avisos = useAvisos();
  const salir = useSalir();
  const [nombre, setNombre] = useState('');
  const [formulario, setFormulario] = useState<{ opcion: OpcionRespuesta | null } | null>(null);
  const b = useBusqueda(buscarOpciones);
  const { refrescar } = b;

  // CU031 - Dar de baja opción de respuesta
  async function eliminar() {
    const o = b.seleccionado;
    if (!o) return;
    const ok = await avisos.confirmar('Baja de opción de respuesta', `Los datos de la opción de respuesta ${o.nombre} serán eliminados del sistema.`, {
      aceptar: 'Eliminar',
      rechazar: 'Cancelar',
      peligro: true,
    });
    if (!ok) return;
    try {
      await api.bajaOpcionRespuesta(o.id, usuario);
      b.setSeleccion(null);
      await avisos.informar('Opción de respuesta eliminada', `Los datos de la opción de respuesta ${o.nombre} han sido eliminados del sistema.`, {
        cualquierTecla: true,
      });
      await refrescar();
    } catch (e) {
      await avisos.error(e, 'No se pudo eliminar la opción de respuesta');
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
    return <FormularioOpcion key={formulario.opcion?.id ?? 'alta'} opcion={formulario.opcion} alTerminar={terminar} />;
  }

  return (
    <Hoja
      titulo="Opciones de respuesta"
      cu="CU024"
      bajada="Escalas que se asignan a las preguntas. Si busca sin completar el nombre, se listan todas."
    >
      <form
        onSubmit={(e: FormEvent) => {
          e.preventDefault();
          b.ejecutar(nombre);
        }}
        className="grid items-end gap-4 sm:grid-cols-[minmax(0,26rem)_auto]"
      >
        <CampoTexto etiqueta="Nombre de la opción de respuesta" valor={nombre} alCambiar={setNombre} maxLength={100} />
        <Boton type="submit" ocupado={b.buscando} textoOcupado="Buscando…">
          Buscar
        </Boton>
      </form>

      <div className="mt-7">
        <Grilla
          etiqueta="Opciones de respuesta encontradas"
          columnas={[
            { titulo: 'Nombre', celda: (o: OpcionRespuesta) => o.nombre, dato: true, ancho: '30%' },
            {
              titulo: 'Descripción',
              celda: (o: OpcionRespuesta) => (
                <>
                  <span className="block">{o.descripcion}</span>
                  <span className="mt-0.5 block text-grafito-claro">
                    {[...o.detalles].sort((a, b) => a.nroOrden - b.nroOrden).map((d) => d.descripcion).join(' / ')}
                  </span>
                </>
              ),
              dato: true,
            },
          ]}
          filas={b.resultados ?? []}
          clave={(o) => o.id}
          seleccion={{
            modo: 'unica',
            nombre: 'opcion',
            seleccionadas: b.seleccion === null ? [] : [b.seleccion],
            alCambiar: (ids) => b.setSeleccion(Number(ids[0])),
            etiquetaFila: (o) => `Seleccionar la opción ${o.nombre}`,
          }}
          vacio={b.resultados === null ? 'Presione Buscar para ver las opciones de respuesta.' : 'No hay opciones con ese nombre.'}
        />
      </div>

      <BarraAcciones
        izquierda={<Boton onClick={salir}>Salir</Boton>}
        derecha={
          <>
            <Boton disabled={!b.seleccionado} onClick={() => b.seleccionado && setFormulario({ opcion: b.seleccionado })}>
              Modificar
            </Boton>
            <Boton disabled={!b.seleccionado} onClick={eliminar}>
              Eliminar
            </Boton>
            <Boton variante="principal" onClick={() => setFormulario({ opcion: null })}>
              Nuevo
            </Boton>
          </>
        }
      />
    </Hoja>
  );
}

interface Item {
  clave: number;
  /** Id de la opción existente (solo al modificar, CU030). */
  id?: number;
  nroOrden: string;
  descripcion: string;
}

let proximaClave = 1;
const nuevoItem = (nroOrden: number, descripcion = '', id?: number): Item => ({
  clave: proximaClave++,
  id,
  nroOrden: String(nroOrden),
  descripcion,
});

/** CU029 - Dar de alta opción de respuesta / CU030 - Modificar opción de respuesta */
function FormularioOpcion({ opcion, alTerminar }: { opcion: OpcionRespuesta | null; alTerminar: (huboCambios: boolean) => void }) {
  const avisos = useAvisos();
  const esAlta = opcion === null;
  const avisoRef = useRef<HTMLDivElement>(null);
  const [nombre, setNombre] = useState(opcion?.nombre ?? '');
  const [descripcion, setDescripcion] = useState(opcion?.descripcion ?? '');
  const [items, setItems] = useState<Item[]>(() =>
    opcion
      ? [...opcion.detalles].sort((a, b) => a.nroOrden - b.nroOrden).map((d) => nuevoItem(d.nroOrden, d.descripcion, d.id))
      : [nuevoItem(1), nuevoItem(2)],
  );
  const [errores, setErrores] = useState<ErroresFormulario | null>(null);
  const [porCampo, setPorCampo] = useState<Record<string, string>>({});
  const [guardando, setGuardando] = useState(false);

  useVolver(useCallback(() => alTerminar(false), [alTerminar]));
  useEffect(() => {
    if (errores) avisoRef.current?.focus();
  }, [errores]);

  function cambiar(clave: number, cambios: Partial<Item>) {
    setItems((lista) => lista.map((i) => (i.clave === clave ? { ...i, ...cambios } : i)));
  }

  function validarItems(): string[] {
    const mensajes: string[] = [];
    if (items.length === 0) return ['Debe ingresar al menos una opción.'];
    if (items.some((i) => i.descripcion.trim() === '')) mensajes.push('Falta la descripción de alguna opción de la lista.');
    const ordenes = items.map((i) => Number(i.nroOrden)).sort((a, b) => a - b);
    if (items.some((i) => !/^\d+$/.test(i.nroOrden.trim())) || ordenes.some((n, idx) => n !== idx + 1)) {
      mensajes.push('El orden de visualización no es consecutivo: debe ir de 1 en 1 sin repetirse.');
    }
    const descripciones = items.map((i) => i.descripcion.trim()).filter(Boolean);
    const repetidas = descripciones.filter((d, idx) => descripciones.indexOf(d) !== idx);
    if (repetidas.length) mensajes.push(`La descripción de opción está más de una vez en la opción de respuesta: ${[...new Set(repetidas)].join(', ')}.`);
    return mensajes;
  }

  async function aceptar(e: FormEvent) {
    e.preventDefault();
    const r = validar([
      { campo: 'Nombre', valor: nombre, obligatorio: true, maximo: 100 },
      { campo: 'Descripción', valor: descripcion, obligatorio: true, maximo: 255 },
    ]);
    const mensajes = [...r.mensajes, ...validarItems()];
    setPorCampo(r.porCampo);
    if (mensajes.length) {
      setErrores({ titulo: 'No se puede guardar la opción de respuesta', items: mensajes });
      return;
    }
    setErrores(null);
    setGuardando(true);
    const lista = items.map((i) => ({ id: i.id, nroOrden: Number(i.nroOrden), descripcion: i.descripcion.trim() }));
    try {
      if (esAlta) {
        await api.crearOpcionRespuesta({ nombre: nombre.trim(), descripcion: descripcion.trim(), items: lista });
        const otra = await avisos.abrir({
          tipo: 'informativa',
          titulo: 'Opción de respuesta guardada',
          mensaje: `La opción ${nombre.trim()} se ha creado correctamente. ¿Desea cargar otra?`,
          aceptar: 'Sí',
          rechazar: 'No',
        });
        if (otra) {
          setNombre('');
          setDescripcion('');
          setItems([nuevoItem(1), nuevoItem(2)]);
        } else {
          alTerminar(true);
        }
      } else {
        await api.modificarOpcionRespuesta(opcion.id, { descripcion: descripcion.trim(), items: lista });
        await avisos.informar('Opción de respuesta modificada', 'La operación ha culminado con éxito.');
        alTerminar(true);
      }
    } catch (err) {
      if (err instanceof ApiError && !err.sinConexion) setErrores({ titulo: err.message, items: err.detalles });
      else await avisos.error(err, 'No se pudo guardar la opción de respuesta');
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Hoja
      titulo={esAlta ? 'Alta de opción de respuesta' : 'Modificar opción de respuesta'}
      cu={esAlta ? 'CU029' : 'CU030'}
      bajada={
        esAlta
          ? 'Los campos con * son obligatorios. El orden de visualización va de 1 en 1.'
          : 'El nombre y la cantidad de opciones no se pueden modificar. Se pueden cambiar la descripción y el orden de visualización, que va de 1 en 1.'
      }
      ancho="angosto"
    >
      <form onSubmit={aceptar} noValidate>
        <AvisoErrores titulo={errores?.titulo ?? null} items={errores?.items} refAviso={avisoRef} />
        <div className="grid gap-5">
          <CampoTexto
            etiqueta="Nombre"
            obligatorio
            valor={nombre}
            alCambiar={setNombre}
            maximo={100}
            error={porCampo['Nombre']}
            disabled={!esAlta}
          />
          <CampoTexto
            etiqueta="Descripción"
            obligatorio
            valor={descripcion}
            alCambiar={setDescripcion}
            maximo={255}
            error={porCampo['Descripción']}
          />
        </div>

        <div className="mt-8">
          <Seccion
            titulo="Lista de opciones"
            accion={
              esAlta && (
                <Boton onClick={() => setItems((l) => [...l, nuevoItem(l.length + 1)])} className="h-9 px-3.5 text-chica">
                  Agregar opción
                </Boton>
              )
            }
          >
            <div className="overflow-x-auto border-y border-linea">
              <table className="w-full border-collapse text-chica">
                <caption className="sr-only">Opciones con su orden de visualización</caption>
                <thead>
                  <tr>
                    <th scope="col" className="w-36 border-b-2 border-tinta px-3 py-2.5 text-left font-bold text-tinta">
                      Orden de visualización
                    </th>
                    <th scope="col" className="border-b-2 border-tinta px-3 py-2.5 text-left font-bold text-tinta">
                      Descripción
                    </th>
                    {esAlta && (
                      <th scope="col" className="w-24 border-b-2 border-tinta px-3 py-2.5">
                        <span className="sr-only">Quitar</span>
                      </th>
                    )}
                  </tr>
                </thead>
                <tbody>
                  {items.map((item, idx) => (
                    <tr key={item.clave} className={`border-b border-linea last:border-b-0 ${idx % 2 ? 'bg-rosa-suave' : 'bg-papel'}`}>
                      <td className="px-3 py-1.5">
                        <input
                          inputMode="numeric"
                          value={item.nroOrden}
                          onChange={(e) => cambiar(item.clave, { nroOrden: e.target.value.replace(/\D/g, '') })}
                          aria-label={`Orden de la opción ${idx + 1}`}
                          className="cifras h-9 w-20 rounded-[4px] border border-grafito-claro bg-papel px-2 text-cuerpo disabled:bg-rosa-suave"
                        />
                      </td>
                      <td className="px-3 py-1.5">
                        <input
                          value={item.descripcion}
                          onChange={(e) => cambiar(item.clave, { descripcion: e.target.value.toLocaleUpperCase('es-AR') })}
                          aria-label={`Descripción de la opción ${idx + 1}`}
                          maxLength={120}
                          className="dato h-9 w-full rounded-[4px] border border-grafito-claro bg-papel px-2 text-cuerpo"
                        />
                      </td>
                      {esAlta && (
                        <td className="px-3 py-1.5 text-right">
                          <button
                            type="button"
                            onClick={() => setItems((l) => l.filter((i) => i.clave !== item.clave))}
                            className="h-9 rounded-[4px] px-2 text-chica font-bold text-grafito underline decoration-linea underline-offset-4 hover:decoration-tinta"
                          >
                            Quitar
                          </button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Seccion>
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
