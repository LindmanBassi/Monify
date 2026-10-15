import { CircleAlert, type LucideIcon } from 'lucide-react'
import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { partesDaMoeda } from '../lib/formatacao'
import { classeBotao, estilosBotao, type Variante } from './estilos'

type PropsBotao = ButtonHTMLAttributes<HTMLButtonElement> & {
  variante?: Variante
  Icone?: LucideIcon
}

export function Botao({ variante = 'primario', Icone, className = '', type = 'button', children, ...props }: PropsBotao) {
  return (
    <button type={type} className={`${classeBotao} ${estilosBotao[variante]} ${className}`} {...props}>
      {Icone && <Icone size={17} aria-hidden="true" />}
      {children}
    </button>
  )
}

// botão só com ícone: o nome acessível vem do aria-label
export function BotaoIcone({ Icone, className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { Icone: LucideIcon; 'aria-label': string }) {
  return (
    <button
      type="button"
      title={props['aria-label']}
      className={`inline-flex size-10 cursor-pointer items-center justify-center rounded-[10px] text-texto-medio hover:bg-fundo hover:text-tinta disabled:cursor-not-allowed disabled:opacity-60 ${className}`}
      {...props}
    >
      <Icone size={18} aria-hidden="true" />
    </button>
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
      <CircleAlert size={16} className="mt-px shrink-0" aria-hidden="true" />
      <span>{children}</span>
    </div>
  )
}

type PropsCabecalho = {
  sobretitulo: string
  titulo: string
  descricao: string
  acoes?: ReactNode
}

export function CabecalhoPagina({ sobretitulo, titulo, descricao, acoes }: PropsCabecalho) {
  return (
    <header className="flex flex-wrap items-end justify-between gap-4">
      <div className="flex flex-col gap-1.5">
        <span className="text-[11px] font-bold tracking-[0.14em] text-texto-suave uppercase">{sobretitulo}</span>
        <h1 className="text-[30px] leading-tight font-bold tracking-tight">{titulo}</h1>
        <p className="text-sm text-texto-suave">{descricao}</p>
      </div>
      {acoes && <div className="flex flex-wrap items-center gap-3">{acoes}</div>}
    </header>
  )
}

export type Tom = 'sucesso' | 'atencao' | 'perigo' | 'info' | 'neutro'

const estilosSelo: Record<Tom, string> = {
  sucesso: 'bg-destaque-suave text-destaque-escuro',
  atencao: 'bg-[#FDF1DC] text-[#7A4D00]',
  perigo: 'bg-erro-fundo text-erro-texto',
  info: 'bg-[#E4ECFB] text-[#2C4A8A]',
  neutro: 'bg-[#ECEFED] text-texto-medio',
}

export function Selo({ tom, children }: { tom: Tom; children: ReactNode }) {
  return (
    <span className={`rounded-md px-2 py-0.5 text-[11px] font-semibold whitespace-nowrap ${estilosSelo[tom]}`}>{children}</span>
  )
}

const coresBarra: Record<Tom, string> = {
  sucesso: 'bg-receita',
  atencao: 'bg-[#D99A1E]',
  perigo: 'bg-despesa',
  info: 'bg-[#3B6FD4]',
  neutro: 'bg-[#A3ABA7]',
}

export function BarraProgresso({ percentual, tom, rotulo }: { percentual: number; tom: Tom; rotulo: string }) {
  const largura = Math.max(0, Math.min(percentual, 100))
  return (
    <div role="img" aria-label={rotulo} className="h-2 overflow-hidden rounded-full bg-[#ECEFED]">
      <div className={`h-full rounded-full ${coresBarra[tom]}`} style={{ width: `${largura}%` }} />
    </div>
  )
}

export function Cartao({ children, className = '' }: { children: ReactNode; className?: string }) {
  return (
    <article className={`flex flex-col gap-4 rounded-2xl border border-borda bg-superficie p-6 ${className}`}>{children}</article>
  )
}

// rodapé do cartão, com as ações separadas por uma linha
export function RodapeCartao({ children }: { children: ReactNode }) {
  return <div className="mt-auto flex items-center justify-between gap-2 border-t border-borda pt-3">{children}</div>
}

// "R$" menor que o número, como nos cartões de valor
export function Valor({ valor, className = '' }: { valor: number; className?: string }) {
  const { sinal, simbolo, numero } = partesDaMoeda(valor)
  return (
    <span className={`font-bold tracking-tight ${className}`}>
      <span className="text-[0.6em] font-semibold">{sinal}{simbolo}</span> {numero}
    </span>
  )
}

type PropsEstadoVazio = {
  Icone: LucideIcon
  titulo: string
  texto: string
  acao?: ReactNode
}

export function EstadoVazio({ Icone, titulo, texto, acao }: PropsEstadoVazio) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-borda-campo px-6 py-12 text-center">
      <span aria-hidden="true" className="flex size-14 items-center justify-center rounded-full bg-destaque-suave text-destaque-escuro">
        <Icone size={24} />
      </span>
      <h3 className="text-base font-semibold">{titulo}</h3>
      <p className="max-w-md text-[13px] text-texto-suave">{texto}</p>
      {acao && <div className="mt-2">{acao}</div>}
    </div>
  )
}
