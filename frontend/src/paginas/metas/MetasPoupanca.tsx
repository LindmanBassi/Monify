import { Pencil, Plus, Target, X } from 'lucide-react'
import { useState } from 'react'
import type { MetaPoupanca } from '../../api/metasPoupanca'
import { Alerta, BarraProgresso, Botao, BotaoIcone, Cartao, EstadoVazio, Selo, Valor } from '../../componentes/ui'
import { useCancelarMetaPoupanca, useMetasPoupanca } from '../../consultas/metas'
import { mensagemDeErro } from '../../lib/erros'
import { formatarData, formatarMoeda, formatarPercentual } from '../../lib/formatacao'
import { CabecalhoSecao } from './CabecalhoSecao'
import { Extrato } from './Extrato'
import { FormularioAporte } from './FormularioAporte'
import { FormularioMetaPoupanca } from './FormularioMetaPoupanca'
import { statusMetaPoupanca } from './rotulos'

type Formulario = { aberto: false } | { aberto: true; meta?: MetaPoupanca }

export function MetasPoupanca() {
  const [formulario, setFormulario] = useState<Formulario>({ aberto: false })
  const [mostrarCanceladas, setMostrarCanceladas] = useState(false)
  const consulta = useMetasPoupanca()

  const todas = consulta.data ?? []
  const metas = todas.filter((meta) => mostrarCanceladas || meta.status !== 'CANCELADA')
  const temCanceladas = todas.some((meta) => meta.status === 'CANCELADA')

  return (
    <section aria-labelledby="titulo-poupanca" className="flex flex-col gap-4">
      <CabecalhoSecao
        id="titulo-poupanca"
        titulo="Metas de poupança"
        subtitulo="Um destino para cada valor guardado."
        acoes={
          <>
            {temCanceladas && (
              <label className="flex min-h-11 items-center gap-2 text-sm text-texto-medio">
                <input
                  type="checkbox"
                  checked={mostrarCanceladas}
                  onChange={(evento) => setMostrarCanceladas(evento.target.checked)}
                  className="size-4 accent-destaque-escuro"
                />
                Mostrar canceladas
              </label>
            )}
            {!formulario.aberto && (
              <Botao variante="secundario" Icone={Plus} onClick={() => setFormulario({ aberto: true })}>Nova meta de poupança</Botao>
            )}
          </>
        }
      />

      {formulario.aberto && (
        <FormularioMetaPoupanca
          key={formulario.meta?.id ?? 'nova'}
          meta={formulario.meta}
          aoConcluir={() => setFormulario({ aberto: false })}
        />
      )}

      {consulta.isPending && <p role="status" className="text-sm text-texto-suave">Carregando metas de poupança…</p>}
      {consulta.isError && <p role="alert" className="text-sm text-erro-texto">{mensagemDeErro(consulta.error)}</p>}
      {consulta.isSuccess && metas.length === 0 && !formulario.aberto && (
        <EstadoVazio
          Icone={Target}
          titulo="Qual é o seu próximo sonho?"
          texto="Uma viagem, uma reserva ou um novo começo. Dê um nome ao seu objetivo."
          acao={<Botao variante="secundario" Icone={Plus} onClick={() => setFormulario({ aberto: true })}>Criar meta de poupança</Botao>}
        />
      )}

      {metas.length > 0 && (
        <div className="grid items-start gap-5 [grid-template-columns:repeat(auto-fill,minmax(300px,1fr))]">
          {metas.map((meta) => (
            <CartaoMetaPoupanca key={meta.id} meta={meta} aoEditar={() => setFormulario({ aberto: true, meta })} />
          ))}
        </div>
      )}
    </section>
  )
}

type Painel = 'nenhum' | 'lancamento' | 'cancelar' | 'extrato'

function CartaoMetaPoupanca({ meta, aoEditar }: { meta: MetaPoupanca; aoEditar: () => void }) {
  const [painel, setPainel] = useState<Painel>('nenhum')
  const cancelar = useCancelarMetaPoupanca()
  const status = statusMetaPoupanca[meta.status]
  const ativa = meta.status === 'ATIVA'

  return (
    <Cartao>
      <div className="flex items-center justify-between gap-2.5">
        <h3 title={meta.nome} className="truncate text-base font-semibold">{meta.nome}</h3>
        <span className="flex flex-none items-center gap-0.5">
          <Selo tom={status.tom}>{status.rotulo}</Selo>
          {ativa && (
            <>
              <BotaoIcone Icone={Pencil} className="ml-1.5 size-8" aria-label={`Editar ${meta.nome}`} onClick={aoEditar} />
              <BotaoIcone Icone={X} className="size-8" aria-label={`Cancelar ${meta.nome}`} onClick={() => { cancelar.reset(); setPainel('cancelar') }} />
            </>
          )}
        </span>
      </div>

      <div className="flex flex-wrap items-baseline gap-1.5">
        <Valor valor={meta.acumulado} className="text-[28px] leading-none" />
        <span className="text-xs text-texto-suave">de {formatarMoeda(meta.valorAlvo)}</span>
      </div>

      <BarraProgresso
        percentual={meta.percentual}
        tom={meta.status === 'CANCELADA' ? 'neutro' : 'sucesso'}
        rotulo={`${formatarPercentual(meta.percentual)} do alvo`}
      />

      <p className="text-xs text-texto-medio">{detalhar(meta)}</p>

      {painel === 'lancamento' && <FormularioAporte meta={meta} aoConcluir={() => setPainel('nenhum')} />}

      {painel === 'cancelar' && (
        <div className="flex flex-col gap-2 border-t border-borda pt-3 text-[13px]">
          <span className="font-semibold text-texto-medio">
            Cancelar a meta? O acumulado fica no histórico e ela não aceita mais lançamentos.
          </span>
          {cancelar.error && <Alerta>{mensagemDeErro(cancelar.error)}</Alerta>}
          <div className="flex gap-2">
            <Botao variante="secundario" onClick={() => setPainel('nenhum')}>Voltar</Botao>
            <Botao disabled={cancelar.isPending} onClick={() => cancelar.mutate(meta.id)}>Cancelar meta</Botao>
          </div>
        </div>
      )}

      {painel === 'extrato' && <Extrato metaId={meta.id} nome={meta.nome} />}

      {(painel === 'nenhum' || painel === 'extrato') && (
        <div className="flex flex-wrap items-center gap-1 border-t border-borda pt-3">
          {ativa && <Botao className="mr-1" onClick={() => setPainel('lancamento')}>Registrar aporte</Botao>}
          {meta.status === 'CONCLUIDA' && <Botao className="mr-1" onClick={() => setPainel('lancamento')}>Registrar resgate</Botao>}
          <Botao
            variante="texto"
            aria-expanded={painel === 'extrato'}
            onClick={() => setPainel((atual) => (atual === 'extrato' ? 'nenhum' : 'extrato'))}
          >
            {painel === 'extrato' ? 'Ocultar extrato' : 'Ver extrato'}
          </Botao>
        </div>
      )}
    </Cartao>
  )
}

function detalhar(meta: MetaPoupanca): string {
  if (meta.status === 'CONCLUIDA') {
    return 'Objetivo atingido · aceita apenas resgates'
  }
  if (meta.status === 'CANCELADA') {
    return 'Meta cancelada · o acumulado fica no histórico'
  }
  const faltam = `faltam ${formatarMoeda(meta.faltante)}`
  if (!meta.prazo) {
    return `Sem prazo definido · ${faltam}`
  }
  if (meta.prazoVencido) {
    return `Prazo vencido em ${formatarData(meta.prazo)} · ${faltam}`
  }
  return `Prazo: ${formatarData(meta.prazo)} · guarde ${formatarMoeda(meta.valorPorMes ?? 0)} por mês`
}
