import { Pencil, Plus, TrendingUp, X } from 'lucide-react'
import { useState } from 'react'
import type { MetaEconomia } from '../../api/metasEconomia'
import { SeletorMes } from '../../componentes/SeletorMes'
import { Alerta, BarraProgresso, Botao, Cartao, EstadoVazio, RodapeCartao, Selo } from '../../componentes/ui'
import { useCategorias } from '../../consultas/categorias'
import { useCancelarMetaEconomia, useMetasEconomia } from '../../consultas/metas'
import { mensagemDeErro } from '../../lib/erros'
import { formatarMoeda, formatarNomeDoMes, formatarPercentual, mesAtualEmSaoPaulo } from '../../lib/formatacao'
import { CabecalhoSecao } from './CabecalhoSecao'
import { FormularioMetaEconomia } from './FormularioMetaEconomia'
import { rotuloReferencia, statusMetaEconomia } from './rotulos'

type Formulario = { aberto: false } | { aberto: true; meta?: MetaEconomia }

export function MetasEconomia() {
  const [mes, setMes] = useState(mesAtualEmSaoPaulo)
  const [formulario, setFormulario] = useState<Formulario>({ aberto: false })
  const consulta = useMetasEconomia(mes)
  const categorias = useCategorias()

  // metas só são criadas para o mês atual ou futuro
  const passado = mes < mesAtualEmSaoPaulo()
  const metas = consulta.data ?? []
  const comMetaAtiva = new Set(metas.filter((meta) => meta.status !== 'CANCELADA').map((meta) => meta.categoria.id))
  const disponiveis = (categorias.data ?? []).filter(
    (categoria) => categoria.tipo === 'DESPESA' && !categoria.arquivada && !comMetaAtiva.has(categoria.id),
  )
  const podeCriar = !passado && !formulario.aberto

  function mudarMes(novo: string) {
    setMes(novo)
    setFormulario({ aberto: false })
  }

  return (
    <section aria-labelledby="titulo-economia" className="flex flex-col gap-4">
      <CabecalhoSecao
        id="titulo-economia"
        titulo={`Metas de economia · ${formatarNomeDoMes(mes)}`}
        subtitulo="Mais consciência nas escolhas do mês."
        acoes={
          <>
            <SeletorMes mes={mes} aoMudar={mudarMes} />
            {podeCriar && metas.length > 0 && (
              <Botao variante="secundario" Icone={Plus} onClick={() => setFormulario({ aberto: true })}>Nova meta de economia</Botao>
            )}
          </>
        }
      />

      {formulario.aberto && (
        <FormularioMetaEconomia
          key={formulario.meta?.id ?? 'nova'}
          mes={mes}
          categorias={disponiveis}
          meta={formulario.meta}
          aoConcluir={() => setFormulario({ aberto: false })}
        />
      )}

      {consulta.isPending && <p role="status" className="text-sm text-texto-suave">Carregando metas de economia…</p>}
      {consulta.isError && <p role="alert" className="text-sm text-erro-texto">{mensagemDeErro(consulta.error)}</p>}
      {consulta.isSuccess && metas.length === 0 && !formulario.aberto && (
        <EstadoVazio
          Icone={TrendingUp}
          titulo="Economizar hoje, conquistar amanhã"
          texto={passado ? 'Não houve metas de economia neste mês.' : 'Você ainda não tem uma meta de economia neste mês.'}
          acao={podeCriar && (
            <Botao variante="secundario" Icone={Plus} onClick={() => setFormulario({ aberto: true })}>Nova meta de economia</Botao>
          )}
        />
      )}

      {metas.length > 0 && (
        <div className="grid gap-5 [grid-template-columns:repeat(auto-fill,minmax(300px,1fr))]">
          {metas.map((meta) => (
            <CartaoMetaEconomia key={meta.id} meta={meta} aoEditar={() => setFormulario({ aberto: true, meta })} />
          ))}
        </div>
      )}
    </section>
  )
}

function CartaoMetaEconomia({ meta, aoEditar }: { meta: MetaEconomia; aoEditar: () => void }) {
  const [confirmando, setConfirmando] = useState(false)
  const cancelar = useCancelarMetaEconomia()
  const status = statusMetaEconomia[meta.status]
  const pendente = meta.status === 'AGUARDANDO_INICIO' || meta.status === 'EM_ANDAMENTO'
  const percentual = meta.valorLimite ? (meta.gasto / meta.valorLimite) * 100 : 0
  const referencia = rotuloReferencia(meta.tipoReferencia).toLowerCase()

  return (
    <Cartao>
      <div className="flex items-center justify-between gap-2.5">
        <h3 className="flex min-w-0 items-center gap-2 text-base font-semibold">
          <span aria-hidden="true" className="size-2.5 flex-none rounded-full" style={{ backgroundColor: meta.categoria.cor }} />
          <span className="truncate">{meta.categoria.nome}</span>
        </h3>
        <Selo tom={status.tom}>{status.rotulo}</Selo>
      </div>

      <p className="text-xs text-texto-suave">
        {meta.valorReferencia === null
          ? `Referência: ${referencia}`
          : `Referência: ${referencia} (${formatarMoeda(meta.valorReferencia)})`}
        {` · economizar ${formatarMoeda(meta.valorEconomia)}`}
      </p>

      <dl className="grid grid-cols-2 gap-x-4 gap-y-2.5 text-xs">
        <div>
          <dt className="text-texto-suave">Limite</dt>
          <dd className="mt-1 text-base font-bold">
            {meta.valorLimite === null ? 'Definido no início do mês' : formatarMoeda(meta.valorLimite)}
          </dd>
        </div>
        <div>
          <dt className="text-texto-suave">Gasto no mês</dt>
          <dd className="mt-1 text-base font-bold">{formatarMoeda(meta.gasto)}</dd>
        </div>
      </dl>

      {meta.valorLimite !== null && (
        <BarraProgresso
          percentual={percentual}
          tom={status.tom}
          rotulo={meta.status === 'NAO_ATINGIDA' ? 'Limite ultrapassado' : `${formatarPercentual(percentual)} do limite usado`}
        />
      )}

      <p className="text-[13px] font-semibold">{resumir(meta)}</p>

      {confirmando ? (
        <div className="mt-auto flex flex-col gap-2 border-t border-borda pt-3 text-[13px]">
          <span className="font-semibold text-texto-medio">Cancelar a meta? Ela fica no histórico e libera uma nova para a categoria.</span>
          {cancelar.error && <Alerta>{mensagemDeErro(cancelar.error)}</Alerta>}
          <div className="flex gap-2">
            <Botao variante="secundario" onClick={() => setConfirmando(false)}>Voltar</Botao>
            <Botao disabled={cancelar.isPending} onClick={() => cancelar.mutate(meta.id)}>Cancelar meta</Botao>
          </div>
        </div>
      ) : (
        pendente && (
          <RodapeCartao>
            {meta.status === 'AGUARDANDO_INICIO' ? (
              <Botao variante="texto" Icone={Pencil} className="-ml-3" onClick={aoEditar}>Editar</Botao>
            ) : (
              <span />
            )}
            <Botao variante="texto" Icone={X} onClick={() => { cancelar.reset(); setConfirmando(true) }}>Cancelar</Botao>
          </RodapeCartao>
        )
      )}
    </Cartao>
  )
}

function resumir(meta: MetaEconomia): string {
  const limite = meta.valorLimite ?? 0
  switch (meta.status) {
    case 'AGUARDANDO_INICIO':
      return 'Pode ser editada até o mês começar.'
    case 'EM_ANDAMENTO':
      return `Restam ${formatarMoeda(limite - meta.gasto)} até o limite.`
    case 'NAO_ATINGIDA':
      return `Passou ${formatarMoeda(meta.gasto - limite)} do limite. Resultado definitivo.`
    case 'ATINGIDA':
      return `Você economizou ${formatarMoeda(meta.economiaRealizada ?? 0)}.`
    case 'CANCELADA':
      return 'Meta cancelada. Fica apenas no histórico.'
  }
}
