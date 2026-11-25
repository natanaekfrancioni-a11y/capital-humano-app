import type { ButtonHTMLAttributes } from 'react';

type Variante = 'principal' | 'secundario' | 'peligro' | 'sutil';

const VARIANTES: Record<Variante, string> = {
  principal: 'bg-grafito text-papel hover:bg-[#3a3d45] border-[1.5px] border-grafito',
  secundario: 'bg-papel text-grafito border-[1.5px] border-grafito hover:bg-rosa-suave',
  peligro: 'bg-error text-papel border-[1.5px] border-error hover:bg-[#981b12]',
  sutil: 'bg-transparent text-grafito border-[1.5px] border-transparent hover:bg-rosa-suave',
};

interface BotonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: Variante;
  /** Muestra el texto de espera y deshabilita el botón mientras dura la operación. */
  ocupado?: boolean;
  textoOcupado?: string;
}

/**
 * Botón nativo: el clic y la tecla Enter tienen el mismo efecto (requerimiento especial).
 */
export function Boton({
  variante = 'secundario',
  ocupado = false,
  textoOcupado,
  className = '',
  children,
  disabled,
  type = 'button',
  ...props
}: BotonProps) {
  return (
    <button
      type={type}
      disabled={disabled || ocupado}
      aria-busy={ocupado || undefined}
      className={`inline-flex h-10 select-none items-center justify-center gap-2 whitespace-nowrap rounded-[4px] px-[18px] text-cuerpo font-bold transition-colors disabled:cursor-not-allowed disabled:opacity-45 ${VARIANTES[variante]} ${className}`}
      {...props}
    >
      {ocupado && textoOcupado ? textoOcupado : children}
    </button>
  );
}
