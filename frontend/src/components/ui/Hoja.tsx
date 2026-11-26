import type { ReactNode, Ref } from 'react';

interface HojaProps {
  titulo: string;
  /** Caso de uso que resuelve la pantalla (trazabilidad con la especificación). */
  cu?: string;
  bajada?: ReactNode;
  children: ReactNode;
  className?: string;
  ancho?: 'normal' | 'angosto';
}

/** La hoja de trabajo de cada pantalla: papel blanco con el encabezado de formulario. */
export function Hoja({ titulo, cu, bajada, children, className = '', ancho = 'normal' }: HojaProps) {
  return (
    <section
      className={`hoja-impresion mx-auto w-full rounded-[6px] border border-linea bg-papel px-5 py-7 sm:px-10 sm:py-9 ${
        ancho === 'angosto' ? 'max-w-3xl' : 'max-w-[72rem]'
      } ${className}`}
    >
      <header className="regla-doble mb-7 pb-3">
        <div className="flex flex-wrap items-baseline justify-between gap-x-6 gap-y-1">
          <h1 className="text-titulo font-extrabold tracking-[-0.01em] text-grafito">{titulo}</h1>
          {cu && <span className="cifras text-chica font-bold text-grafito-claro">{cu}</span>}
        </div>
        {bajada && <p className="mt-2 max-w-[62ch] text-cuerpo text-grafito-claro">{bajada}</p>}
      </header>
      {children}
    </section>
  );
}

/** Botones al pie de la hoja: secundarios a la izquierda y el principal a la derecha. */
export function BarraAcciones({ izquierda, derecha }: { izquierda?: ReactNode; derecha?: ReactNode }) {
  return (
    <div className="no-imprimir mt-7 flex flex-wrap items-center justify-between gap-3 border-t border-linea pt-5">
      <div className="flex flex-wrap gap-2.5">{izquierda}</div>
      <div className="flex flex-wrap gap-2.5">{derecha}</div>
    </div>
  );
}

/**
 * Subtítulo de sección dentro de una hoja.
 * min-w-0: dentro de una grilla, la sección puede achicarse y la tabla se desplaza dentro de su marco
 * en lugar de ensanchar la página en el celular.
 */
export function Seccion({ titulo, children, accion }: { titulo: string; children: ReactNode; accion?: ReactNode }) {
  return (
    <section className="min-w-0">
      <div className="mb-3 flex flex-wrap items-end justify-between gap-3">
        <h2 className="text-subtitulo font-extrabold text-grafito">{titulo}</h2>
        {accion}
      </div>
      {children}
    </section>
  );
}

/**
 * Errores de validación del formulario: van en el flujo de la página, arriba del formulario,
 * "sin tapar campos ni botones" (flujo alternativo 2.A de las altas).
 */
export function AvisoErrores({
  titulo,
  items = [],
  refAviso,
}: {
  titulo: string | null;
  items?: string[];
  refAviso?: Ref<HTMLDivElement>;
}) {
  if (!titulo) return null;
  return (
    <div
      ref={refAviso}
      tabIndex={-1}
      role="alert"
      className="mb-6 border-l-4 border-error bg-[#fdf1f0] px-4 py-3 text-cuerpo text-grafito outline-none"
    >
      <p className="font-bold text-error">{titulo}</p>
      {items.length > 0 && (
        <ul className="mt-1.5 list-disc space-y-0.5 pl-5">
          {items.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      )}
    </div>
  );
}

/** Estado del aviso de errores de un formulario, con foco al aviso para lectores de pantalla. */
export interface ErroresFormulario {
  titulo: string;
  items: string[];
}
