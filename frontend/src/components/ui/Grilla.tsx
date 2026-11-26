'use client';

import type { ReactNode } from 'react';

export interface Columna<T> {
  titulo: string;
  celda: (fila: T) => ReactNode;
  alinear?: 'izquierda' | 'derecha' | 'centro';
  /** Datos guardados en mayúsculas (nombres, empresas): leve espaciado entre letras. */
  dato?: boolean;
  /** Números (documentos, puntajes): cifras de ancho fijo. */
  cifras?: boolean;
  ancho?: string;
}

type Id = number | string;

interface Seleccion<T> {
  modo: 'unica' | 'multiple';
  /** nombre del grupo de radios / checkboxes */
  nombre: string;
  seleccionadas: Id[];
  alCambiar: (ids: Id[]) => void;
  /** Texto accesible de cada círculo: "Seleccionar PEREZ, LUCAS" */
  etiquetaFila: (fila: T) => string;
}

interface GrillaProps<T> {
  /** Describe la grilla para lectores de pantalla. */
  etiqueta: string;
  columnas: Columna<T>[];
  filas: T[];
  clave: (fila: T) => Id;
  seleccion?: Seleccion<T>;
  vacio?: ReactNode;
  /** Alto máximo con encabezado fijo, ej. "22rem". */
  altoMaximo?: string;
  pie?: ReactNode;
}

const ALINEAR = { izquierda: 'text-left', derecha: 'text-right', centro: 'text-center' } as const;

/**
 * Grilla de resultados con el círculo de selección de la hoja de respuestas a la derecha,
 * como en los wireframes. El círculo es un radio/checkbox real: se usa con teclado.
 */
export function Grilla<T>({ etiqueta, columnas, filas, clave, seleccion, vacio, altoMaximo, pie }: GrillaProps<T>) {
  const seleccionadas = new Set(seleccion?.seleccionadas ?? []);

  function alternar(id: Id) {
    if (!seleccion) return;
    if (seleccion.modo === 'unica') {
      seleccion.alCambiar([id]);
    } else {
      seleccion.alCambiar(seleccionadas.has(id) ? [...seleccionadas].filter((x) => x !== id) : [...seleccionadas, id]);
    }
  }

  return (
    <div className="min-w-0">
      <div
        className="relative overflow-x-auto border-y border-linea"
        style={altoMaximo ? { maxHeight: altoMaximo, overflowY: 'auto' } : undefined}
      >
        <table className="w-full border-collapse text-chica">
          <caption className="sr-only">{etiqueta}</caption>
          <thead className={altoMaximo ? 'sticky top-0 z-[1] bg-papel' : undefined}>
            <tr>
              {columnas.map((c) => (
                <th
                  key={c.titulo}
                  scope="col"
                  style={c.ancho ? { width: c.ancho } : undefined}
                  className={`border-b-2 border-tinta px-3 py-2.5 font-bold whitespace-nowrap text-tinta ${ALINEAR[c.alinear ?? 'izquierda']}`}
                >
                  {c.titulo}
                </th>
              ))}
              {seleccion && (
                <th scope="col" className="w-12 border-b-2 border-tinta px-3 py-2.5">
                  <span className="sr-only">Selección</span>
                </th>
              )}
            </tr>
          </thead>
          <tbody>
            {filas.length === 0 && (
              <tr>
                <td colSpan={columnas.length + (seleccion ? 1 : 0)} className="px-3 py-8 text-center text-cuerpo text-grafito-claro">
                  {vacio ?? 'No hay resultados.'}
                </td>
              </tr>
            )}
            {filas.map((fila, i) => {
              const id = clave(fila);
              const marcada = seleccionadas.has(id);
              return (
                <tr
                  key={id}
                  onClick={seleccion ? (e) => {
                    if ((e.target as HTMLElement).closest('input, button, a, select, textarea')) return;
                    alternar(id);
                  } : undefined}
                  className={`border-b border-linea last:border-b-0 ${
                    marcada ? 'bg-rosa-marcado' : i % 2 === 1 ? 'bg-rosa-suave' : 'bg-papel'
                  } ${seleccion ? 'cursor-pointer' : ''}`}
                >
                  {columnas.map((c) => (
                    <td
                      key={c.titulo}
                      className={`px-3 py-2.5 align-middle ${ALINEAR[c.alinear ?? 'izquierda']} ${c.dato ? 'dato' : ''} ${c.cifras ? 'cifras' : ''}`}
                    >
                      {c.celda(fila)}
                    </td>
                  ))}
                  {seleccion && (
                    <td className="px-3 py-2.5 text-center align-middle">
                      <input
                        type={seleccion.modo === 'unica' ? 'radio' : 'checkbox'}
                        name={seleccion.nombre}
                        className="burbuja"
                        checked={marcada}
                        onChange={() => alternar(id)}
                        aria-label={seleccion.etiquetaFila(fila)}
                      />
                    </td>
                  )}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      {pie && <div className="pt-2 text-chica text-grafito-claro">{pie}</div>}
    </div>
  );
}
