import type { CandidatoClave } from './tipos';

const dos = (n: number) => String(n).padStart(2, '0');

/** nombre del archivo del CU025: aaaa-mm-dd-hh-mm-NombreFunción (sin caracteres inválidos en Windows) */
export function nombreArchivoClaves(nombreFuncion: string, fecha = new Date()): string {
  const marca = `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}-${dos(fecha.getHours())}-${dos(fecha.getMinutes())}`;
  const funcion = nombreFuncion.replace(/[\\/:*?"<>|]+/g, ' ').replace(/\s+/g, ' ').trim();
  return `${marca}-${funcion}`;
}

/** genera el .xls (Excel 97-2003) con la lista de candidatos y sus claves de ingreso */
export async function libroClaves(
  candidatos: CandidatoClave[],
  puesto: { nombre: string; empresa: string },
): Promise<Blob> {
  const XLSX = await import('xlsx');
  const filas: (string | number)[][] = [
    ['Puesto', puesto.nombre],
    ['Empresa', puesto.empresa],
    [],
    ['Apellido', 'Nombre', 'Tipo de documento', 'Número de documento', 'Clave de ingreso'],
    ...candidatos.map((c) => [c.apellido, c.nombre, c.tipoDocumento, c.nroDocumento, c.claveIngreso]),
  ];
  const hoja = XLSX.utils.aoa_to_sheet(filas);
  hoja['!cols'] = [{ wch: 22 }, { wch: 26 }, { wch: 18 }, { wch: 20 }, { wch: 16 }];
  const libro = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(libro, hoja, 'Claves de ingreso');
  const datos = XLSX.write(libro, { bookType: 'biff8', type: 'array' }) as ArrayBuffer;
  return new Blob([datos], { type: 'application/vnd.ms-excel' });
}

// File System Access API (Chrome y Edge): permite elegir la carpeta donde guardar
export interface ManejadorCarpeta {
  name: string;
  getFileHandle(
    nombre: string,
    opciones: { create: boolean },
  ): Promise<{ createWritable(): Promise<{ write(datos: Blob): Promise<void>; close(): Promise<void> }> }>;
}

type VentanaConCarpetas = { showDirectoryPicker?: (opciones?: { mode?: 'readwrite' }) => Promise<ManejadorCarpeta> };

export function puedeElegirCarpeta(): boolean {
  return typeof window !== 'undefined' && typeof (window as unknown as VentanaConCarpetas).showDirectoryPicker === 'function';
}

/** abre el selector de carpetas del sistema, devuelve null si el usuario lo cancela */
export async function elegirCarpeta(): Promise<ManejadorCarpeta | null> {
  const ventana = window as unknown as VentanaConCarpetas;
  if (!ventana.showDirectoryPicker) return null;
  try {
    return await ventana.showDirectoryPicker({ mode: 'readwrite' });
  } catch {
    return null;
  }
}

export async function guardarEnCarpeta(carpeta: ManejadorCarpeta, nombreArchivo: string, datos: Blob) {
  const archivo = await carpeta.getFileHandle(nombreArchivo, { create: true });
  const escritura = await archivo.createWritable();
  await escritura.write(datos);
  await escritura.close();
}

/** para navegadores sin selector de carpetas: descarga el archivo */
export function descargar(nombreArchivo: string, datos: Blob) {
  const url = URL.createObjectURL(datos);
  const a = document.createElement('a');
  a.href = url;
  a.download = nombreArchivo;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
}
