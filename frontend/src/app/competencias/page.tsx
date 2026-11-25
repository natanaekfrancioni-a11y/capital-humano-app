'use client';

import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
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
import type { Competencia } from '@/lib/tipos';
import { validar } from '@/lib/validacion';

const buscarCompetencias = (nombre: string) => api.buscarCompetencias(nombre);
const TIPOS = [
  { valor: 'TECNICA', texto: 'TÉCNICA' },
  { valor: 'ACTITUDINAL', texto: 'ACTITUDINAL' },
];

export default function CompetenciasPage() {
  return <PantallaConsultor>{(sesion) => <Competencias usuario={sesion.username} />}</PantallaConsultor>;
}

/** CU012 - Gestión de competencias (invoca CU013, CU014 y CU015). */
function Competencias({ usuario }: { usuario: string }) {
  const avisos = useAvisos();
  const salir = useSalir();
  const [formulario, setFormulario] = useState<{ competencia: Competencia | null } | null>(null);
  const [nombre, setNombre] = useState('');
  const b = useBusqueda(buscarCompetencias);
  const { refrescar } = b;

  // CU015 - Dar de baja competencia: primero se constata que no esté en uso.
  async function eliminar() {
    const c = b.seleccionado;
    if (!c) return;
    try {
      const uso = await api.competenciaEnUso(c.id);
      if (uso.enUso) {
        await avisos.abrir({ tipo: 'error', titulo: 'No se puede eliminar', mensaje: uso.mensaje, cualquierTecla: true });
        return;
      }
      const ok = await avisos.confirmar('Baja de competencia', uso.mensaje, { aceptar: 'Eliminar', rechazar: 'Cancelar', peligro: true });
      if (!ok) return;
      await api.bajaCompetencia(c.id, usuario);
      b.setSeleccion(null);
      await avisos.informar('Competencia eliminada', `Los datos de la competencia ${c.nombre} han sido eliminados del sistema.`, {
        cualquierTecla: true,
      });
      await refrescar();
    } catch (e) {
      await avisos.error(e, 'No se pudo eliminar la competencia');
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
    return <FormularioCompetencia key={formulario.competencia?.id ?? 'alta'} competencia={formulario.competencia} alTerminar={terminar} />;
  }

  return (
    <Hoja
      titulo="Competencias"
      cu="CU012"
      bajada="Buscar por nombre. Si busca sin completar el nombre, se listan todas las competencias."
    >
      <form
        onSubmit={(e: FormEvent) => {
          e.preventDefault();
          b.ejecutar(nombre);
        }}
        className="grid items-end gap-4 sm:grid-cols-[minmax(0,26rem)_auto]"
      >
        <CampoTexto etiqueta="Nombre de competencia" valor={nombre} alCambiar={setNombre} maxLength={100} />
        <Boton type="submit" variante="secundario" ocupado={b.buscando} textoOcupado="Buscando…">
          Buscar
        </Boton>
      </form>

      <div className="mt-7">
        <Grilla
          etiqueta="Competencias encontradas"
          columnas={[
            { titulo: 'Código', celda: (c: Competencia) => c.codigo, dato: true, ancho: '12rem' },
            { titulo: 'Nombre', celda: (c: Competencia) => c.nombre, dato: true },
          ]}
          filas={b.resultados ?? []}
          clave={(c) => c.id}
          seleccion={{
            modo: 'unica',
            nombre: 'competencia',
            seleccionadas: b.seleccion === null ? [] : [b.seleccion],
            alCambiar: (ids) => b.setSeleccion(Number(ids[0])),
            etiquetaFila: (c) => `Seleccionar la competencia ${c.nombre}`,
          }}
          vacio={b.resultados === null ? 'Presione Buscar para ver las competencias.' : 'No hay competencias con ese nombre.'}
        />
      </div>

      <BarraAcciones
        izquierda={<Boton onClick={salir}>Salir</Boton>}
        derecha={
          <>
            <Boton disabled={!b.seleccionado} onClick={() => b.seleccionado && setFormulario({ competencia: b.seleccionado })}>
              Modificar
            </Boton>
            <Boton disabled={!b.seleccionado} onClick={eliminar}>
              Eliminar
            </Boton>
            <Boton variante="principal" onClick={() => setFormulario({ competencia: null })}>
              Nuevo
            </Boton>
          </>
        }
      />
    </Hoja>
  );
}

/** CU013 - Dar de alta competencia / CU014 - Modificar competencia */
function FormularioCompetencia({
  competencia,
  alTerminar,
}: {
  competencia: Competencia | null;
  alTerminar: (huboCambios: boolean) => void;
}) {
  const avisos = useAvisos();
  const esAlta = competencia === null;
  const avisoRef = useRef<HTMLDivElement>(null);
  const [codigo, setCodigo] = useState(competencia?.codigo ?? '');
  const [nombre, setNombre] = useState(competencia?.nombre ?? '');
  const [descripcion, setDescripcion] = useState(competencia?.descripcion ?? '');
  const [tipo, setTipo] = useState(competencia?.tipo ?? 'TECNICA');
  const [errores, setErrores] = useState<ErroresFormulario | null>(null);
  const [porCampo, setPorCampo] = useState<Record<string, string>>({});
  const [guardando, setGuardando] = useState(false);

  useVolver(useCallback(() => alTerminar(false), [alTerminar]));
  useEffect(() => {
    if (errores) avisoRef.current?.focus();
  }, [errores]);

  async function aceptar(e: FormEvent) {
    e.preventDefault();
    const r = validar([
      { campo: 'Código', valor: codigo, obligatorio: true, maximo: 30 },
      { campo: 'Nombre', valor: nombre, obligatorio: true, maximo: 100 },
      { campo: 'Descripción', valor: descripcion, obligatorio: true, maximo: 255 },
    ]);
    setPorCampo(r.porCampo);
    if (r.mensajes.length) {
      setErrores({ titulo: 'No se puede guardar la competencia', items: r.mensajes });
      return;
    }
    setErrores(null);
    setGuardando(true);
    try {
      if (esAlta) {
        await api.crearCompetencia({ codigo: codigo.trim(), nombre: nombre.trim(), descripcion: descripcion.trim(), tipo });
        const otra = await avisos.abrir({
          tipo: 'informativa',
          titulo: 'Competencia guardada',
          mensaje: `La competencia ${nombre.trim()} se ha creado correctamente. ¿Desea cargar otra?`,
          aceptar: 'Sí',
          rechazar: 'No',
        });
        if (otra) {
          setCodigo('');
          setNombre('');
          setDescripcion('');
          setTipo('TECNICA');
        } else {
          alTerminar(true);
        }
      } else {
        await api.modificarCompetencia(competencia.id, { nombre: nombre.trim(), descripcion: descripcion.trim() });
        await avisos.informar('Competencia modificada', 'La operación ha culminado con éxito.');
        alTerminar(true);
      }
    } catch (err) {
      if (err instanceof ApiError && !err.sinConexion) setErrores({ titulo: err.message, items: err.detalles });
      else await avisos.error(err, 'No se pudo guardar la competencia');
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Hoja
      titulo={esAlta ? 'Alta de competencia' : 'Modificar competencia'}
      cu={esAlta ? 'CU013' : 'CU014'}
      bajada={esAlta ? 'Los campos con * son obligatorios.' : 'El código no se puede modificar.'}
      ancho="angosto"
    >
      <form onSubmit={aceptar} noValidate>
        <AvisoErrores titulo={errores?.titulo ?? null} items={errores?.items} refAviso={avisoRef} />
        <div className="grid gap-5 sm:grid-cols-2">
          <CampoTexto
            etiqueta="Código"
            obligatorio
            valor={codigo}
            alCambiar={setCodigo}
            maximo={30}
            error={porCampo['Código']}
            disabled={!esAlta}
          />
          <CampoSeleccion
            etiqueta="Tipo"
            valor={tipo}
            alCambiar={(v) => setTipo(v || 'TECNICA')}
            opciones={TIPOS}
            textoVacio="Seleccionar…"
            disabled={!esAlta}
          />
          <CampoTexto
            className="sm:col-span-2"
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
