'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import React from 'react';

const NAV_ITEMS = [
  { href: '/', label: 'Candidatos' },
  { href: '/evaluaciones', label: 'Evaluar' },
  { href: '/cuestionario', label: 'Cuestionario' },
  { href: '/reportes/orden-merito', label: 'Orden de Méritos' },
  { href: '/reportes/comparativo', label: 'Reporte Comparativo' },
  { href: '/puestos', label: 'Puestos' },
  { href: '/preguntas', label: 'Preguntas' },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  return (
    <div className="min-h-screen bg-[#fdf4f5] text-slate-900">
      <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/95 backdrop-blur">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-4 py-3 sm:px-6">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-900 text-sm font-bold text-white">
              CH
            </div>
            <div>
              <p className="text-sm font-semibold tracking-tight text-slate-900">
                Capital Humano
              </p>
              <p className="text-xs text-slate-500">Evaluación y Perfiles</p>
            </div>
          </div>

          <nav className="flex flex-wrap items-center gap-1">
            {NAV_ITEMS.map((item) => {
              const active =
                item.href === '/'
                  ? pathname === '/'
                  : pathname === item.href || pathname.startsWith(`${item.href}/`);
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`rounded-lg px-3 py-1.5 text-xs font-medium transition sm:text-sm ${
                    active
                      ? 'bg-slate-900 text-white shadow-xs'
                      : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
                  }`}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-7 sm:px-6">{children}</main>
    </div>
  );
}
