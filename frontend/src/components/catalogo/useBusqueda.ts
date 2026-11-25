'use client';

import { useCallback, useState } from 'react';
import { useAvisos } from '@/components/ui/Dialogo';

/**
 * Búsqueda de una pantalla de gestión: guarda los últimos filtros para "actualizar la grilla"
 * después de un alta, modificación o baja (pasos 3.A.2 / 5.A.2 de los casos de uso de gestión).
 * `buscar` tiene que ser estable (definida fuera del componente).
 */
export function useBusqueda<F, R extends { id: number }>(buscar: (filtros: F) => Promise<R[]>) {
  const avisos = useAvisos();
  const [resultados, setResultados] = useState<R[] | null>(null);
  const [ultimos, setUltimos] = useState<F | null>(null);
  const [buscando, setBuscando] = useState(false);
  const [seleccion, setSeleccion] = useState<number | null>(null);

  const ejecutar = useCallback(
    async (filtros: F) => {
      setBuscando(true);
      try {
        const r = await buscar(filtros);
        setResultados(r);
        setUltimos(filtros);
        setSeleccion((actual) => (r.some((x) => x.id === actual) ? actual : null));
      } catch (e) {
        await avisos.error(e, 'No se pudo realizar la búsqueda');
      } finally {
        setBuscando(false);
      }
    },
    [avisos, buscar],
  );

  const refrescar = useCallback(async () => {
    if (ultimos !== null) await ejecutar(ultimos);
  }, [ejecutar, ultimos]);

  const seleccionado = resultados?.find((r) => r.id === seleccion) ?? null;

  return { resultados, buscando, ejecutar, refrescar, seleccion, setSeleccion, seleccionado };
}
