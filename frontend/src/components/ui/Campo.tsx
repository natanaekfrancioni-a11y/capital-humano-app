'use client';

import {
  useEffect,
  useId,
  useRef,
  useState,
  type ChangeEvent,
  type InputHTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
  type Ref,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from 'react';

const CONTROL =
  'w-full rounded-[4px] border border-grafito-claro bg-papel px-3 text-cuerpo text-grafito dato placeholder:text-grafito-claro/80 placeholder:tracking-normal disabled:cursor-not-allowed disabled:bg-rosa-suave disabled:text-grafito-claro aria-[invalid=true]:border-2 aria-[invalid=true]:border-error';

interface Marco {
  etiqueta: string;
  obligatorio?: boolean;
  error?: string | null;
  ayuda?: ReactNode;
  /** largo máximo para el contador, no bloquea la escritura porque se valida al aceptar */
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

/** pasa a mayúsculas sin mover el cursor, el texto se ingresa siempre en mayúsculas */
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

type ComboProps = Marco & {
  valor: string;
  alCambiar: (valor: string) => void;
  /** valores sugeridos, se puede elegir uno o escribir parte de un nombre */
  opciones: string[];
  disabled?: boolean;
  maxLength?: number;
  placeholder?: string;
  textoSinCoincidencias?: string;
  id?: string;
};

const coincide = (opcion: string, texto: string) => opcion.toLocaleUpperCase('es-AR').includes(texto);

/** campo con lista desplegable: la flecha muestra la lista completa y al escribir se filtra por "contiene" */
export function CampoCombo({
  etiqueta,
  obligatorio,
  error,
  ayuda,
  className,
  valor,
  alCambiar,
  opciones,
  disabled,
  maxLength,
  placeholder,
  textoSinCoincidencias = 'No hay coincidencias.',
  id,
}: ComboProps) {
  const autoId = useId();
  const campoId = id ?? autoId;
  const listaId = `${campoId}-lista`;
  const contenedorRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const [abierto, setAbierto] = useState(false);
  const [filtrar, setFiltrar] = useState(false);
  const [activo, setActivo] = useState(-1);

  const texto = valor.trim().toLocaleUpperCase('es-AR');
  const visibles = filtrar && texto ? opciones.filter((o) => coincide(o, texto)) : opciones;

  useEffect(() => {
    if (!abierto) return;
    const alTocarFuera = (e: PointerEvent) => {
      if (!contenedorRef.current?.contains(e.target as Node)) {
        setAbierto(false);
        setActivo(-1);
      }
    };
    document.addEventListener('pointerdown', alTocarFuera);
    return () => document.removeEventListener('pointerdown', alTocarFuera);
  }, [abierto]);

  useEffect(() => {
    if (abierto && activo >= 0) document.getElementById(`${listaId}-${activo}`)?.scrollIntoView({ block: 'nearest' });
  }, [abierto, activo, listaId]);

  /** abre la lista completa y marca el valor actual si está en la lista */
  function abrirCompleta() {
    setFiltrar(false);
    setActivo(opciones.indexOf(valor));
    setAbierto(true);
  }

  function cerrar() {
    setAbierto(false);
    setActivo(-1);
  }

  function elegir(opcion: string) {
    alCambiar(opcion);
    cerrar();
    inputRef.current?.focus();
  }

  function alTeclear(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault();
      if (!abierto) {
        abrirCompleta();
        return;
      }
      if (visibles.length === 0) return;
      const paso = e.key === 'ArrowDown' ? 1 : -1;
      setActivo((a) => (a < 0 ? (paso === 1 ? 0 : visibles.length - 1) : (a + paso + visibles.length) % visibles.length));
    } else if (e.key === 'Enter') {
      // sin una opción marcada el Enter sigue de largo y envía el formulario (Buscar)
      if (abierto && activo >= 0 && activo < visibles.length) {
        e.preventDefault();
        elegir(visibles[activo]);
      }
    } else if (e.key === 'Escape') {
      if (abierto) {
        e.preventDefault();
        e.stopPropagation();
        cerrar();
      }
    } else if (e.key === 'Tab') {
      cerrar();
    }
  }

  return (
    <MarcoCampo id={campoId} etiqueta={etiqueta} obligatorio={obligatorio} error={error} ayuda={ayuda} className={className}>
      <div ref={contenedorRef} className="relative">
        <input
          ref={inputRef}
          id={campoId}
          role="combobox"
          aria-expanded={abierto}
          aria-controls={listaId}
          aria-autocomplete="list"
          aria-activedescendant={abierto && activo >= 0 ? `${listaId}-${activo}` : undefined}
          aria-invalid={error ? true : undefined}
          aria-describedby={error || ayuda ? `${campoId}-msj` : undefined}
          aria-required={obligatorio || undefined}
          value={valor}
          onChange={(e) => {
            alCambiar(aMayusculas(e));
            setFiltrar(true);
            setActivo(-1);
            setAbierto(true);
          }}
          onKeyDown={alTeclear}
          disabled={disabled}
          maxLength={maxLength}
          placeholder={placeholder}
          autoComplete="off"
          spellCheck={false}
          className={`${CONTROL} h-10 pr-10`}
        />
        <button
          type="button"
          tabIndex={-1}
          aria-label={abierto ? 'Ocultar la lista' : 'Mostrar la lista completa'}
          disabled={disabled}
          onPointerDown={(e) => e.preventDefault()}
          onClick={() => {
            if (abierto) cerrar();
            else abrirCompleta();
            inputRef.current?.focus();
          }}
          className="absolute inset-y-0 right-0 flex w-10 items-center justify-center text-grafito disabled:cursor-not-allowed disabled:text-grafito-claro"
        >
          <svg viewBox="0 0 16 16" aria-hidden="true" className={`h-4 w-4 transition-transform ${abierto ? 'rotate-180' : ''}`}>
            <path d="M3.5 6l4.5 4.5L12.5 6" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
        {abierto && (
          <ul
            id={listaId}
            role="listbox"
            aria-label={etiqueta}
            className="absolute inset-x-0 top-[calc(100%+4px)] z-30 max-h-64 overflow-y-auto rounded-[4px] border border-grafito-claro bg-papel py-1 shadow-[0_10px_28px_rgba(37,39,45,0.16)]"
          >
            {visibles.length === 0 ? (
              <li role="option" aria-selected={false} aria-disabled="true" className="px-3 py-2 text-chica text-grafito-claro">
                {textoSinCoincidencias}
              </li>
            ) : (
              visibles.map((o, i) => (
                <li
                  key={o}
                  id={`${listaId}-${i}`}
                  role="option"
                  aria-selected={o === valor}
                  onPointerDown={(e) => e.preventDefault()}
                  onPointerMove={() => setActivo(i)}
                  onClick={() => elegir(o)}
                  className={`dato flex cursor-pointer items-center justify-between gap-3 px-3 py-2 text-cuerpo ${
                    i === activo ? 'bg-rosa-marcado' : ''
                  } ${o === valor ? 'font-bold' : ''}`}
                >
                  <span>{o}</span>
                  {o === valor && (
                    <svg viewBox="0 0 16 16" aria-hidden="true" className="h-4 w-4 shrink-0 text-tinta">
                      <path d="M3 8.5l3.2 3L13 4.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  )}
                </li>
              ))
            )}
          </ul>
        )}
      </div>
    </MarcoCampo>
  );
}
