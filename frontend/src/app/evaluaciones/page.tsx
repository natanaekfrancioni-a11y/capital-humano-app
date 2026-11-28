import { redirect } from 'next/navigation';

// ruta vieja, el CU025 está en /evaluar
export default function EvaluacionesPage() {
  redirect('/evaluar');
}
