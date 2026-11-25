'use client';

import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Boton } from '@/components/ui/Boton';
import { useAvisos } from '@/components/ui/Dialogo';
import { api } from '@/lib/api';
import { aFecha, estado as textoEstado, fechaHora, reloj } from '@/lib/formato';
import { cerrarSesion, useSesionCandidato } from '@/lib/sesion';
import type { Configuracion, Cuestionario, PreguntaCuestionario, SesionCandidato } from '@/lib/tipos';

type Etapa = 'cargando' | 'instrucciones' | 'bloque' | 'completo';

/** CU026 - Completar cuestionario (el candidato entra por el CU001). */
export default function CuestionarioPage() {
  const router = useRouter();
  const sesion = useSesionCandidato();

  useEffect(() => {
    if (sesion === null) router.replace('/');
  }, [sesion, router]);

  if (!sesion) {
    return (
      <p className="p-8 text-grafito-claro" role="status">
        Verificando el acceso…
      </p>
    );
  }
  return <CompletarCuestionario sesion={sesion} />;
}

function CompletarCuestionario({ sesion }: { sesion: SesionCandidato }) {
  const router = useRouter();
  const avisos = useAvisos();
  const [etapa, setEtapa] = useState<Etapa>('cargando');
  const [config, setConfig] = useState<Configuracion | null>(null);
  const [cuestionario, setCuestionario] = useState<Cuestionario | null>(null);
  const [selecciones, setSelecciones] = useState<Record<number, number>>({});
  const [faltantes, setFaltantes] = useState<number[]>([]);
  const [enviando, setEnviando] = useState(false);
  const [ahora, setAhora] = useState(() => Date.now());
  const tituloRef = useRef<HTMLHeadingElement>(null);
  const tiempoAgotado = useRef(false);

  const terminar = useCallback(() => {
    cerrarSesion();
    router.replace('/');
  }, [router]);

  const iniciar = useCallback(async () => {
    try {
      const q = await api.iniciarCuestionario(sesion.cuestionarioId);
      setCuestionario(q);
      setSelecciones({});
      setFaltantes([]);
      setEtapa(q.estado === 'COMPLETO' ? 'completo' : 'bloque');
    } catch (e) {
      // 1.A / 1.B / accesos superados: el sistema avisa la situación y el CU finaliza.
      await avisos.error(e, 'No se puede completar el cuestionario');
      terminar();
    }
  }, [sesion.cuestionarioId, avisos, terminar]);

  // Paso 1: instrucciones si el cuestionario está activo; si ya estaba en proceso, se retoma (1.C).
  useEffect(() => {
    let vigente = true;
    api
      .configuracion()
      .then((c) => {
        if (!vigente) return;
        setConfig(c);
        if (sesion.estado === 'ACTIVO') setEtapa('instrucciones');
        else iniciar();
      })
      .catch(async (e) => {
        await avisos.error(e, 'No se han podido recuperar las preguntas. Por favor, reintente en unos minutos.');
        terminar();
      });
    return () => {
      vigente = false;
    };
  }, [sesion.estado, iniciar, avisos, terminar]);

  // Cronómetro: cuenta regresiva desde el inicio del cuestionario.
  useEffect(() => {
    if (etapa !== 'bloque') return;
    const id = window.setInterval(() => setAhora(Date.now()), 1000);
    return () => window.clearInterval(id);
  }, [etapa]);

  const fin = useMemo(() => {
    const inicio = aFecha(cuestionario?.fechaInicio);
    return inicio && config ? inicio.getTime() + config.tiempoMaxMinutos * 60_000 : null;
  }, [cuestionario?.fechaInicio, config]);
  const restante = fin === null ? null : Math.max(0, Math.round((fin - ahora) / 1000));

  useEffect(() => {
    if (etapa !== 'bloque' || restante !== 0 || tiempoAgotado.current) return;
    tiempoAgotado.current = true;
    avisos
      .abrir({
        tipo: 'informativa',
        titulo: 'Tiempo agotado',
        mensaje: 'El tiempo para completar el cuestionario ha expirado. Las respuestas de los bloques ya enviados quedaron guardadas.',
      })
      .then(terminar);
  }, [etapa, restante, avisos, terminar]);

  const preguntas = useMemo(() => [...(cuestionario?.preguntas ?? [])].sort((a, b) => a.ordenVisual - b.ordenVisual), [cuestionario]);
  const totalBloques = preguntas.reduce((max, p) => Math.max(max, p.nroBloque), 0);
  const bloque = cuestionario?.bloqueActual ?? 1;
  const delBloque = preguntas.filter((p) => p.nroBloque === bloque);
  const esUltimo = bloque >= totalBloques;

  useEffect(() => {
    if (etapa === 'bloque' || etapa === 'completo') {
      window.scrollTo({ top: 0 });
      tituloRef.current?.focus();
    }
  }, [etapa, bloque]);

  // Pasos 5 a 8: se valida el bloque, se guarda y se pasa al siguiente (o se finaliza).
  async function enviarBloque() {
    if (!cuestionario) return;
    const sinResponder = delBloque.filter((p) => selecciones[p.id] === undefined).map((p) => p.id);
    if (sinResponder.length) {
      setFaltantes(sinResponder);
      document.getElementById(`pregunta-${sinResponder[0]}`)?.focus();
      return;
    }
    if (esUltimo) {
      const ok = await avisos.confirmar(
        'Finalizar el cuestionario',
        '¿Está seguro que desea finalizar la evaluación? Una vez enviada no podrá modificar sus respuestas.',
        { aceptar: 'Finalizar', rechazar: 'Seguir revisando' },
      );
      if (!ok) return;
    }
    setEnviando(true);
    try {
      const q = await api.responderBloque(
        cuestionario.id,
        bloque,
        delBloque.map((p) => ({ preguntaCuestionarioId: p.id, detalleOpcionRespuestaId: selecciones[p.id] })),
      );
      setCuestionario(q);
      setFaltantes([]);
      if (q.estado === 'COMPLETO') setEtapa('completo');
    } catch (e) {
      await avisos.error(e, 'No se pudo guardar el bloque');
      if (e instanceof Error && /tiempo máximo|accesos/.test(e.message)) terminar();
    } finally {
      setEnviando(false);
    }
  }

  async function salir() {
    if (etapa === 'bloque') {
      const ok = await avisos.confirmar(
        'Salir del cuestionario',
        'Las respuestas de los bloques ya enviados quedan guardadas. Puede volver a ingresar con su clave mientras el cuestionario siga vigente; cada ingreso cuenta como un acceso.',
        { aceptar: 'Salir', rechazar: 'Seguir respondiendo' },
      );
      if (!ok) return;
    }
    terminar();
  }

  const respondidasAntes = preguntas.filter((p) => p.nroBloque < bloque).length;
  const respondidasAhora = respondidasAntes + delBloque.filter((p) => selecciones[p.id] !== undefined).length;

  return (
    <div className="min-h-dvh">
      <header className="sticky top-0 z-20 border-b border-linea bg-papel">
        <div className="mx-auto flex max-w-3xl items-center justify-between gap-4 px-4 py-3">
          <div className="min-w-0">
            <p className="truncate text-chica font-extrabold text-grafito">
              <span className="hidden sm:inline">Consultora </span>Capit@l Humano
            </p>
            <p className="dato truncate text-chica text-grafito-claro">{sesion.candidatoNombre}</p>
          </div>
          <div className="flex items-center gap-3">
            {etapa === 'bloque' && restante !== null && (
              <p
                className={`cifras text-right text-chica ${restante <= 300 ? 'font-extrabold text-error' : 'text-grafito'}`}
                aria-live={restante <= 300 && restante % 60 === 0 ? 'polite' : 'off'}
              >
                <span className="block whitespace-nowrap text-grafito-claro">Tiempo restante</span>
                <span className="text-subtitulo font-extrabold">{reloj(restante)}</span>
              </p>
            )}
            <button
              type="button"
              onClick={salir}
              className="h-9 rounded-[4px] border-[1.5px] border-grafito px-3.5 text-chica font-bold text-grafito hover:bg-rosa-suave"
            >
              Salir
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-4 py-6 sm:py-10">
        {etapa === 'cargando' && (
          <p className="text-grafito-claro" role="status">
            Preparando el cuestionario…
          </p>
        )}

        {etapa === 'instrucciones' && config && (
          <section className="rounded-[6px] border border-linea bg-papel px-5 py-7 sm:px-9">
            <h1 className="regla-doble pb-3 text-titulo font-extrabold">Cuestionario: instrucciones</h1>
            <p className="dato mt-6 max-w-[62ch] text-cuerpo leading-relaxed">{config.instrucciones}</p>
            <ul className="mt-6 divide-y divide-linea border-y border-linea text-cuerpo">
              <li className="flex justify-between gap-4 py-3">
                <span className="font-bold">Tiempo para completarlo</span>
                <span className="cifras">{config.tiempoMaxMinutos} minutos desde que lo comienza</span>
              </li>
              <li className="flex justify-between gap-4 py-3">
                <span className="font-bold">Preguntas por bloque</span>
                <span className="cifras">{config.preguntasPorBloque}</span>
              </li>
              <li className="flex justify-between gap-4 py-3">
                <span className="font-bold">Ingresos permitidos</span>
                <span className="cifras">{config.maxAccesos}</span>
              </li>
              <li className="flex justify-between gap-4 py-3">
                <span className="font-bold">Estado del cuestionario</span>
                <span>{textoEstado(sesion.estado)}</span>
              </li>
            </ul>
            <p className="mt-5 text-chica text-grafito-claro">
              Cada pregunta tiene una única respuesta. Al pasar al siguiente bloque, las respuestas se guardan y ya no se pueden
              cambiar. El tiempo empieza a correr al presionar Aceptar.
            </p>
            <div className="mt-7 flex flex-wrap justify-between gap-3 border-t border-linea pt-5">
              <Boton onClick={terminar}>Cancelar</Boton>
              <Boton
                variante="principal"
                onClick={() => {
                  setEtapa('cargando');
                  iniciar();
                }}
              >
                Aceptar y comenzar
              </Boton>
            </div>
          </section>
        )}

        {etapa === 'bloque' && cuestionario && (
          <section aria-labelledby="titulo-bloque">
            <div className="mb-6">
              <h1 id="titulo-bloque" ref={tituloRef} tabIndex={-1} className="text-titulo font-extrabold outline-none">
                Bloque {bloque} de {totalBloques}
              </h1>
              <p className="dato mt-1 text-chica text-grafito-claro">
                {cuestionario.puesto.nombre} ({cuestionario.puesto.empresa})
              </p>
              <Progreso total={totalBloques} actual={bloque} respondidas={respondidasAhora} preguntas={preguntas.length} />
            </div>

            {faltantes.length > 0 && (
              <p role="alert" className="mb-5 border-l-4 border-error bg-[#fdf1f0] px-4 py-3 font-bold text-error">
                Debe seleccionar una opción para continuar. Falta responder{' '}
                {faltantes.length === 1 ? 'la pregunta' : 'las preguntas'}{' '}
                {faltantes.map((id) => preguntas.find((p) => p.id === id)?.ordenVisual).join(', ')}.
              </p>
            )}

            <ol className="grid gap-5">
              {delBloque.map((pq) => (
                <PreguntaBloque
                  key={pq.id}
                  pq={pq}
                  total={preguntas.length}
                  seleccion={selecciones[pq.id]}
                  falta={faltantes.includes(pq.id)}
                  alElegir={(det) => {
                    setSelecciones((s) => ({ ...s, [pq.id]: det }));
                    setFaltantes((f) => f.filter((x) => x !== pq.id));
                  }}
                />
              ))}
            </ol>

            <div className="mt-7 flex justify-end border-t border-linea pt-5">
              <Boton variante="principal" onClick={enviarBloque} ocupado={enviando} textoOcupado="Guardando respuestas…">
                {esUltimo ? 'Finalizar' : 'Siguiente'}
              </Boton>
            </div>
          </section>
        )}

        {etapa === 'completo' && cuestionario && (
          <section className="rounded-[6px] border border-linea bg-papel px-5 py-8 sm:px-9">
            <h1 ref={tituloRef} tabIndex={-1} className="regla-doble pb-3 text-titulo font-extrabold outline-none">
              Cuestionario completado
            </h1>
            <p className="mt-6 text-subtitulo">La evaluación ha sido enviada correctamente. ¡Muchas gracias!</p>
            <dl className="mt-6 grid gap-x-8 gap-y-2 text-cuerpo sm:grid-cols-[auto_1fr]">
              <dt className="font-bold">Fecha de finalización</dt>
              <dd className="cifras">{fechaHora(cuestionario.fechaFin)} hs</dd>
              <dt className="font-bold">Tiempo</dt>
              <dd className="cifras">{duracion(cuestionario.fechaInicio, cuestionario.fechaFin)}</dd>
              <dt className="font-bold">Cantidad de preguntas</dt>
              <dd className="cifras">{preguntas.length}</dd>
            </dl>
            <div className="mt-8 flex justify-end border-t border-linea pt-5">
              <Boton variante="principal" onClick={terminar}>
                Finalizar
              </Boton>
            </div>
          </section>
        )}
      </main>
    </div>
  );
}

function duracion(inicio: string | null, fin: string | null): string {
  const a = aFecha(inicio);
  const b = aFecha(fin);
  if (!a || !b) return '-';
  const minutos = Math.max(1, Math.round((b.getTime() - a.getTime()) / 60_000));
  return minutos === 1 ? '1 minuto' : `${minutos} minutos`;
}

/** Avance por bloques: un círculo por bloque, relleno en los ya enviados. */
function Progreso({ total, actual, respondidas, preguntas }: { total: number; actual: number; respondidas: number; preguntas: number }) {
  const porcentaje = preguntas ? Math.round((respondidas / preguntas) * 100) : 0;
  return (
    <div className="mt-4 flex flex-wrap items-center gap-x-4 gap-y-2">
      <div
        role="progressbar"
        aria-label="Avance del cuestionario"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={porcentaje}
        className="flex items-center gap-1.5"
      >
        {Array.from({ length: total }, (_, i) => (
          <span
            key={i}
            className={`h-3.5 w-3.5 rounded-full border-2 ${
              i + 1 < actual ? 'border-grafito bg-grafito' : i + 1 === actual ? 'border-tinta bg-papel' : 'border-linea bg-papel'
            }`}
          />
        ))}
      </div>
      <span className="cifras text-chica text-grafito-claro">
        {respondidas} de {preguntas} preguntas respondidas ({porcentaje}%)
      </span>
    </div>
  );
}

function PreguntaBloque({
  pq,
  total,
  seleccion,
  falta,
  alElegir,
}: {
  pq: PreguntaCuestionario;
  total: number;
  seleccion: number | undefined;
  falta: boolean;
  alElegir: (detalleId: number) => void;
}) {
  const opciones = [...pq.pregunta.opcionRespuesta.detalles].sort((a, b) => a.nroOrden - b.nroOrden);
  return (
    <li
      className={`rounded-[6px] border bg-papel px-5 py-5 sm:px-7 ${falta ? 'border-2 border-error' : 'border-linea'}`}
    >
      <fieldset>
        <legend id={`pregunta-${pq.id}`} tabIndex={-1} className="outline-none">
          <span className="cifras block text-chica font-bold text-tinta">
            Pregunta {pq.ordenVisual} de {total}
            {falta && <span className="text-error"> (sin responder)</span>}
          </span>
          <span className="dato mt-2 block text-subtitulo font-bold leading-snug text-grafito">{pq.pregunta.enunciado}</span>
        </legend>
        <div className="mt-4 divide-y divide-linea border-y border-linea">
          {opciones.map((o) => (
            <label key={o.id} className="flex cursor-pointer items-center gap-4 py-3 hover:bg-rosa-suave">
              <input
                type="radio"
                name={`pq-${pq.id}`}
                className="burbuja burbuja-grande"
                checked={seleccion === o.id}
                onChange={() => alElegir(o.id)}
              />
              <span className="dato text-cuerpo">{o.descripcion}</span>
            </label>
          ))}
        </div>
      </fieldset>
    </li>
  );
}
