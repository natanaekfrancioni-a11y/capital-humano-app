/** Validaciones de formulario de las altas (flujos alternativos 2.A y 2.B). */

export interface Regla {
  campo: string;
  valor: string;
  obligatorio?: boolean;
  maximo?: number;
}

export interface ResultadoValidacion {
  omitidos: string[];
  excedidos: string[];
  /** Error por campo, para marcarlo debajo del control. */
  porCampo: Record<string, string>;
  /** Mensajes para el aviso de arriba del formulario. */
  mensajes: string[];
}

export function validar(reglas: Regla[]): ResultadoValidacion {
  const omitidos: string[] = [];
  const excedidos: string[] = [];
  const porCampo: Record<string, string> = {};
  for (const r of reglas) {
    const v = r.valor.trim();
    if (r.obligatorio && v === '') {
      omitidos.push(r.campo);
      porCampo[r.campo] = 'Dato obligatorio.';
    } else if (r.maximo !== undefined && v.length > r.maximo) {
      excedidos.push(`${r.campo} (máximo ${r.maximo})`);
      porCampo[r.campo] = `Supera los ${r.maximo} caracteres.`;
    }
  }
  const mensajes: string[] = [];
  if (omitidos.length) {
    mensajes.push(`Los siguientes campos son obligatorios: ${omitidos.join(', ')}. Es necesario completarlos para continuar.`);
  }
  if (excedidos.length) {
    mensajes.push(
      `Se ha superado el límite de caracteres en los siguientes campos: ${excedidos.join(', ')}. Por favor, acorte el texto para continuar.`,
    );
  }
  return { omitidos, excedidos, porCampo, mensajes };
}
