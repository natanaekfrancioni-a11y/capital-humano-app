/** Indicador de pasos de un asistente (CU025): cada paso es un círculo numerado de la hoja de respuestas. */
export function Pasos({ pasos, actual }: { pasos: string[]; actual: number }) {
  return (
    <ol className="mb-8 flex flex-wrap items-center gap-x-3 gap-y-2" aria-label="Pasos">
      {pasos.map((paso, i) => {
        const estado = i < actual ? 'hecho' : i === actual ? 'actual' : 'pendiente';
        return (
          <li key={paso} className="flex items-center gap-3" aria-current={estado === 'actual' ? 'step' : undefined}>
            <span
              className={`cifras grid h-8 w-8 place-items-center rounded-full border-2 text-chica font-extrabold ${
                estado === 'actual'
                  ? 'border-tinta bg-tinta text-papel'
                  : estado === 'hecho'
                    ? 'border-grafito bg-grafito text-papel'
                    : 'border-linea bg-papel text-grafito-claro'
              }`}
            >
              {i + 1}
            </span>
            <span className={`text-chica font-bold ${estado === 'pendiente' ? 'text-grafito-claro' : 'text-grafito'}`}>
              {paso}
              {estado === 'hecho' && <span className="sr-only"> (completado)</span>}
            </span>
            {i < pasos.length - 1 && <span aria-hidden="true" className="h-px w-8 bg-linea sm:w-14" />}
          </li>
        );
      })}
    </ol>
  );
}
