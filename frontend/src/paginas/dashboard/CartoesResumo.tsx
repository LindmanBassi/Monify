import { ArrowDownLeft, ArrowUpRight, CalendarDays, ShieldCheck, Wallet, type LucideIcon } from 'lucide-react'
import type { Dashboard } from '../../api/dashboard'
import { Valor } from '../../componentes/ui'
import { formatarNomeDoMes, formatarPercentual, somarMeses } from '../../lib/formatacao'

// receitas, despesas e saldo do mês, com a variação em relação ao mês anterior
export function CartoesResumo({ dados }: { dados: Dashboard }) {
  const mesAnterior = formatarNomeDoMes(somarMeses(dados.mes, -1))
  const { resumo, comparacaoMesAnterior: variacao } = dados

  return (
    <section aria-label="Resumo do mês" className="grid gap-5 [grid-template-columns:repeat(auto-fit,minmax(240px,1fr))]">
      <CartaoValor
        rotulo="Receitas do mês"
        valor={resumo.receitas}
        classeValor="text-receita"
        Icone={ArrowDownLeft}
        classeIcone="text-receita"
        legenda={textoVariacao(variacao.receitas, mesAnterior) ?? 'Entradas no período'}
        IconeLegenda={CalendarDays}
      />
      <CartaoValor
        rotulo="Despesas do mês"
        valor={resumo.despesas}
        classeValor="text-despesa"
        Icone={ArrowUpRight}
        classeIcone="text-despesa"
        legenda={textoVariacao(variacao.despesas, mesAnterior) ?? 'Saídas no período'}
        IconeLegenda={CalendarDays}
      />
      <CartaoValor
        rotulo="Saldo do mês"
        valor={resumo.saldo}
        Icone={Wallet}
        legenda={textoVariacao(variacao.saldo, mesAnterior) ?? 'Receitas menos despesas'}
        IconeLegenda={ShieldCheck}
        escuro
      />
    </section>
  )
}

function textoVariacao(variacao: number | null, mesAnterior: string): string | null {
  if (variacao === null) {
    return null
  }
  if (variacao === 0) {
    return `Igual a ${mesAnterior}`
  }
  const seta = variacao > 0 ? '▲' : '▼'
  return `${seta} ${formatarPercentual(Math.abs(variacao))} em relação a ${mesAnterior}`
}

type PropsCartao = {
  rotulo: string
  valor: number
  Icone: LucideIcon
  legenda: string
  IconeLegenda: LucideIcon
  classeValor?: string
  classeIcone?: string
  escuro?: boolean
}

function CartaoValor({ rotulo, valor, Icone, legenda, IconeLegenda, classeValor = '', classeIcone = '', escuro = false }: PropsCartao) {
  return (
    <div
      role="group"
      aria-label={rotulo}
      className={`relative flex flex-col gap-5 overflow-hidden rounded-2xl p-6 ${
        escuro ? 'bg-escuro text-white' : 'border border-borda bg-superficie'
      }`}
    >
      {escuro && (
        <>
          <span aria-hidden="true" className="absolute -top-16 -right-10 size-56 rounded-full border-[28px] border-white/[0.04]" />
          <span aria-hidden="true" className="absolute -right-24 -bottom-28 size-64 rounded-full border-[28px] border-white/[0.03]" />
        </>
      )}
      <div className="relative flex items-center justify-between gap-3">
        <span className={`text-sm font-semibold ${escuro ? 'text-white' : ''}`}>{rotulo}</span>
        <span
          aria-hidden="true"
          className={`flex size-9 items-center justify-center rounded-full ${escuro ? 'bg-white/10 text-white' : `bg-fundo ${classeIcone}`}`}
        >
          <Icone size={17} />
        </span>
      </div>
      <Valor valor={valor} className={`relative text-[34px] leading-none ${classeValor}`} />
      <span className={`relative flex items-center gap-1.5 text-xs ${escuro ? 'text-[#C9D2CD]' : 'text-texto-suave'}`}>
        <IconeLegenda size={14} aria-hidden="true" />
        {legenda}
      </span>
    </div>
  )
}
