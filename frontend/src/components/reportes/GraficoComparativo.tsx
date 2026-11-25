import { puntaje } from '@/lib/formato';
import type { ItemComparativo } from '@/lib/tipos';

const AZUL = '#2f67ae'; // Requerido
const VERDE = '#1f6b4a'; // Obtenido

/** Parte un nombre en renglones de hasta `max` caracteres para el eje X. */
function renglones(texto: string, max = 16): string[] {
  const palabras = texto.split(/\s+/);
  const lineas: string[] = [];
  let actual = '';
  for (const p of palabras) {
    if ((actual + ' ' + p).trim().length > max && actual) {
      lineas.push(actual);
      actual = p;
    } else {
      actual = (actual + ' ' + p).trim();
    }
  }
  if (actual) lineas.push(actual);
  return lineas.slice(0, 3);
}

/**
 * CU027: gráfico de barras agrupadas. Eje X: competencias del puesto; eje Y: escala de 0 a 10.
 * Series: Requerido (azul) y Obtenido (verde), como pide la tabla de especificaciones.
 */
export function GraficoComparativo({ items }: { items: ItemComparativo[] }) {
  const ancho = 680;
  const alto = 320;
  const m = { izq: 44, der: 12, arr: 22, aba: 62 };
  const areaAncho = ancho - m.izq - m.der;
  const areaAlto = alto - m.arr - m.aba;
  const grupo = areaAncho / Math.max(items.length, 1);
  const barra = Math.min(46, grupo * 0.28);
  const y = (v: number) => m.arr + areaAlto - (Math.max(0, Math.min(10, v)) / 10) * areaAlto;

  const resumen = items
    .map((i) => `${i.competencia}: requerido ${puntaje(i.requerido)}, obtenido ${puntaje(i.obtenido)}`)
    .join('; ');

  return (
    <figure className="m-0">
      <figcaption className="mb-3 flex flex-wrap items-center gap-x-6 gap-y-1 text-chica">
        <span className="flex items-center gap-2">
          <span className="inline-block h-3 w-3 rounded-[2px]" style={{ background: AZUL }} aria-hidden="true" />
          Requerido por el puesto
        </span>
        <span className="flex items-center gap-2">
          <span className="inline-block h-3 w-3 rounded-[2px]" style={{ background: VERDE }} aria-hidden="true" />
          Obtenido por el candidato
        </span>
      </figcaption>
      <svg viewBox={`0 0 ${ancho} ${alto}`} role="img" aria-label={`Gráfico comparativo. ${resumen}`} className="h-auto w-full max-w-[46rem]">
        {[0, 2, 4, 6, 8, 10].map((v) => (
          <g key={v}>
            <line x1={m.izq} x2={ancho - m.der} y1={y(v)} y2={y(v)} stroke={v === 0 ? '#25272d' : '#e5bcc7'} strokeWidth={v === 0 ? 1.5 : 1} />
            <text x={m.izq - 8} y={y(v) + 4} textAnchor="end" fontSize="12" fill="#5b5e67" style={{ fontVariantNumeric: 'tabular-nums' }}>
              {v}
            </text>
          </g>
        ))}
        {items.map((it, i) => {
          const centro = m.izq + grupo * i + grupo / 2;
          const xReq = centro - barra - 3;
          const xObt = centro + 3;
          return (
            <g key={it.competencia}>
              <rect x={xReq} y={y(it.requerido)} width={barra} height={y(0) - y(it.requerido)} fill={AZUL} />
              <rect x={xObt} y={y(it.obtenido)} width={barra} height={y(0) - y(it.obtenido)} fill={VERDE} />
              <text x={xReq + barra / 2} y={y(it.requerido) - 6} textAnchor="middle" fontSize="12" fontWeight="700" fill={AZUL}>
                {puntaje(it.requerido)}
              </text>
              <text x={xObt + barra / 2} y={y(it.obtenido) - 6} textAnchor="middle" fontSize="12" fontWeight="700" fill={VERDE}>
                {puntaje(it.obtenido)}
              </text>
              {renglones(it.competencia).map((linea, j) => (
                <text key={j} x={centro} y={y(0) + 20 + j * 15} textAnchor="middle" fontSize="12" fill="#25272d" letterSpacing="0.02em">
                  {linea}
                </text>
              ))}
            </g>
          );
        })}
      </svg>
    </figure>
  );
}
