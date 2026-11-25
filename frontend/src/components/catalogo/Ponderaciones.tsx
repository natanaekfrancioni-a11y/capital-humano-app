'use client';

import { aNumero, puntaje } from '@/lib/formato';
import type { DetalleOpcion } from '@/lib/tipos';

interface Props {
  detalles: DetalleOpcion[];
  valores: Record<number, string>;
  alCambiar: (valores: Record<number, string>) => void;
  error?: string | null;
}

export function sumaPonderaciones(valores: Record<number, string>): number {
  return Object.values(valores).reduce((acc, v) => {
    const n = aNumero(v);
    return acc + (Number.isNaN(n) ? 0 : n);
  }, 0);
}

/**
 * Grilla de la opción de respuesta elegida: Orden y Descripción de solo lectura,
 * Ponderación editable. Las ponderaciones son 0 o positivas y suman 10 (CU021).
 */
export function Ponderaciones({ detalles, valores, alCambiar, error }: Props) {
  const suma = sumaPonderaciones(valores);
  const exacta = Math.abs(suma - 10) < 0.005;
  const ordenados = [...detalles].sort((a, b) => a.nroOrden - b.nroOrden);

  return (
    <div>
      <div className="overflow-x-auto border-y border-linea">
        <table className="w-full border-collapse text-chica">
          <caption className="sr-only">Ponderación de cada opción de respuesta</caption>
          <thead>
            <tr>
              <th scope="col" className="w-20 border-b-2 border-tinta px-3 py-2.5 text-left font-bold text-tinta">Orden</th>
              <th scope="col" className="border-b-2 border-tinta px-3 py-2.5 text-left font-bold text-tinta">Descripción</th>
              <th scope="col" className="w-40 border-b-2 border-tinta px-3 py-2.5 text-right font-bold text-tinta">Ponderación</th>
            </tr>
          </thead>
          <tbody>
            {ordenados.map((d, i) => {
              const valor = valores[d.id] ?? '';
              const n = aNumero(valor);
              const invalida = valor.trim() !== '' && (Number.isNaN(n) || n < 0);
              return (
                <tr key={d.id} className={`border-b border-linea last:border-b-0 ${i % 2 ? 'bg-rosa-suave' : 'bg-papel'}`}>
                  <td className="cifras px-3 py-2">{d.nroOrden}</td>
                  <td className="dato px-3 py-2">{d.descripcion}</td>
                  <td className="px-3 py-1.5 text-right">
                    <input
                      inputMode="decimal"
                      value={valor}
                      onChange={(e) => alCambiar({ ...valores, [d.id]: e.target.value.replace(/[^\d.,]/g, '') })}
                      aria-label={`Ponderación de ${d.descripcion}`}
                      aria-invalid={invalida || undefined}
                      className="cifras h-9 w-24 rounded-[4px] border border-grafito-claro bg-papel px-2 text-right text-cuerpo aria-[invalid=true]:border-2 aria-[invalid=true]:border-error"
                    />
                  </td>
                </tr>
              );
            })}
          </tbody>
          <tfoot>
            <tr>
              <td colSpan={2} className="px-3 py-2.5 text-right font-bold">
                Suma de ponderaciones
              </td>
              <td className={`cifras px-3 py-2.5 text-right text-cuerpo font-extrabold ${exacta ? 'text-cumple' : 'text-error'}`}>
                {puntaje(suma)} / 10
              </td>
            </tr>
          </tfoot>
        </table>
      </div>
      {error && <p className="mt-2 text-chica font-bold text-error">{error}</p>}
    </div>
  );
}
