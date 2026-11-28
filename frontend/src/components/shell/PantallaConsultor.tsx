'use client';

import { useRouter } from 'next/navigation';
import { useEffect, type ReactNode } from 'react';
import { cerrarSesion, useSesionConsultor } from '@/lib/sesion';
import type { SesionConsultor } from '@/lib/tipos';
import { BarraSuperior } from './BarraSuperior';
import { VolverProvider } from './Volver';

/** marco de las pantallas del consultor: pide haber ingresado por el CU001 y agrega la barra de arriba */
export function PantallaConsultor({ children }: { children: (sesion: SesionConsultor) => ReactNode }) {
  const router = useRouter();
  const sesion = useSesionConsultor();

  useEffect(() => {
    if (sesion === null) router.replace('/');
  }, [sesion, router]);

  function salir() {
    cerrarSesion();
    router.replace('/');
  }

  return (
    <VolverProvider>
      <BarraSuperior usuario={sesion?.nombreCompleto} alSalir={sesion ? salir : undefined} />
      <main className="px-3 py-6 sm:px-5 sm:py-10">
        {sesion ? (
          children(sesion)
        ) : (
          <p className="mx-auto max-w-[72rem] px-2 text-grafito-claro" role="status">
            Verificando la sesión…
          </p>
        )}
      </main>
    </VolverProvider>
  );
}
