'use client';

import { useMemo, useSyncExternalStore } from 'react';
import type { SesionCandidato, SesionConsultor } from './tipos';

// La sesión vive en sessionStorage: se pierde al cerrar la pestaña.
const CLAVE_CONSULTOR = 'capital-humano.consultor';
const CLAVE_CANDIDATO = 'capital-humano.candidato';
const EVENTO = 'capital-humano:sesion';

function leer(clave: string): string | null {
  try {
    return window.sessionStorage.getItem(clave);
  } catch {
    return null;
  }
}

function escribir(clave: string, valor: string | null) {
  try {
    if (valor === null) window.sessionStorage.removeItem(clave);
    else window.sessionStorage.setItem(clave, valor);
  } catch {
    // Almacenamiento no disponible (modo privado estricto): la sesión dura lo que dure la página.
  }
  window.dispatchEvent(new Event(EVENTO));
}

function suscribir(avisar: () => void) {
  window.addEventListener(EVENTO, avisar);
  window.addEventListener('storage', avisar);
  return () => {
    window.removeEventListener(EVENTO, avisar);
    window.removeEventListener('storage', avisar);
  };
}

function parsear<T>(texto: string | null | undefined): T | null | undefined {
  if (texto === undefined) return undefined;
  if (texto === null) return null;
  try {
    return JSON.parse(texto) as T;
  } catch {
    return null;
  }
}

/** undefined mientras no se sabe (render del servidor), null si no hay sesión. */
function useSesion<T>(clave: string): T | null | undefined {
  const texto = useSyncExternalStore(
    suscribir,
    () => leer(clave),
    () => undefined,
  );
  return useMemo(() => parsear<T>(texto), [texto]);
}

export const useSesionConsultor = () => useSesion<SesionConsultor>(CLAVE_CONSULTOR);
export const useSesionCandidato = () => useSesion<SesionCandidato>(CLAVE_CANDIDATO);

export function guardarSesionConsultor(sesion: SesionConsultor) {
  escribir(CLAVE_CANDIDATO, null);
  escribir(CLAVE_CONSULTOR, JSON.stringify(sesion));
}

export function guardarSesionCandidato(sesion: SesionCandidato) {
  escribir(CLAVE_CONSULTOR, null);
  escribir(CLAVE_CANDIDATO, JSON.stringify(sesion));
}

export function cerrarSesion() {
  escribir(CLAVE_CONSULTOR, null);
  escribir(CLAVE_CANDIDATO, null);
}
