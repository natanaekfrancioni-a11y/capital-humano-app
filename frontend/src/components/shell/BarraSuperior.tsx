'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import type { ReactNode } from 'react';
import { IconoRecargar, IconoUsuario, IconoVolver } from '@/components/ui/Iconos';
import { useControlVolver } from './Volver';

const BOTON_ICONO =
  'inline-flex h-10 w-10 items-center justify-center rounded-[4px] text-grafito hover:bg-rosa-suave';

/** Barra de los wireframes: volver y recargar a la izquierda, usuario a la derecha. */
export function BarraSuperior({ usuario, alSalir, inicio = '/inicio' }: { usuario?: ReactNode; alSalir?: () => void; inicio?: string }) {
  const router = useRouter();
  const control = useControlVolver();

  function volver() {
    const accion = control?.actual();
    if (accion) {
      accion();
      return;
    }
    if (window.history.length > 1) router.back();
    else router.push(inicio);
  }

  return (
    <header className="no-imprimir sticky top-0 z-20 border-b border-linea bg-papel">
      <div className="mx-auto flex h-14 max-w-[72rem] items-center gap-1 px-3 sm:px-5">
        <button type="button" onClick={volver} className={BOTON_ICONO} aria-label="Volver">
          <IconoVolver />
        </button>
        <button type="button" onClick={() => window.location.reload()} className={BOTON_ICONO} aria-label="Recargar la pantalla">
          <IconoRecargar />
        </button>
        <Link
          href={inicio}
          className="ml-2 rounded-[4px] px-1 text-cuerpo font-extrabold tracking-[-0.01em] text-grafito"
        >
          Capit@l Humano
        </Link>
        <div className="ml-auto flex items-center gap-3">
          {usuario && (
            <span className="hidden items-center gap-2 text-chica text-grafito-claro sm:inline-flex">
              <IconoUsuario className="h-[18px] w-[18px]" />
              <span className="dato">{usuario}</span>
            </span>
          )}
          {alSalir && (
            <button
              type="button"
              onClick={alSalir}
              className="h-9 rounded-[4px] border-[1.5px] border-grafito px-3.5 text-chica font-bold text-grafito hover:bg-rosa-suave"
            >
              Cerrar sesión
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
