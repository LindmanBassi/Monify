import { zodResolver } from '@hookform/resolvers/zod'
import { ChartPie, Pencil, Plus, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { Orcamento } from '../../api/orcamentos'
import { SeletorMes } from '../../componentes/SeletorMes'
import { classeCampo } from '../../componentes/estilos'
import { Alerta, BarraProgresso, Botao, BotaoIcone, CabecalhoPagina, Cartao, EstadoVazio, RodapeCartao, Selo, Valor } from '../../componentes/ui'
import { useCategorias } from '../../consultas/categorias'
import { useDashboard } from '../../consultas/dashboard'
import { useAlterarLimite, useExcluirOrcamento, useOrcamentos } from '../../consultas/orcamentos'
import { mensagemDeErro } from '../../lib/erros'
import { valorPositivo } from '../../lib/esquemas'
import {
  converterValor,
  formatarMoeda,
  formatarNomeDoMes,
  formatarPercentual,
  mesAtualEmSaoPaulo,
  valorParaCampo,
} from '../../lib/formatacao'
import { CartoesResumo } from '../dashboard/CartoesResumo'
import { FormularioOrcamento } from './FormularioOrcamento'
import { statusOrcamento } from './status'

export function Orcamentos() {
  const [mes, setMes] = useState(mesAtualEmSaoPaulo)
  const [definindo, setDefinindo] = useState(false)
  const consulta = useOrcamentos(mes)
  const resumo = useDashboard(mes)
  const categorias = useCategorias()

  // meses passados são histórico
  const passado = mes < mesAtualEmSaoPaulo()
  const orcamentos = consulta.data ?? []
  const comOrcamento = new Set(orcamentos.map((orcamento) => orcamento.categoria.id))
  const semOrcamento = (categorias.data ?? []).filter(
    (categoria) => categoria.tipo === 'DESPESA' && !categoria.arquivada && !comOrcamento.has(categoria.id),
  )

  function mudarMes(novo: string) {
    setMes(novo)
    setDefinindo(false)
  }

  return (
    <>
      <CabecalhoPagina
        sobretitulo="Planeje com tranquilidade"
        titulo={`Orçamentos de ${formatarNomeDoMes(mes)}`}
        descricao="Defina seus limites e mantenha os gastos sob controle."
        acoes={
          <>
            <SeletorMes mes={mes} aoMudar={mudarMes} />
            {!passado && !definindo && <Botao Icone={Plus} onClick={() => setDefinindo(true)}>Definir orçamento</Botao>}
          </>
        }
      />

      {resumo.data && <CartoesResumo dados={resumo.data} />}

      {definindo && <FormularioOrcamento mes={mes} categorias={semOrcamento} aoConcluir={() => setDefinindo(false)} />}

      {consulta.isPending && <p role="status" className="text-sm text-texto-suave">Carregando orçamentos…</p>}
      {consulta.isError && <p role="alert" className="text-sm text-erro-texto">{mensagemDeErro(consulta.error)}</p>}
      {consulta.isSuccess && orcamentos.length === 0 && (
        <EstadoVazio
          Icone={ChartPie}
          titulo="Nenhum orçamento neste mês"
          texto={passado ? 'Não houve orçamentos definidos neste mês.' : 'Defina um limite para as categorias em que você quer gastar com mais atenção.'}
          acao={!passado && !definindo && <Botao variante="secundario" Icone={Plus} onClick={() => setDefinindo(true)}>Definir orçamento</Botao>}
        />
      )}

      {orcamentos.length > 0 && (
        <section aria-label="Orçamentos do mês" className="grid gap-5 [grid-template-columns:repeat(auto-fill,minmax(280px,1fr))]">
          {orcamentos.map((orcamento) => (
            <CartaoOrcamento key={orcamento.id} orcamento={orcamento} editavel={!passado} />
          ))}
        </section>
      )}

      <p className="max-w-3xl text-xs text-texto-suave">
        Orçamentos de meses passados ficam como histórico e não podem ser alterados. Excluir um orçamento cancela as
        metas de economia pendentes ligadas a ele.
      </p>
    </>
  )
}

function CartaoOrcamento({ orcamento, editavel }: { orcamento: Orcamento; editavel: boolean }) {
  const [acao, setAcao] = useState<'nenhuma' | 'alterando' | 'excluindo'>('nenhuma')
  const excluir = useExcluirOrcamento()
  const status = statusOrcamento[orcamento.status]
  const estourado = orcamento.restante < 0
  const { nome, cor } = orcamento.categoria

  return (
    <Cartao>
      <div className="flex items-center justify-between gap-2.5">
        <h2 className="flex min-w-0 items-center gap-2 text-base font-semibold">
          <span aria-hidden="true" className="size-2.5 flex-none rounded-full" style={{ backgroundColor: cor }} />
          <span className="truncate">{nome}</span>
        </h2>
        <Selo tom={status.tom}>{status.rotulo}</Selo>
      </div>

      <div className="flex flex-wrap items-baseline gap-1.5">
        <Valor valor={orcamento.gasto} className="text-[28px] leading-none" />
        <span className="text-xs text-texto-suave">de {formatarMoeda(orcamento.valorLimite)}</span>
      </div>

      <BarraProgresso percentual={orcamento.percentual} tom={status.tom} rotulo={`${formatarPercentual(orcamento.percentual)} do limite usado`} />

      <div className="flex justify-between gap-2 text-xs">
        <span className="text-texto-suave">{formatarPercentual(orcamento.percentual)} utilizado</span>
        <span className={`font-bold ${estourado ? 'text-despesa' : 'text-receita'}`}>
          {estourado ? `${formatarMoeda(Math.abs(orcamento.restante))} acima` : `Restam ${formatarMoeda(orcamento.restante)}`}
        </span>
      </div>

      {orcamento.categoria.arquivada && (
        <p className="text-xs text-texto-suave">Categoria arquivada: o orçamento continua valendo neste mês.</p>
      )}

      {editavel && acao === 'alterando' && <FormularioLimite orcamento={orcamento} aoConcluir={() => setAcao('nenhuma')} />}

      {editavel && acao === 'excluindo' && (
        <div className="mt-auto flex flex-col gap-2 border-t border-borda pt-3 text-[13px]">
          <span className="font-semibold text-texto-medio">Excluir? As metas de economia pendentes ligadas a ele serão canceladas.</span>
          {excluir.error && <Alerta>{mensagemDeErro(excluir.error)}</Alerta>}
          <div className="flex gap-2">
            <Botao variante="secundario" onClick={() => setAcao('nenhuma')}>Voltar</Botao>
            <Botao disabled={excluir.isPending} onClick={() => excluir.mutate(orcamento.id)}>Excluir</Botao>
          </div>
        </div>
      )}

      {editavel && acao === 'nenhuma' && (
        <RodapeCartao>
          <Botao variante="texto" Icone={Pencil} className="-ml-3" onClick={() => setAcao('alterando')}>Alterar limite</Botao>
          <BotaoIcone Icone={Trash2} aria-label={`Excluir orçamento de ${nome}`} onClick={() => { excluir.reset(); setAcao('excluindo') }} />
        </RodapeCartao>
      )}
    </Cartao>
  )
}

const esquemaLimite = z.object({ valorLimite: valorPositivo() })

function FormularioLimite({ orcamento, aoConcluir }: { orcamento: Orcamento; aoConcluir: () => void }) {
  const alterar = useAlterarLimite()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<z.infer<typeof esquemaLimite>>({
    resolver: zodResolver(esquemaLimite),
    defaultValues: { valorLimite: valorParaCampo(orcamento.valorLimite) },
  })

  return (
    <form
      noValidate
      aria-label={`Alterar limite de ${orcamento.categoria.nome}`}
      onSubmit={handleSubmit(({ valorLimite }) =>
        alterar.mutate({ id: orcamento.id, valorLimite: converterValor(valorLimite) ?? 0 }, { onSuccess: aoConcluir }),
      )}
      className="mt-auto flex flex-col gap-2 border-t border-borda pt-3"
    >
      <label className="flex flex-col gap-1.5 text-[13px] font-semibold">
        Novo limite
        <input type="text" inputMode="decimal" {...register('valorLimite')} className={classeCampo(!!errors.valorLimite)} />
        {errors.valorLimite && <span className="text-erro-texto">{errors.valorLimite.message}</span>}
      </label>
      {alterar.error && <Alerta>{mensagemDeErro(alterar.error)}</Alerta>}
      <div className="flex gap-2">
        <Botao variante="secundario" onClick={aoConcluir}>Cancelar</Botao>
        <Botao type="submit" disabled={alterar.isPending}>Salvar</Botao>
      </div>
    </form>
  )
}
