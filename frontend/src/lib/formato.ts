import type { EstadoCuestionario } from './tipos';

/** convierte el texto ISO de la API ("2026-11-24T21:03:57.123456") a Date en hora local */
export function aFecha(iso: string | null | undefined): Date | null {
  if (!iso) return null;
  const [fecha, hora = '00:00:00'] = iso.split('T');
  const [a, m, d] = fecha.split('-').map(Number);
  const [hh, mm, ssConFraccion = '0'] = hora.split(':');
  const ss = Number.parseFloat(ssConFraccion);
  const resultado = new Date(a, m - 1, d, Number(hh), Number(mm), Math.floor(ss), Math.round((ss % 1) * 1000));
  return Number.isNaN(resultado.getTime()) ? null : resultado;
}

const dos = (n: number) => String(n).padStart(2, '0');

/** dd/mm/aaaa */
export function fecha(iso: string | null | undefined): string {
  const f = aFecha(iso);
  return f ? `${dos(f.getDate())}/${dos(f.getMonth() + 1)}/${f.getFullYear()}` : '-';
}

/** dd/mm/aaaa hh:mm */
export function fechaHora(iso: string | null | undefined): string {
  const f = aFecha(iso);
  return f ? `${fecha(iso)} ${dos(f.getHours())}:${dos(f.getMinutes())}` : '-';
}

/** número con 2 decimales y coma decimal: 5,25 */
export function puntaje(valor: number | null | undefined): string {
  if (valor === null || valor === undefined || Number.isNaN(valor)) return '-';
  return valor.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

/** diferencia con signo: +2,50 / −0,50 */
export function diferencia(valor: number): string {
  const texto = puntaje(Math.abs(valor));
  if (valor > 0) return `+${texto}`;
  if (valor < 0) return `−${texto}`;
  return texto;
}

/** acepta coma o punto decimal, devuelve NaN si no es un número */
export function aNumero(texto: string): number {
  const limpio = texto.trim().replace(',', '.');
  if (limpio === '' || !/^-?\d+(\.\d+)?$/.test(limpio)) return Number.NaN;
  return Number(limpio);
}

const ESTADOS: Record<EstadoCuestionario, string> = {
  ACTIVO: 'Activo',
  EN_PROCESO: 'En proceso',
  COMPLETO: 'Completo',
  INCOMPLETO: 'Incompleto',
  SIN_CONTESTAR: 'Sin contestar',
};

export function estado(e: EstadoCuestionario | string): string {
  return ESTADOS[e as EstadoCuestionario] ?? e;
}

/** "Apellido, Nombre" */
export function nombreCompleto(apellido: string, nombre: string): string {
  return `${apellido}, ${nombre}`;
}

/** lista legible: "A", "A y B", "A, B y C" */
export function enumerar(items: string[]): string {
  if (items.length <= 1) return items.join('');
  return `${items.slice(0, -1).join(', ')} y ${items[items.length - 1]}`;
}

/** mm:ss o h:mm:ss para el cronómetro del cuestionario */
export function reloj(segundos: number): string {
  const s = Math.max(0, Math.floor(segundos));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const r = s % 60;
  return h > 0 ? `${h}:${dos(m)}:${dos(r)}` : `${dos(m)}:${dos(r)}`;
}
