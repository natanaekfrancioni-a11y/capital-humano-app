import type { Metadata, Viewport } from 'next';
import type { ReactNode } from 'react';
import localFont from 'next/font/local';
import './globals.css';
import { DialogoProvider } from '@/components/ui/Dialogo';

// fuente Atkinson Hyperlegible Next (SIL OFL 1.1), se lee fácil incluso contra reloj
const atkinson = localFont({
  src: '../fonts/atkinson-hyperlegible-next-latin-wght-normal.woff2',
  weight: '200 800',
  style: 'normal',
  display: 'swap',
  variable: '--font-atkinson',
});

export const metadata: Metadata = {
  title: 'Capit@l Humano — Evaluación de perfiles',
  description: 'Gestión de evaluaciones de perfiles por competencias',
};

export const viewport: Viewport = {
  themeColor: '#f6edef',
};

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="es" className={atkinson.variable}>
      <body className="min-h-dvh antialiased">
        <DialogoProvider>{children}</DialogoProvider>
      </body>
    </html>
  );
}
