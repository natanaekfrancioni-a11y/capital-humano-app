'use client';

import { createContext, useContext, useEffect, useMemo, useRef, type ReactNode } from 'react';

type Accion = () => void;

interface ControlVolver {
  registrar: (accion: Accion) => void;
  quitar: (accion: Accion) => void;
  /** devuelve la acción registrada por la pantalla actual, si hay una */
  actual: () => Accion | null;
}

const VolverContexto = createContext<ControlVolver | null>(null);

/** deja que una pantalla defina qué hace la flecha "Volver" de la barra de arriba */
export function VolverProvider({ children }: { children: ReactNode }) {
  const registrada = useRef<Accion | null>(null);
  const control = useMemo<ControlVolver>(
    () => ({
      registrar: (accion) => {
        registrada.current = accion;
      },
      quitar: (accion) => {
        if (registrada.current === accion) registrada.current = null;
      },
      actual: () => registrada.current,
    }),
    [],
  );
  return <VolverContexto.Provider value={control}>{children}</VolverContexto.Provider>;
}

export function useControlVolver() {
  return useContext(VolverContexto);
}

/** mientras la pantalla está montada "Volver" ejecuta accion (por ejemplo salir del alta a la gestión) */
export function useVolver(accion: Accion) {
  const control = useContext(VolverContexto);
  useEffect(() => {
    if (!control) return;
    control.registrar(accion);
    return () => control.quitar(accion);
  }, [control, accion]);
}
