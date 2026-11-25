'use client';

import { useId, type ChangeEvent, type InputHTMLAttributes, type ReactNode, type Ref, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react';

const CONTROL =
  'w-full rounded-[4px] border border-grafito-claro bg-papel px-3 text-cuerpo text-grafito dato placeholder:text-grafito-claro/80 placeholder:tracking-normal disabled:cursor-not-allowed disabled:bg-rosa-suave disabled:text-grafito-claro aria-[invalid=true]:border-2 aria-[invalid=true]:border-error';

interface Marco {
  etiqueta: string;
  obligatorio?: boolean;
  error?: string | null;
  ayuda?: ReactNode;
  /** Longitud máxima a mostrar como contador. No bloquea la escritura: se valida al aceptar (2.B). */
  maximo?: number;
  largo?: number;
  className?: string;
}

function MarcoCampo({
  id,
  etiqueta,
  obligatorio,
  error,
  ayuda,
  maximo,
  largo = 0,
  className = '',
  children,
}: Marco & { id: string; children: ReactNode }) {
  const excedido = maximo !== undefined && largo > maximo;
  return (
    <div className={`flex min-w-0 flex-col gap-1.5 ${className}`}>
      <label htmlFor={id} className="text-chica font-bold text-grafito">
        {etiqueta}
        {obligatorio && (
          <span className="text-tinta" aria-hidden="true">
            {' '}*
          </span>
        )}
        {obligatorio && <span className="sr-only"> (obligatorio)</span>}
      </label>
      {children}
      {(error || ayuda || maximo !== undefined) && (
        <div className="flex items-start justify-between gap-3 text-chica">
          <span id={`${id}-msj`} className={error ? 'font-bold text-error' : 'text-grafito-claro'}>
            {error ?? ayuda}
          </span>
          {maximo !== undefined && (
            <span className={`cifras shrink-0 ${excedido ? 'font-bold text-error' : 'text-grafito-claro'}`}>
              {largo}/{maximo}
            </span>
          )}
        </div>
      )}
    </div>
  );
}

/** Pasa a mayúsculas sin mover el cursor: el ingreso de texto es SIEMPRE en mayúsculas. */
function aMayusculas(e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>): string {
  const el = e.target;
  const mayus = el.value.toLocaleUpperCase('es-AR');
  if (mayus !== el.value) {
    const { selectionStart, selectionEnd } = el;
    el.value = mayus;
    if (selectionStart !== null && selectionEnd !== null) el.setSelectionRange(selectionStart, selectionEnd);
  }
  return mayus;
}

type TextoProps = Marco &
  Omit<InputHTMLAttributes<HTMLInputElement>, 'onChange' | 'value' | 'className'> & {
    valor: string;
    alCambiar: (valor: string) => void;
    mayusculas?: boolean;
    ref?: Ref<HTMLInputElement>;
  };

export function CampoTexto({
  etiqueta,
  obligatorio,
  error,
  ayuda,
  maximo,
  className,
  valor,
  alCambiar,
  mayusculas = true,
  id,
  ref,
  ...props
}: TextoProps) {
  const autoId = useId();
  const campoId = id ?? autoId;
  return (
    <MarcoCampo
      id={campoId}
      etiqueta={etiqueta}
      obligatorio={obligatorio}
      error={error}
      ayuda={ayuda}
      maximo={maximo}
      largo={valor.length}
      className={className}
    >
      <input
        ref={ref}
        id={campoId}
        value={valor}
        onChange={(e) => alCambiar(mayusculas ? aMayusculas(e) : e.target.value)}
        aria-invalid={error ? true : undefined}
        aria-describedby={error || ayuda || maximo !== undefined ? `${campoId}-msj` : undefined}
        aria-required={obligatorio || undefined}
        autoComplete="off"
        spellCheck={false}
        className={`${CONTROL} h-10`}
        {...props}
      />
    </MarcoCampo>
  );
}

type AreaProps = Marco &
  Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'onChange' | 'value' | 'className'> & {
    valor: string;
    alCambiar: (valor: string) => void;
    mayusculas?: boolean;
  };

export function CampoArea({
  etiqueta,
  obligatorio,
  error,
  ayuda,
  maximo,
  className,
  valor,
  alCambiar,
  mayusculas = true,
  id,
  rows = 3,
  ...props
}: AreaProps) {
  const autoId = useId();
  const campoId = id ?? autoId;
  return (
    <MarcoCampo
      id={campoId}
      etiqueta={etiqueta}
      obligatorio={obligatorio}
      error={error}
      ayuda={ayuda}
      maximo={maximo}
      largo={valor.length}
      className={className}
    >
      <textarea
        id={campoId}
        rows={rows}
        value={valor}
        onChange={(e) => alCambiar(mayusculas ? aMayusculas(e) : e.target.value)}
        aria-invalid={error ? true : undefined}
        aria-describedby={error || ayuda || maximo !== undefined ? `${campoId}-msj` : undefined}
        aria-required={obligatorio || undefined}
        spellCheck={false}
        className={`${CONTROL} resize-y py-2 leading-normal`}
        {...props}
      />
    </MarcoCampo>
  );
}

type SeleccionProps = Marco &
  Omit<SelectHTMLAttributes<HTMLSelectElement>, 'onChange' | 'value' | 'className'> & {
    valor: string;
    alCambiar: (valor: string) => void;
    opciones: { valor: string; texto: string }[];
    textoVacio?: string;
  };

export function CampoSeleccion({
  etiqueta,
  obligatorio,
  error,
  ayuda,
  className,
  valor,
  alCambiar,
  opciones,
  textoVacio = 'Seleccionar…',
  id,
  ...props
}: SeleccionProps) {
  const autoId = useId();
  const campoId = id ?? autoId;
  return (
    <MarcoCampo id={campoId} etiqueta={etiqueta} obligatorio={obligatorio} error={error} ayuda={ayuda} className={className}>
      <select
        id={campoId}
        value={valor}
        onChange={(e) => alCambiar(e.target.value)}
        aria-invalid={error ? true : undefined}
        aria-describedby={error || ayuda ? `${campoId}-msj` : undefined}
        aria-required={obligatorio || undefined}
        className={`${CONTROL} h-10 pr-8`}
        {...props}
      >
        <option value="">{textoVacio}</option>
        {opciones.map((o) => (
          <option key={o.valor} value={o.valor}>
            {o.texto}
          </option>
        ))}
      </select>
    </MarcoCampo>
  );
}
