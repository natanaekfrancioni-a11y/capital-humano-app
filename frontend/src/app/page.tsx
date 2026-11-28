'use client';

import { useRouter } from 'next/navigation';
import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Boton } from '@/components/ui/Boton';
import { CampoSeleccion, CampoTexto } from '@/components/ui/Campo';
import { useAvisos } from '@/components/ui/Dialogo';
import { ApiError, api } from '@/lib/api';
import { guardarSesionCandidato, guardarSesionConsultor, useSesionCandidato, useSesionConsultor } from '@/lib/sesion';

type Acceso = 'consultor' | 'candidato';

const TIPOS_DOCUMENTO = ['DNI', 'LE', 'LC', 'PP'].map((t) => ({ valor: t, texto: t }));

/** CU001 - Autenticar usuario */
export default function IngresoPage() {
  const router = useRouter();
  const avisos = useAvisos();
  const consultor = useSesionConsultor();
  const candidato = useSesionCandidato();

  const [acceso, setAcceso] = useState<Acceso | null>(null);
  const [usuario, setUsuario] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [tipoDocumento, setTipoDocumento] = useState('DNI');
  const [nroDocumento, setNroDocumento] = useState('');
  const [clave, setClave] = useState('');
  const [enviando, setEnviando] = useState(false);
  const primerCampo = useRef<HTMLInputElement>(null);

  // si ya hay una sesión abierta se retoma
  useEffect(() => {
    if (consultor) router.replace('/inicio');
    else if (candidato) router.replace('/cuestionario');
  }, [consultor, candidato, router]);

  useEffect(() => {
    if (acceso) primerCampo.current?.focus();
  }, [acceso]);

  function blanquear() {
    setUsuario('');
    setContrasena('');
    setNroDocumento('');
    setClave('');
  }

  function salir() {
    blanquear();
    setTipoDocumento('DNI');
    setAcceso(null);
  }

  async function ingresar(e: FormEvent) {
    e.preventDefault();
    if (!acceso || enviando) return;
    setEnviando(true);
    try {
      if (acceso === 'consultor') {
        const sesion = await api.ingresarConsultor(usuario, contrasena);
        guardarSesionConsultor(sesion);
        router.replace('/inicio');
      } else {
        const sesion = await api.ingresarCandidato(tipoDocumento, nroDocumento, clave);
        guardarSesionCandidato(sesion);
        router.replace('/cuestionario');
      }
    } catch (err) {
      // se muestra el error, se limpian los campos y se vuelve a pedir el ingreso
      if (err instanceof ApiError && !err.sinConexion) {
        await avisos.abrir({ tipo: 'error', titulo: 'No se pudo ingresar', mensaje: err.message });
        blanquear();
        primerCampo.current?.focus();
      } else {
        await avisos.error(err, 'No se pudo ingresar');
      }
    } finally {
      setEnviando(false);
    }
  }

  const completo =
    acceso === 'consultor'
      ? usuario.trim() !== '' && contrasena !== ''
      : acceso === 'candidato' && nroDocumento.trim() !== '' && clave.trim() !== '';

  return (
    <main className="grid min-h-dvh place-items-center px-4 py-10">
      <div className="grid w-full max-w-[60rem] items-start gap-10 md:grid-cols-[1fr_minmax(0,27rem)] md:gap-16">
        <div className="md:pt-10">
          <p className="text-portada font-extrabold tracking-[-0.02em] text-grafito sm:text-[3.25rem] sm:leading-none">
            Capit@l Humano
          </p>
          <p className="mt-4 max-w-[34ch] text-subtitulo text-grafito-claro">
            Evaluación de perfiles por competencias para la selección de personal TIC.
          </p>
        </div>

        <form onSubmit={ingresar} className="rounded-[6px] border border-linea bg-papel px-6 py-7 sm:px-8" noValidate>
          <h1 className="regla-doble pb-3 text-titulo font-extrabold text-grafito">Ingresar</h1>

          <fieldset className="mt-6">
            <legend className="text-chica font-bold text-grafito">Tipo de acceso</legend>
            <div className="mt-2 divide-y divide-linea border-y border-linea">
              {(
                [
                  ['consultor', 'Ingresar como consultor', 'Gestión de puestos, preguntas, evaluaciones y reportes.'],
                  ['candidato', 'Realizar un cuestionario', 'Con el documento y la clave que le entregó la consultora.'],
                ] as const
              ).map(([valor, titulo, detalle]) => (
                <label key={valor} className="flex cursor-pointer items-start gap-3 py-3.5">
                  <input
                    type="radio"
                    name="acceso"
                    value={valor}
                    checked={acceso === valor}
                    onChange={() => {
                      blanquear();
                      setAcceso(valor);
                    }}
                    className="burbuja burbuja-grande mt-0.5"
                  />
                  <span>
                    <span className="block font-bold text-grafito">{titulo}</span>
                    <span className="block text-chica text-grafito-claro">{detalle}</span>
                  </span>
                </label>
              ))}
            </div>
          </fieldset>

          {acceso === 'consultor' && (
            <div className="mt-6 grid gap-4">
              <CampoTexto
                ref={primerCampo}
                etiqueta="Usuario"
                obligatorio
                valor={usuario}
                alCambiar={setUsuario}
                maxLength={15}
                autoComplete="username"
              />
              <CampoTexto
                etiqueta="Contraseña"
                obligatorio
                type="password"
                valor={contrasena}
                alCambiar={setContrasena}
                mayusculas={false}
                maxLength={20}
                autoComplete="current-password"
              />
            </div>
          )}

          {acceso === 'candidato' && (
            <div className="mt-6 grid gap-4 sm:grid-cols-[7rem_1fr]">
              <CampoSeleccion
                etiqueta="Tipo"
                obligatorio
                valor={tipoDocumento}
                alCambiar={setTipoDocumento}
                opciones={TIPOS_DOCUMENTO}
                textoVacio="Tipo"
              />
              <CampoTexto
                ref={primerCampo}
                etiqueta="Número de documento"
                obligatorio
                inputMode="numeric"
                valor={nroDocumento}
                alCambiar={setNroDocumento}
                maxLength={25}
              />
              <CampoTexto
                className="sm:col-span-2"
                etiqueta="Clave del cuestionario"
                obligatorio
                type="password"
                valor={clave}
                alCambiar={setClave}
                maxLength={8}
                ayuda="8 letras y números."
              />
            </div>
          )}

          <div className="mt-7 flex flex-wrap justify-between gap-3 border-t border-linea pt-5">
            <Boton variante="secundario" onClick={salir} disabled={!acceso}>
              Salir
            </Boton>
            <Boton type="submit" variante="principal" disabled={!acceso || !completo} ocupado={enviando} textoOcupado="Verificando…">
              {acceso === 'candidato' ? 'Ingresar al cuestionario' : 'Ingresar'}
            </Boton>
          </div>
        </form>
      </div>
    </main>
  );
}
