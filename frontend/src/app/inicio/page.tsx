'use client';

import Link from 'next/link';
import { PantallaConsultor } from '@/components/shell/PantallaConsultor';
import { Hoja } from '@/components/ui/Hoja';

interface Opcion {
  href: string;
  titulo: string;
  detalle: string;
  cu: string;
}

const GRUPOS: { titulo: string; opciones: Opcion[] }[] = [
  {
    titulo: 'Evaluación',
    opciones: [
      { href: '/evaluar', titulo: 'Evaluar candidatos', detalle: 'Elegir candidatos y un puesto, y generar sus claves de ingreso.', cu: 'CU025' },
    ],
  },
  {
    titulo: 'Reportes',
    opciones: [
      { href: '/reportes/orden-merito', titulo: 'Orden de mérito', detalle: 'Candidatos de un puesto ordenados por puntaje.', cu: 'CU028' },
    ],
  },
  {
    titulo: 'Estructura de evaluación',
    opciones: [
      { href: '/preguntas', titulo: 'Preguntas', detalle: 'Banco de preguntas por factor, con sus ponderaciones.', cu: 'CU020' },
    ],
  },
];

export default function InicioPage() {
  return (
    <PantallaConsultor>
      {() => (
        <Hoja titulo="Menú principal">
          <nav aria-label="Funciones del consultor" className="grid gap-x-10 gap-y-9 md:grid-cols-2 lg:grid-cols-3">
            {GRUPOS.map((grupo) => (
              <section key={grupo.titulo}>
                <h2 className="border-b-2 border-tinta pb-2 text-subtitulo font-extrabold text-grafito">{grupo.titulo}</h2>
                <ul className="divide-y divide-linea">
                  {grupo.opciones.map((o) => (
                    <li key={o.href}>
                      <Link href={o.href} className="group flex items-start justify-between gap-4 px-1 py-3.5 hover:bg-rosa-suave">
                        <span>
                          <span className="block font-bold text-grafito group-hover:underline group-hover:decoration-tinta group-hover:underline-offset-4">
                            {o.titulo}
                          </span>
                          <span className="mt-0.5 block text-chica text-grafito-claro">{o.detalle}</span>
                        </span>
                        <span className="cifras mt-0.5 shrink-0 text-chica text-grafito-claro">{o.cu}</span>
                      </Link>
                    </li>
                  ))}
                </ul>
              </section>
            ))}
          </nav>
        </Hoja>
      )}
    </PantallaConsultor>
  );
}
