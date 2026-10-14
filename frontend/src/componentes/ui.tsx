import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { IconeAlerta } from './Icones'

type Variante = 'primario' | 'secundario' | 'texto'

const estilosBotao: Record<Variante, string> = {
  primario: 'bg-destaque text-white hover:bg-destaque-escuro',
  secundario: 'border border-borda-campo bg-superficie text-tinta hover:bg-fundo',
  texto: 'bg-transparent text-texto-medio hover:bg-fundo',
}

export function Botao({
  variante = 'primario',
  className = '',
  type = 'button',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variante?: Variante }) {
  return (
    <button
      type={type}
      className={`inline-flex h-11 cursor-pointer items-center justify-center gap-2 rounded-[10px] px-4 text-sm font-bold disabled:cursor-not-allowed disabled:opacity-60 ${estilosBotao[variante]} ${className}`}
      {...props}
    />
  )
}

export function Campo({ rotulo, erro, children }: { rotulo: string; erro?: string; children: ReactNode }) {
  return (
    <label className="flex min-w-0 flex-col gap-1.5 text-[13px] font-semibold">
      {rotulo}
      {children}
      {erro && <span className="text-[13px] font-semibold text-erro-texto">{erro}</span>}
    </label>
  )
}

export function Alerta({ children }: { children: ReactNode }) {
  return (
    <div role="alert" className="flex items-start gap-2.5 rounded-[10px] bg-erro-fundo px-3 py-2.5 text-[13px] leading-snug text-erro-texto">
      <span className="mt-px shrink-0"><IconeAlerta tamanho={16} /></span>
      <span>{children}</span>
    </div>
  )
}

export function CabecalhoPagina({ titulo, descricao, acoes }: { titulo: string; descricao: string; acoes?: ReactNode }) {
  return (
    <header className="flex flex-wrap items-center justify-between gap-4">
      <div className="flex flex-col gap-1">
        <h1 className="text-[28px] font-extrabold tracking-tight">{titulo}</h1>
        <p className="text-sm text-texto-suave">{descricao}</p>
      </div>
      {acoes && <div className="flex flex-wrap items-center gap-3">{acoes}</div>}
    </header>
  )
}
