'use client';

import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Boton } from '@/components/ui/Boton';
import { CampoTexto } from '@/components/ui/Campo';
import { IconoCarpeta } from '@/components/ui/Iconos';
import { elegirCarpeta, puedeElegirCarpeta, type ManejadorCarpeta } from '@/lib/excel';

interface Props {
  abierto: boolean;
  nombreInicial: string;
  alCancelar: () => void;
  alGuardar: (carpeta: ManejadorCarpeta | null, nombreArchivo: string) => Promise<void>;
}

/** CU025: pantalla para elegir la carpeta y guardar el Excel (en navegadores sin selector se descarga) */
export function GuardarArchivo({ abierto, nombreInicial, alCancelar, alGuardar }: Props) {
  const ref = useRef<HTMLDialogElement>(null);
  const [carpeta, setCarpeta] = useState<ManejadorCarpeta | null>(null);
  const [nombre, setNombre] = useState(nombreInicial);
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);
  const conSelector = puedeElegirCarpeta();

  useEffect(() => {
    const d = ref.current;
    if (!d) return;
    if (abierto && !d.open) d.showModal();
    if (!abierto && d.open) d.close();
  }, [abierto]);

  async function examinar() {
    const elegida = await elegirCarpeta();
    if (elegida) {
      setCarpeta(elegida);
      setError(null);
    }
  }

  async function guardar(e: FormEvent) {
    e.preventDefault();
    const faltan = [conSelector && !carpeta ? 'Ubicación' : null, nombre.trim() === '' ? 'Nombre de archivo' : null].filter(Boolean);
    if (faltan.length) {
      setError(`Los siguientes campos son obligatorios: ${faltan.join(', ')}.`);
      return;
    }
    if (nombre.trim().length > 100) {
      setError('El nombre de archivo supera los 100 caracteres.');
      return;
    }
    setGuardando(true);
    try {
      await alGuardar(carpeta, nombre.trim());
    } catch (err) {
      setError(err instanceof Error ? `No se pudo guardar el archivo: ${err.message}` : 'No se pudo guardar el archivo.');
    } finally {
      setGuardando(false);
    }
  }

  return (
    <dialog
      ref={ref}
      aria-labelledby="guardar-titulo"
      onCancel={(e) => {
        e.preventDefault();
        alCancelar();
      }}
      className="m-auto w-[min(94vw,36rem)] rounded-[6px] border border-linea bg-papel p-0 text-grafito shadow-[0_18px_50px_rgba(37,39,45,0.22)] backdrop:bg-grafito/45"
    >
      <form onSubmit={guardar} noValidate className="px-6 pb-5 pt-5">
        <h2 id="guardar-titulo" className="regla-doble pb-2 text-subtitulo font-extrabold">
          Guardar archivo
        </h2>
        {error && (
          <p role="alert" className="mt-4 border-l-4 border-error bg-[#fdf1f0] px-3 py-2 text-chica font-bold text-error">
            {error}
          </p>
        )}
        <div className="mt-5 grid gap-4">
          <div className="grid items-end gap-3 sm:grid-cols-[1fr_auto]">
            <CampoTexto
              etiqueta="Ubicación"
              obligatorio={conSelector}
              valor={conSelector ? (carpeta?.name ?? '') : 'Carpeta de descargas del navegador'}
              alCambiar={() => undefined}
              readOnly
              mayusculas={false}
              placeholder="Elija una carpeta con Examinar"
            />
            <Boton onClick={examinar} disabled={!conSelector} autoFocus={conSelector}>
              <IconoCarpeta className="h-[18px] w-[18px]" />
              Examinar
            </Boton>
          </div>
          {!conSelector && (
            <p className="-mt-2 text-chica text-grafito-claro">
              Este navegador no permite elegir la carpeta: el archivo se descarga. Con Chrome o Edge se puede elegir.
            </p>
          )}
          <CampoTexto etiqueta="Nombre de archivo" obligatorio valor={nombre} alCambiar={setNombre} mayusculas={false} maximo={100} />
          <CampoTexto etiqueta="Tipo de archivo" valor="Excel (*.xls)" alCambiar={() => undefined} readOnly mayusculas={false} />
        </div>
        <p className="mt-3 text-chica text-grafito-claro">Si ya existe un archivo con ese nombre en la carpeta, se reemplaza.</p>
        <div className="mt-6 flex justify-end gap-2.5 border-t border-linea pt-4">
          <Boton onClick={alCancelar}>Cancelar</Boton>
          <Boton type="submit" variante="principal" ocupado={guardando} textoOcupado="Guardando…">
            Guardar
          </Boton>
        </div>
      </form>
    </dialog>
  );
}
