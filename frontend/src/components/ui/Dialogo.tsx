'use client';

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { ApiError } from '@/lib/api';
import { Boton } from './Boton';
import { IconoConfirmacion, IconoError, IconoInformativa, IconoValidacion } from './Iconos';

/** los cuatro mensajes emergentes del diseño de entradas y salidas (etapa 1) */
export type TipoDialogo = 'validacion' | 'error' | 'informativa' | 'confirmacion';

export interface OpcionesDialogo {
  tipo: TipoDialogo;
  titulo: string;
  mensaje?: ReactNode;
  detalles?: string[];
  /** texto del botón principal (por defecto "Aceptar") */
  aceptar?: string;
  /** si se indica se muestra un segundo botón que devuelve false */
  rechazar?: string;
  peligro?: boolean;
  /** "Presione cualquier tecla para continuar": cualquier tecla cierra el mensaje */
  cualquierTecla?: boolean;
}

const TIPOS: Record<TipoDialogo, { etiqueta: string; color: string; Icono: typeof IconoError }> = {
  validacion: { etiqueta: 'Validación de datos', color: 'var(--color-tinta)', Icono: IconoValidacion },
  error: { etiqueta: 'Mensaje de error', color: 'var(--color-error)', Icono: IconoError },
  informativa: { etiqueta: 'Informativa', color: 'var(--color-cumple)', Icono: IconoInformativa },
  confirmacion: { etiqueta: 'Confirmación', color: 'var(--color-grafito)', Icono: IconoConfirmacion },
};

type Abrir = (opciones: OpcionesDialogo) => Promise<boolean>;
const DialogoContexto = createContext<Abrir | null>(null);

interface Pendiente extends OpcionesDialogo {
  id: number;
  resolver: (valor: boolean) => void;
}

export function DialogoProvider({ children }: { children: ReactNode }) {
  const [cola, setCola] = useState<Pendiente[]>([]);
  const colaRef = useRef<Pendiente[]>([]);
  const contador = useRef(0);
  const ref = useRef<HTMLDialogElement>(null);
  const actual = cola[0] ?? null;

  const abrir = useCallback<Abrir>(
    (opciones) =>
      new Promise<boolean>((resolver) => {
        contador.current += 1;
        colaRef.current = [...colaRef.current, { ...opciones, id: contador.current, resolver }];
        setCola(colaRef.current);
      }),
    [],
  );

  const cerrar = useCallback((id: number, valor: boolean) => {
    const primero = colaRef.current[0];
    if (!primero || primero.id !== id) return;
    colaRef.current = colaRef.current.slice(1);
    setCola(colaRef.current);
    primero.resolver(valor);
  }, []);

  useEffect(() => {
    const dialogo = ref.current;
    if (!dialogo) return;
    if (actual && !dialogo.open) dialogo.showModal();
    if (!actual && dialogo.open) dialogo.close();
  }, [actual]);

  const tipo = actual ? TIPOS[actual.tipo] : null;
  const valorEscape = actual ? !actual.rechazar : false;

  return (
    <DialogoContexto.Provider value={abrir}>
      {children}
      <dialog
        ref={ref}
        aria-labelledby="dialogo-titulo"
        aria-describedby="dialogo-mensaje"
        onCancel={(e) => {
          e.preventDefault();
          if (actual) cerrar(actual.id, valorEscape);
        }}
        onKeyDown={(e) => {
          if (actual?.cualquierTecla && !['Tab', 'Shift'].includes(e.key)) {
            e.preventDefault();
            cerrar(actual.id, true);
          }
        }}
        className="m-auto w-[min(92vw,32rem)] rounded-[6px] border border-linea bg-papel p-0 text-grafito shadow-[0_18px_50px_rgba(37,39,45,0.22)] backdrop:bg-grafito/45"
      >
        {actual && tipo && (
          <div>
            <p className="border-b border-linea px-6 py-2 text-chica font-bold" style={{ color: tipo.color }}>
              {tipo.etiqueta}
            </p>
            <div className="flex gap-4 px-6 pt-5">
              <tipo.Icono className="mt-0.5 h-8 w-8 shrink-0" style={{ color: tipo.color }} />
              <div className="min-w-0">
                <h2 id="dialogo-titulo" className="text-subtitulo font-extrabold text-grafito">
                  {actual.titulo}
                </h2>
                <div id="dialogo-mensaje" className="mt-2 text-cuerpo">
                  {actual.mensaje}
                  {actual.detalles && actual.detalles.length > 0 && (
                    <ul className="dato mt-2 list-disc space-y-1 pl-5">
                      {actual.detalles.map((d) => (
                        <li key={d}>{d}</li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>
            </div>
            <div className="flex flex-wrap justify-end gap-2.5 px-6 pb-5 pt-6">
              {actual.rechazar && (
                <Boton variante="secundario" onClick={() => cerrar(actual.id, false)}>
                  {actual.rechazar}
                </Boton>
              )}
              <Boton
                variante={actual.peligro ? 'peligro' : 'principal'}
                autoFocus
                onClick={() => cerrar(actual.id, true)}
              >
                {actual.aceptar ?? 'Aceptar'}
              </Boton>
            </div>
            {actual.cualquierTecla && (
              <p className="border-t border-linea px-6 py-2.5 text-chica text-grafito-claro">
                Presione cualquier tecla para continuar.
              </p>
            )}
          </div>
        )}
      </dialog>
    </DialogoContexto.Provider>
  );
}

/** atajos para los cuatro mensajes emergentes, todos devuelven una promesa */
export function useAvisos() {
  const abrir = useContext(DialogoContexto);
  if (!abrir) throw new Error('useAvisos debe usarse dentro de DialogoProvider');

  return useMemo(
    () => ({
      abrir,
      informar: (titulo: string, mensaje?: ReactNode, extra?: Partial<OpcionesDialogo>) =>
        abrir({ tipo: 'informativa', titulo, mensaje, ...extra }),
      validacion: (titulo: string, mensaje?: ReactNode, detalles?: string[]) =>
        abrir({ tipo: 'validacion', titulo, mensaje, detalles }),
      confirmar: (
        titulo: string,
        mensaje?: ReactNode,
        extra: { aceptar?: string; rechazar?: string; peligro?: boolean } = {},
      ) =>
        abrir({
          tipo: 'confirmacion',
          titulo,
          mensaje,
          aceptar: extra.aceptar ?? 'Aceptar',
          rechazar: extra.rechazar ?? 'Cancelar',
          peligro: extra.peligro,
        }),
      /** muestra el error de la API (mensaje y detalles), con reintentar devuelve true si eligió Reintentar */
      error: (e: unknown, titulo = 'No se pudo completar la operación', reintentar = false) =>
        abrir({
          tipo: 'error',
          titulo,
          mensaje: e instanceof Error ? e.message : String(e),
          detalles: e instanceof ApiError ? e.detalles : undefined,
          aceptar: reintentar ? 'Reintentar' : 'Aceptar',
          rechazar: reintentar ? 'Cancelar' : undefined,
        }),
    }),
    [abrir],
  );
}
