'use client';

import { useRouter } from 'next/navigation';
import { useCallback } from 'react';

/** salir de un caso de uso: vuelve a la pantalla principal sin guardar cambios */
export function useSalir() {
  const router = useRouter();
  return useCallback(() => router.push('/inicio'), [router]);
}
