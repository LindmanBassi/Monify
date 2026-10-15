import { ArrowUpRight, ChartPie, Plus, TrendingUp, type LucideIcon } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { Link } from 'react-router'
import type { TotalCategoria } from '../../api/dashboard'
import type { Orcamento } from '../../api/orcamentos'
import { useAutenticacao } from '../../autenticacao/ContextoAutenticacao'
import { SeletorMes } from '../../componentes/SeletorMes'
import { BarraProgresso, CabecalhoPagina, Selo } from '../../componentes/ui'
import { classeBotao, estilosBotao } from '../../componentes/estilos'
import { useDashboard } from '../../consultas/dashboard'
import { useOrcamentos } from '../../consultas/orcamentos'
import { mensagemDeErro } from '../../lib/erros'
import { formatarMoeda, formatarPercentual, mesAtualEmSaoPaulo } from '../../lib/formatacao'
import { statusOrcamento } from '../orcamentos/status'
import { CartoesResumo } from './CartoesResumo'
import { GraficoEvolucao } from './GraficoEvolucao'
import { GraficoGastos } from './GraficoGastos'

export function Dashboard() {
  const [mes, setMes] = useState(mesAtualEmSaoPaulo)
  const { estado } = useAutenticacao()
  const consulta = useDashboard(mes)
  const primeiroNome = estado.situacao === 'autenticado' ? estado.usuario.nome.split(' ')[0] : ''
  const dados = consulta.data

  return (
    <>
      <CabecalhoPagina
        sobretitulo="Sua vida financeira"
        titulo={`Olá, ${primeiroNome}`}
        descricao="Vamos cuidar do seu dinheiro? Aqui está o resumo do mês."
        acoes={
          <>
            <SeletorMes mes={mes} aoMudar={setMes} />
            <Link to="/transacoes" className={`${classeBotao} ${estilosBotao.primario} no-underline`}>
              <Plus size={17} aria-hidden="true" />
              Nova transação
            </Link>
          </>
        }
      />

      {consulta.isPending && <p role="status" className="text-sm text-texto-suave">Carregando o resumo do mês…</p>}
      {consulta.isError && <p role="alert" className="text-sm text-erro-texto">{mensagemDeErro(consulta.error)}</p>}

      {dados && (
        <>
          <CartoesResumo dados={dados} />

          <div className="flex flex-wrap gap-5">
            <Painel titulo="Evolução financeira" subtitulo="Seu dinheiro ao longo do tempo" className="flex-[5_1_440px]">
              <GraficoEvolucao meses={dados.evolucaoMensal} />
            </Painel>
            <Painel
              titulo="Para onde vai seu dinheiro"
              subtitulo="Despesas por categoria"
              IconeTitulo={ChartPie}
              className="flex-[3_1_300px]"
            >
              <GraficoGastos gastos={dados.gastosPorCategoria} total={dados.resumo.despesas} />
            </Painel>
          </div>

          <div className="flex flex-wrap gap-5">
            <OrcamentosEmAtencao mes={mes} />
            <Painel
              titulo="De onde vem seu dinheiro"
              subtitulo="Receitas por categoria"
              IconeTitulo={TrendingUp}
              className="flex-[3_1_300px]"
            >
              <ReceitasPorCategoria receitas={dados.receitasPorCategoria} />
            </Painel>
          </div>
        </>
      )}
    </>
  )
}

type PropsPainel = {
  titulo: string
  subtitulo: string
  IconeTitulo?: LucideIcon
  acao?: ReactNode
  className: string
  children: ReactNode
}

function Painel({ titulo, subtitulo, IconeTitulo, acao, className, children }: PropsPainel) {
  return (
    <section aria-label={titulo} className={`flex min-w-0 flex-col gap-5 rounded-2xl border border-borda bg-superficie p-6 ${className}`}>
      <div className="flex items-start justify-between gap-3">
        <div className="flex flex-col gap-1">
          <h2 className="text-base font-bold">{titulo}</h2>
          <p className="text-xs text-destaque-escuro">{subtitulo}</p>
        </div>
        {acao}
        {IconeTitulo && <IconeTitulo size={19} className="text-texto-medio" aria-hidden="true" />}
      </div>
      {children}
    </section>
  )
}

function ReceitasPorCategoria({ receitas }: { receitas: TotalCategoria[] }) {
  if (receitas.length === 0) {
    return <p className="text-sm text-texto-suave">Nenhuma receita neste mês.</p>
  }
  return (
    <ul aria-label="Receitas por categoria" className="flex flex-col gap-4">
      {receitas.map(({ categoria, total, percentual }) => (
        <li key={categoria.id} className="flex flex-col gap-2">
          <div className="flex justify-between gap-3 text-[13px]">
            <span className="flex items-center gap-2">
              <span aria-hidden="true" className="size-2.5 rounded-sm" style={{ backgroundColor: categoria.cor }} />
              {categoria.nome}
            </span>
            <span><strong>{formatarMoeda(total)}</strong> <span className="text-xs text-texto-suave">· {formatarPercentual(percentual)}</span></span>
          </div>
          <div aria-hidden="true" className="h-2 overflow-hidden rounded-full bg-[#ECEFED]">
            <div className="h-full rounded-full" style={{ width: `${percentual}%`, backgroundColor: categoria.cor }} />
          </div>
        </li>
      ))}
    </ul>
  )
}

// o dashboard da API não traz orçamentos: aqui se usa a rota própria deles
function OrcamentosEmAtencao({ mes }: { mes: string }) {
  const consulta = useOrcamentos(mes)
  const orcamentos = consulta.data ?? []
  const emAtencao = orcamentos
    .filter((orcamento) => orcamento.status !== 'DENTRO_DO_LIMITE')
    .sort((a, b) => b.percentual - a.percentual)

  return (
    <Painel
      titulo="Orçamentos em atenção"
      subtitulo="Categorias perto ou acima do limite"
      acao={
        <Link to="/orcamentos" className="inline-flex items-center gap-1 text-xs font-semibold text-destaque-escuro no-underline hover:underline">
          Ver todos <ArrowUpRight size={15} aria-hidden="true" />
        </Link>
      }
      className="flex-[5_1_440px]"
    >
      {consulta.isError && <p role="alert" className="text-sm text-erro-texto">{mensagemDeErro(consulta.error)}</p>}
      {consulta.isSuccess && orcamentos.length === 0 && (
        <p className="text-sm text-texto-suave">Nenhum orçamento definido para este mês.</p>
      )}
      {orcamentos.length > 0 && emAtencao.length === 0 && (
        <p className="text-sm text-texto-suave">Todos os orçamentos estão dentro do limite.</p>
      )}
      {emAtencao.map((orcamento) => (
        <ResumoOrcamento key={orcamento.id} orcamento={orcamento} />
      ))}
    </Painel>
  )
}

function ResumoOrcamento({ orcamento }: { orcamento: Orcamento }) {
  const status = statusOrcamento[orcamento.status]
  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center justify-between gap-2 text-[13px]">
        <span className="flex items-center gap-2 font-semibold">
          <span aria-hidden="true" className="size-2.5 rounded-sm" style={{ backgroundColor: orcamento.categoria.cor }} />
          {orcamento.categoria.nome}
        </span>
        <span><strong>{formatarMoeda(orcamento.gasto)}</strong> <span className="text-texto-suave">de {formatarMoeda(orcamento.valorLimite)}</span></span>
      </div>
      <BarraProgresso percentual={orcamento.percentual} tom={status.tom} rotulo={`${formatarPercentual(orcamento.percentual)} do limite usado`} />
      <span className="self-start">
        <Selo tom={status.tom}>{status.rotulo} · {formatarPercentual(orcamento.percentual)}</Selo>
      </span>
    </div>
  )
}
