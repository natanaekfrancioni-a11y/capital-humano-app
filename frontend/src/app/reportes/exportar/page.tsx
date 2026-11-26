'use client';

import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { GuardarArchivo } from '@/components/evaluar/GuardarArchivo';
import { PantallaConsultor } from '@/components/shell/PantallaConsultor';
import { useSalir } from '@/components/shell/useSalir';
import { Boton } from '@/components/ui/Boton';
import { CampoCombo, CampoTexto } from '@/components/ui/Campo';
import { useAvisos } from '@/components/ui/Dialogo';
import { Grilla } from '@/components/ui/Grilla';
import { BarraAcciones, Hoja, Seccion } from '@/components/ui/Hoja';
import { api } from '@/lib/api';
import { descargar, guardarEnCarpeta, type ManejadorCarpeta } from '@/lib/excel';
import { fecha, fechaHora } from '@/lib/formato';
import type { ArchivoExportado, CandidatoExportable, Puesto, PuestoExportable } from '@/lib/tipos';

export default function ExportarResultadosPage() {
  return <PantallaConsultor>{() => <ExportarResultados />}</PantallaConsultor>;
}

interface Filtros {
  nombre: string;
  empresa: string;
  desde: string;
  hasta: string;
}

interface Periodo {
  desde?: string;
  hasta?: string;
}

const SIN_FILTROS: Filtros = { nombre: '', empresa: '', desde: '', hasta: '' };
const TIPO_ARCHIVO = 'CSV separado por punto y coma (*.csv)';

const contiene = (valor: string, filtro: string) => !filtro.trim() || valor.toUpperCase().includes(filtro.trim().toUpperCase());

function textoPeriodo({ desde, hasta }: Periodo): string | null {
  if (desde && hasta) return `Cuestionarios finalizados entre el ${fecha(desde)} y el ${fecha(hasta)}.`;
  if (desde) return `Cuestionarios finalizados desde el ${fecha(desde)}.`;
  if (hasta) return `Cuestionarios finalizados hasta el ${fecha(hasta)}.`;
  return null;
}

/** CU007 - Exportar resultados */
function ExportarResultados() {
  const avisos = useAvisos();
  const salir = useSalir();
  const [todos, setTodos] = useState<Puesto[]>([]);
  const [filtros, setFiltros] = useState<Filtros>(SIN_FILTROS);
  const [buscando, setBuscando] = useState(false);
  // Período de la última búsqueda: con él se listan y exportan los candidatos, aunque después se editen las fechas.
  const [periodo, setPeriodo] = useState<Periodo>({});
  const [funciones, setFunciones] = useState<PuestoExportable[] | null>(null);
  const [puestoId, setPuestoId] = useState<number | null>(null);
  const [candidatos, setCandidatos] = useState<CandidatoExportable[] | null>(null);
  const [exportando, setExportando] = useState(false);
  const [archivo, setArchivo] = useState<ArchivoExportado | null>(null);

  useEffect(() => {
    api.buscarPuestos().then(setTodos).catch((e) => avisos.error(e, 'No se pudieron cargar los puestos'));
  }, [avisos]);

  const empresas = useMemo(() => [...new Set(todos.map((p) => p.empresa))].sort(), [todos]);
  const nombresPuestos = useMemo(
    () => [...new Set(todos.filter((p) => contiene(p.empresa, filtros.empresa)).map((p) => p.nombre))].sort(),
    [todos, filtros.empresa],
  );
  const funcion = funciones?.find((f) => f.id === puestoId) ?? null;
  const cambiar = (campo: keyof Filtros) => (valor: string) => setFiltros((f) => ({ ...f, [campo]: valor }));

  // Pasos 2 y 3: al menos un filtro; las fechas son las de finalización del cuestionario.
  async function buscar(e: FormEvent) {
    e.preventDefault();
    if (Object.values(filtros).every((v) => v.trim() === '')) {
      await avisos.validacion('Filtros de búsqueda', 'Complete al menos un filtro para buscar funciones.');
      return;
    }
    if (filtros.desde && filtros.hasta && filtros.desde > filtros.hasta) {
      await avisos.validacion('Período inválido', 'La fecha desde no puede ser posterior a la fecha hasta.');
      return;
    }
    const buscado: Periodo = { desde: filtros.desde || undefined, hasta: filtros.hasta || undefined };
    setBuscando(true);
    try {
      const encontradas = await api.buscarPuestosExportables({ nombre: filtros.nombre, empresa: filtros.empresa, ...buscado });
      setFunciones(encontradas);
      setPeriodo(buscado);
      setPuestoId(null);
      setCandidatos(null);
    } catch (err) {
      await avisos.error(err, 'No se pudieron buscar las funciones');
    } finally {
      setBuscando(false);
    }
  }

  // Pasos 4 y 5: candidatos evaluados (cuestionario completo) en la función elegida.
  async function elegirFuncion(id: number) {
    setPuestoId(id);
    setCandidatos(null);
    try {
      setCandidatos(await api.candidatosExportables(id, periodo));
    } catch (err) {
      setCandidatos([]);
      await avisos.error(err, 'No se pudieron obtener los candidatos evaluados');
    }
  }

  // Paso 6 / 6.A. Con el archivo armado se abre la pantalla para elegir la carpeta (paso 7).
  async function exportar() {
    if (puestoId === null || funcion === null) {
      await avisos.validacion('Selección obligatoria', 'Debe seleccionar una función de la lista para exportar los resultados.');
      return;
    }
    if (candidatos !== null && candidatos.length === 0) {
      await avisos.validacion('Sin candidatos evaluados', `La función ${funcion.nombre} no tiene candidatos evaluados para exportar.`);
      return;
    }
    setExportando(true);
    try {
      setArchivo(await api.exportarResultados(puestoId, periodo));
    } catch (err) {
      await avisos.error(err, 'No se pudo generar el archivo');
    } finally {
      setExportando(false);
    }
  }

  // Pasos 8 a 10: guarda el archivo y vuelve a la pantalla principal.
  async function guardarArchivo(carpeta: ManejadorCarpeta | null, nombre: string) {
    if (!archivo) return;
    const nombreFinal = nombre.toLowerCase().endsWith('.csv') ? nombre : `${nombre}.csv`;
    // La marca BOM al principio hace que Excel lea el archivo como UTF-8 y muestre bien los acentos.
    const datos = new Blob(['﻿', archivo.contenido], { type: 'text/csv;charset=utf-8' });
    if (carpeta) await guardarEnCarpeta(carpeta, nombreFinal, datos);
    else descargar(nombreFinal, datos);
    setArchivo(null);
    await avisos.informar(
      'Archivo guardado',
      carpeta ? `Se guardó ${nombreFinal} en la carpeta ${carpeta.name}.` : `Se descargó ${nombreFinal}.`,
    );
    salir();
  }

  async function cancelar() {
    if (funciones !== null) {
      const ok = await avisos.confirmar('Confirmación de salida', '¿Está seguro que desea cancelar la operación? Se perderá la búsqueda actual.', {
        aceptar: 'Sí, salir',
        rechazar: 'No',
      });
      if (!ok) return;
    }
    salir();
  }

  const periodoBuscado = textoPeriodo(periodo);

  return (
    <Hoja
      titulo="Exportar resultados"
      cu="CU007"
      bajada="Genera un archivo CSV con el resultado de cada candidato evaluado en una función, competencia por competencia."
    >
      <form onSubmit={buscar} noValidate>
        <div className="grid items-end gap-4 md:grid-cols-2 xl:grid-cols-[1.2fr_1.2fr_0.8fr_0.8fr_auto]">
          <CampoCombo
            etiqueta="Nombre de función o puesto"
            valor={filtros.nombre}
            alCambiar={cambiar('nombre')}
            opciones={nombresPuestos}
            maxLength={50}
          />
          <CampoCombo etiqueta="Empresa" valor={filtros.empresa} alCambiar={cambiar('empresa')} opciones={empresas} maxLength={50} />
          <CampoTexto
            etiqueta="Finalizado desde"
            type="date"
            valor={filtros.desde}
            alCambiar={cambiar('desde')}
            mayusculas={false}
            max={filtros.hasta || undefined}
          />
          <CampoTexto
            etiqueta="Finalizado hasta"
            type="date"
            valor={filtros.hasta}
            alCambiar={cambiar('hasta')}
            mayusculas={false}
            min={filtros.desde || undefined}
          />
          <Boton type="submit" ocupado={buscando} textoOcupado="Buscando…">
            Buscar
          </Boton>
        </div>
        <p className="mt-2 text-chica text-grafito-claro">
          Complete al menos un filtro. Las fechas se refieren a la finalización del cuestionario.
        </p>
      </form>

      <div className="mt-8 grid gap-8 xl:grid-cols-[0.85fr_1.15fr]">
        <Seccion titulo="Funciones">
          <Grilla
            etiqueta="Funciones que coinciden con la búsqueda"
            columnas={[
              { titulo: 'Puesto', celda: (p: PuestoExportable) => p.nombre, dato: true },
              { titulo: 'Empresa', celda: (p: PuestoExportable) => p.empresa, dato: true },
              { titulo: 'Evaluados', celda: (p: PuestoExportable) => p.evaluados, cifras: true, alinear: 'derecha', ancho: '6.5rem' },
            ]}
            filas={funciones ?? []}
            clave={(p) => p.id}
            seleccion={{
              modo: 'unica',
              nombre: 'funcion',
              seleccionadas: puestoId === null ? [] : [puestoId],
              alCambiar: (ids) => elegirFuncion(Number(ids[0])),
              etiquetaFila: (p) => `Seleccionar la función ${p.nombre} de ${p.empresa}`,
            }}
            vacio={funciones === null ? 'Complete al menos un filtro y presione Buscar.' : 'No hay funciones que coincidan con los filtros.'}
          />
          {periodoBuscado && funciones !== null && <p className="mt-2 text-chica text-grafito-claro">{periodoBuscado}</p>}
        </Seccion>

        <Seccion titulo="Candidatos evaluados">
          <Grilla
            etiqueta="Candidatos con cuestionario completo en la función seleccionada"
            columnas={[
              { titulo: 'Apellido', celda: (c: CandidatoExportable) => c.apellido, dato: true },
              { titulo: 'Nombre', celda: (c: CandidatoExportable) => c.nombre, dato: true },
              { titulo: 'Documento', celda: (c: CandidatoExportable) => `${c.tipoDocumento} ${c.nroDocumento}`, cifras: true },
              { titulo: 'Nro candidato', celda: (c: CandidatoExportable) => c.nroCandidato, cifras: true },
              { titulo: 'Finalizó', celda: (c: CandidatoExportable) => fechaHora(c.fechaFin), cifras: true },
            ]}
            filas={candidatos ?? []}
            clave={(c) => c.cuestionarioId}
            vacio={
              puestoId === null
                ? 'Elija una función para ver sus candidatos evaluados.'
                : candidatos === null
                  ? 'Buscando candidatos…'
                  : 'La función no tiene candidatos evaluados en el período.'
            }
          />
          {funcion !== null && candidatos !== null && candidatos.length > 0 && (
            <p className="mt-2 text-chica text-grafito-claro">
              El archivo tendrá una fila por candidato y por cada competencia que exige {funcion.nombre}.
            </p>
          )}
        </Seccion>
      </div>

      <BarraAcciones
        izquierda={<Boton onClick={cancelar}>Cancelar</Boton>}
        derecha={
          <Boton variante="principal" onClick={exportar} ocupado={exportando} textoOcupado="Generando…">
            Exportar
          </Boton>
        }
      />

      {archivo && (
        <GuardarArchivo
          key={archivo.nombreArchivo}
          abierto
          nombreInicial={archivo.nombreArchivo.replace(/\.csv$/i, '')}
          tipoArchivo={TIPO_ARCHIVO}
          alCancelar={() => setArchivo(null)}
          alGuardar={guardarArchivo}
        />
      )}
    </Hoja>
  );
}
