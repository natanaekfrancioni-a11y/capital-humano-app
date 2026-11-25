import { redirect } from 'next/navigation';

// Ruta vieja: el CU025 está en /evaluar.
export default function EvaluacionesPage() {
  redirect('/evaluar');
}
