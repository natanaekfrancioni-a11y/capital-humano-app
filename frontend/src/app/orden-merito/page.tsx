import { redirect } from 'next/navigation';

// Ruta vieja: el CU028 está en /reportes/orden-merito.
export default function OrdenMeritoViejoPage() {
  redirect('/reportes/orden-merito');
}
