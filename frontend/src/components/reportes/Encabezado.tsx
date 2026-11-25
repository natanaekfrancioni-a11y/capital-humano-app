import type { ReactNode } from 'react';

/** Bloque de datos del encabezado de un reporte ("Función", "Evaluación", "Candidato"). */
export function BloqueDatos({ titulo, filas }: { titulo: string; filas: [string, ReactNode][] }) {
  return (
    <section className="border-t-2 border-tinta pt-2">
      <h2 className="text-chica font-bold text-tinta">{titulo}</h2>
      <dl className="mt-1.5 grid grid-cols-[auto_1fr] gap-x-6 gap-y-1 text-cuerpo">
        {filas.map(([dt, dd]) => (
          <div key={dt} className="contents">
            <dt className="font-bold">{dt}</dt>
            <dd className="dato">{dd}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}
