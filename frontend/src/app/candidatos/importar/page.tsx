'use client';

import { useEffect, useId, useRef, useState, type FormEvent } from 'react';
import { PantallaConsultor } from '@/components/shell/PantallaConsultor';
import { useSalir } from '@/components/shell/useSalir';
import { Boton } from '@/components/ui/Boton';
import { useAvisos } from '@/components/ui/Dialogo';
import { IconoArchivo } from '@/components/ui/Iconos';
import { BarraAcciones, Hoja, Seccion } from '@/components/ui/Hoja';
import { api } from '@/lib/api';

const COLUMNAS = [
  'Nro candidato',
  'Apellido',
  'Nombre',
  'Nro documento',
  'Tipo documento (DNI, LE, LC, PP)',
  'Fecha de nacimiento (aaaa-mm-dd)',
  'Género (H/M)',
  'Nacionalidad',
  'Email',
  'Escolaridad',
  'Eliminado (True/False)',
];

export default function ImportarCandidatosPage() {
  return <PantallaConsultor>{() => <ImportarCandidatos />}</PantallaConsultor>;
}

/** Lee el CSV en UTF-8; si trae caracteres inválidos (archivo guardado por Excel en Windows), usa Windows-1252. */
async function leerTexto(archivo: File): Promise<string> {
  const bytes = await archivo.arrayBuffer();
  const utf8 = new TextDecoder('utf-8').decode(bytes);
  return utf8.includes('�') ? new TextDecoder('windows-1252').decode(bytes) : utf8;
}

/** CU006 - Importar candidatos */
function ImportarCandidatos() {
  const avisos = useAvisos();
  const salir = useSalir();
  const inputId = useId();
  const inputRef = useRef<HTMLInputElement>(null);
  const [archivo, setArchivo] = useState<File | null>(null);
  const [filas, setFilas] = useState<number | null>(null);
  const [importando, setImportando] = useState(false);
  const [segundos, setSegundos] = useState(0);

  useEffect(() => {
    if (!importando) return;
    const id = window.setInterval(() => setSegundos((s) => s + 1), 1000);
    return () => window.clearInterval(id);
  }, [importando]);

  async function elegir(f: File | null) {
    setArchivo(f);
    setFilas(null);
    if (!f) return;
    const texto = await leerTexto(f);
    setFilas(Math.max(0, texto.split(/\r?\n/).filter((l) => l.trim()).length - 1));
  }

  async function importar(e: FormEvent) {
    e.preventDefault();
    if (!archivo) {
      await avisos.validacion('Archivo obligatorio', 'Seleccione el archivo CSV a importar.');
      return;
    }
    if (!archivo.name.toLowerCase().endsWith('.csv')) {
      await avisos.validacion('Formato no válido', 'El archivo a importar debe tener extensión .csv.');
      return;
    }
    setSegundos(0);
    setImportando(true);
    try {
      const contenido = await leerTexto(archivo);
      const r = await api.importarCandidatos(archivo.name, contenido);
      setImportando(false);
      await avisos.abrir({
        tipo: 'informativa',
        titulo: 'Importación realizada con éxito',
        mensaje: (
          <dl className="grid grid-cols-[1fr_auto] gap-x-8 gap-y-1">
            <dt>Candidatos creados</dt>
            <dd className="cifras text-right font-extrabold">{r.creados}</dd>
            <dt>Candidatos actualizados</dt>
            <dd className="cifras text-right font-extrabold">{r.actualizados}</dd>
            <dt>Candidatos eliminados</dt>
            <dd className="cifras text-right font-extrabold">{r.eliminados}</dd>
            <div className="col-span-2 my-1 border-t border-linea" aria-hidden="true" />
            <dt className="text-grafito-claro">Filas procesadas</dt>
            <dd className="cifras text-right text-grafito-claro">{r.totalProcesados}</dd>
          </dl>
        ),
      });
      salir();
    } catch (err) {
      setImportando(false);
      await avisos.error(err, 'No se pudo importar el archivo');
    }
  }

  return (
    <Hoja titulo="Importar candidatos" cu="CU006" bajada="Carga los candidatos exportados por el sistema de gestión de candidatos y CV." ancho="angosto">
      <form onSubmit={importar}>
        <label htmlFor={inputId} className="text-chica font-bold">
          Archivo CSV
          <span className="text-tinta" aria-hidden="true">
            {' '}*
          </span>
        </label>
        <div className="mt-1.5 flex flex-wrap items-center gap-3 rounded-[4px] border border-dashed border-grafito-claro bg-rosa-suave px-4 py-4">
          <IconoArchivo className="h-6 w-6 text-tinta" />
          <div className="min-w-0 flex-1">
            {archivo ? (
              <>
                <p className="truncate font-bold">{archivo.name}</p>
                <p className="cifras text-chica text-grafito-claro">
                  {(archivo.size / 1024).toLocaleString('es-AR', { maximumFractionDigits: 1 })} KB
                  {filas !== null && `, ${filas} filas de candidatos`}
                </p>
              </>
            ) : (
              <p className="text-grafito-claro">Ningún archivo seleccionado.</p>
            )}
          </div>
          <Boton onClick={() => inputRef.current?.click()} disabled={importando}>
            Examinar…
          </Boton>
          <input
            ref={inputRef}
            id={inputId}
            type="file"
            accept=".csv,text/csv"
            className="sr-only"
            onChange={(e) => elegir(e.target.files?.[0] ?? null)}
          />
        </div>

        {importando && (
          <div className="mt-5" role="status" aria-live="polite">
            <div className="flex justify-between text-chica">
              <span className="font-bold">Importando candidatos…</span>
              <span className="cifras text-grafito-claro">{segundos} s</span>
            </div>
            <div className="mt-2 h-2 overflow-hidden rounded-full bg-rosa-marcado">
              <div className="barra-avance h-full w-1/3 rounded-full bg-tinta" />
            </div>
          </div>
        )}

        <div className="mt-8">
          <Seccion titulo="Formato esperado">
            <p className="text-chica text-grafito-claro">
              Columnas separadas por punto y coma (;), en este orden. La primera fila tiene los nombres de las columnas. El número de
              candidato identifica a cada candidato: si ya existe, se actualiza.
            </p>
            <ol className="mt-3 list-decimal gap-x-8 space-y-1 pl-6 text-chica sm:columns-2">
              {COLUMNAS.map((c) => (
                <li key={c}>{c}</li>
              ))}
            </ol>
          </Seccion>
        </div>

        <BarraAcciones
          izquierda={
            <Boton onClick={salir} disabled={importando}>
              Cancelar
            </Boton>
          }
          derecha={
            <Boton type="submit" variante="principal" ocupado={importando} textoOcupado="Importando…">
              Importar
            </Boton>
          }
        />
      </form>
    </Hoja>
  );
}
