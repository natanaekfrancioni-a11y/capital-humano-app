'use client';

import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Boton } from '@/components/ui/Boton';
import { CampoSeleccion } from '@/components/ui/Campo';
import { useAvisos } from '@/components/ui/Dialogo';
import { AvisoErrores, type ErroresFormulario } from '@/components/ui/Hoja';
import { api } from '@/lib/api';
import type { Competencia, Factor, PreguntaGenerada } from '@/lib/tipos';
import { validar } from '@/lib/validacion';

interface Props {
  competencias: Competencia[];
  /** Competencia y factor ya elegidos en el alta, para no volver a cargarlos. */
  competenciaInicial: string;
  factorInicial: string;
  alCancelar: () => void;
  alGenerar: (pregunta: PreguntaGenerada, factores: Factor[]) => void;
}

/**
 * CU033 - Generar pregunta con IA. Ventana emergente con Competencia y Factor (paso 1).
 * Al generar (pasos 2 a 4) devuelve los datos para completar el alta de pregunta (CU021).
 * Se muestra mientras está montada.
 */
export function GenerarConIA({ competencias, competenciaInicial, factorInicial, alCancelar, alGenerar }: Props) {
  const avisos = useAvisos();
  const ref = useRef<HTMLDialogElement>(null);
  const avisoRef = useRef<HTMLDivElement>(null);
  const cancelado = useRef(false);
  const [competenciaId, setCompetenciaId] = useState(competenciaInicial);
  const [factorId, setFactorId] = useState(factorInicial);
  const [factores, setFactores] = useState<Factor[]>([]);
  const [errores, setErrores] = useState<ErroresFormulario | null>(null);
  const [porCampo, setPorCampo] = useState<Record<string, string>>({});
  const [generando, setGenerando] = useState(false);

  useEffect(() => {
    const dialogo = ref.current;
    if (dialogo && !dialogo.open) dialogo.showModal();
  }, []);

  // Al seleccionar una competencia se cargan sus factores.
  useEffect(() => {
    if (!competenciaId) return;
    let vigente = true;
    api
      .buscarFactores(Number(competenciaId))
      .then((f) => vigente && setFactores([...f].sort((a, b) => a.nroOrden - b.nroOrden)))
      .catch((e) => avisos.error(e, 'No se pudieron cargar los factores'));
    return () => {
      vigente = false;
    };
  }, [competenciaId, avisos]);

  useEffect(() => {
    if (errores) avisoRef.current?.focus();
  }, [errores]);

  function cancelar() {
    cancelado.current = true;
    alCancelar();
  }

  async function generar(e: FormEvent) {
    e.preventDefault();
    // 2.A: se detallan todas las omisiones, sin tapar campos ni botones.
    const r = validar([
      { campo: 'Competencia', valor: competenciaId, obligatorio: true },
      { campo: 'Factor', valor: factorId, obligatorio: true },
    ]);
    setPorCampo(r.porCampo);
    if (r.mensajes.length) {
      setErrores({ titulo: 'No se puede generar la pregunta', items: r.mensajes });
      return;
    }
    setErrores(null);
    setGenerando(true);
    try {
      const generada = await api.generarPreguntaIA(Number(competenciaId), Number(factorId));
      if (!cancelado.current) alGenerar(generada, factores);
    } catch (err) {
      setGenerando(false);
      // 4.A: mensaje emergente "Ocurrió un error al comunicarse con el servicio de IA..."
      if (!cancelado.current) await avisos.error(err, 'No se pudo generar la pregunta');
    } finally {
      setGenerando(false);
    }
  }

  return (
    <dialog
      ref={ref}
      aria-labelledby="ia-titulo"
      onCancel={(e) => {
        e.preventDefault();
        cancelar();
      }}
      className="m-auto w-[min(94vw,34rem)] rounded-[6px] border border-linea bg-papel p-0 text-grafito shadow-[0_18px_50px_rgba(37,39,45,0.22)] backdrop:bg-grafito/45"
    >
      <form onSubmit={generar} noValidate className="px-6 pb-5 pt-5">
        <div className="regla-doble flex items-baseline justify-between gap-4 pb-2">
          <h2 id="ia-titulo" className="text-subtitulo font-extrabold">
            Generar pregunta con IA
          </h2>
          <span className="cifras text-chica font-bold text-grafito-claro">CU033</span>
        </div>
        <p className="mt-3 text-chica text-grafito-claro">
          La IA propone el nombre, el texto, la descripción, la opción de respuesta y las ponderaciones. Después se pueden
          revisar y corregir antes de aceptar el alta.
        </p>

        <div className="mt-5">
          <AvisoErrores titulo={errores?.titulo ?? null} items={errores?.items} refAviso={avisoRef} />
          <div className="grid gap-4">
            <CampoSeleccion
              etiqueta="Competencia"
              obligatorio
              valor={competenciaId}
              alCambiar={(v) => {
                setCompetenciaId(v);
                setFactorId('');
                if (!v) setFactores([]);
              }}
              opciones={competencias.map((c) => ({ valor: String(c.id), texto: c.nombre }))}
              error={porCampo['Competencia']}
              disabled={generando}
            />
            <CampoSeleccion
              etiqueta="Factor"
              obligatorio
              valor={factorId}
              alCambiar={setFactorId}
              opciones={factores.map((f) => ({ valor: String(f.id), texto: f.nombre }))}
              textoVacio={competenciaId ? 'Seleccionar…' : 'Primero elija la competencia'}
              error={porCampo['Factor']}
              disabled={generando || !competenciaId}
            />
          </div>
        </div>

        {generando && (
          <div className="mt-5" role="status">
            <p className="text-chica text-grafito">Consultando al servicio de IA. Puede tardar unos segundos.</p>
            <div className="mt-2 h-2 overflow-hidden rounded-full bg-rosa-marcado">
              <div className="barra-avance h-full w-1/3 rounded-full bg-tinta" />
            </div>
          </div>
        )}

        <div className="mt-6 flex justify-end gap-2.5 border-t border-linea pt-4">
          <Boton onClick={cancelar}>Cancelar</Boton>
          <Boton type="submit" variante="principal" ocupado={generando} textoOcupado="Generando…">
            Generar pregunta
          </Boton>
        </div>
      </form>
    </dialog>
  );
}
