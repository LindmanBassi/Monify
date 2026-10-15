import { ArrowLeftRight, Pencil, Plus, Search, Trash2 } from 'lucide-react'
import { useState } from 'react'
import type { TipoTransacao } from '../../api/categorias'
import type { FiltrosTransacoes, Transacao } from '../../api/transacoes'
import { SeloCategoria } from '../../componentes/IconeCategoria'
import { OpcoesPorTipo } from '../../componentes/OpcoesPorTipo'
import { Botao, BotaoIcone, CabecalhoPagina, EstadoVazio } from '../../componentes/ui'
import { classeCampo } from '../../componentes/estilos'
import { useCategorias } from '../../consultas/categorias'
import { useExcluirTransacao, useTransacoes } from '../../consultas/transacoes'
import { mensagemDeErro } from '../../lib/erros'
import { formatarData, formatarMoeda } from '../../lib/formatacao'
import { useValorAtrasado } from '../../lib/useValorAtrasado'
import { FormularioTransacao } from './FormularioTransacao'

type Formulario = { aberto: false } | { aberto: true; transacao?: Transacao }

export function Transacoes() {
  const [filtros, setFiltros] = useState<FiltrosTransacoes>({})
  const [pagina, setPagina] = useState(0)
  const [formulario, setFormulario] = useState<Formulario>({ aberto: false })

  const texto = useValorAtrasado(filtros.texto ?? '')
  const periodoInvalido = !!filtros.inicio && !!filtros.fim && filtros.inicio > filtros.fim
  const consulta = useTransacoes({ ...filtros, texto }, pagina, !periodoInvalido)
  const categorias = useCategorias()
  const filtrando = Object.values(filtros).some((valor) => !!valor)

  function alterarFiltro(mudanca: Partial<FiltrosTransacoes>) {
    setFiltros((atuais) => ({ ...atuais, ...mudanca }))
    setPagina(0)
  }

  const resultado = consulta.data

  return (
    <>
      <CabecalhoPagina
        sobretitulo="Cada movimento conta"
        titulo="Transações"
        descricao="Todas as suas entradas e saídas, em um só lugar."
        acoes={!formulario.aberto && <Botao Icone={Plus} onClick={() => setFormulario({ aberto: true })}>Nova transação</Botao>}
      />

      {formulario.aberto && (
        <FormularioTransacao
          key={formulario.transacao?.id ?? 'nova'}
          transacao={formulario.transacao}
          categorias={categorias.data ?? []}
          aoConcluir={() => setFormulario({ aberto: false })}
        />
      )}

      <section aria-label="Filtros" className="flex flex-col gap-2">
        <div className="flex flex-wrap gap-3">
          <div className="relative flex-[2_1_240px]">
            <Search size={17} className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-texto-suave" aria-hidden="true" />
            <input
              type="search"
              aria-label="Buscar na descrição"
              placeholder="Buscar uma transação..."
              value={filtros.texto ?? ''}
              onChange={(e) => alterarFiltro({ texto: e.target.value })}
              className={`${classeCampo()} pl-10`}
            />
          </div>
          <select aria-label="Categoria" value={filtros.categoriaId ?? ''} onChange={(e) => alterarFiltro({ categoriaId: e.target.value })} className={`${classeCampo()} flex-[1_1_190px]`}>
            <option value="">Todas as categorias</option>
            <OpcoesPorTipo categorias={categorias.data ?? []} />
          </select>
          <select
            aria-label="Tipo"
            value={filtros.tipo ?? ''}
            onChange={(e) => alterarFiltro({ tipo: (e.target.value || undefined) as TipoTransacao | undefined })}
            className={`${classeCampo()} flex-[1_1_190px]`}
          >
            <option value="">Receitas e despesas</option>
            <option value="RECEITA">Receitas</option>
            <option value="DESPESA">Despesas</option>
          </select>
          <input type="date" aria-label="De" title="De" value={filtros.inicio ?? ''} onChange={(e) => alterarFiltro({ inicio: e.target.value })} className={`${classeCampo()} flex-[1_1_150px]`} />
          <input type="date" aria-label="Até" title="Até" value={filtros.fim ?? ''} onChange={(e) => alterarFiltro({ fim: e.target.value })} className={`${classeCampo(periodoInvalido)} flex-[1_1_150px]`} />
        </div>
        {periodoInvalido && <span className="text-[13px] font-semibold text-erro-texto">A data inicial não pode ser depois da final</span>}
      </section>

      {consulta.isError && <p role="alert" className="text-sm text-erro-texto">{mensagemDeErro(consulta.error)}</p>}
      {consulta.isPending && !periodoInvalido && <p role="status" className="text-sm text-texto-suave">Carregando transações…</p>}

      {resultado && resultado.conteudo.length === 0 && (
        <EstadoVazio
          Icone={ArrowLeftRight}
          titulo={filtrando ? 'Nenhuma transação encontrada' : 'Nenhuma transação ainda'}
          texto={filtrando ? 'Tente outros filtros ou um período maior.' : 'Registre sua primeira receita ou despesa para começar.'}
        />
      )}

      {resultado && resultado.conteudo.length > 0 && (
        <section aria-label="Lista de transações" className="overflow-hidden rounded-2xl border border-borda bg-superficie">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] border-collapse text-[13px]">
              <thead>
                <tr className="bg-fundo/70 text-left text-[11px] tracking-[0.08em] text-texto-suave uppercase">
                  <th scope="col" className="px-5 py-3.5 font-semibold">Descrição</th>
                  <th scope="col" className="px-5 py-3.5 font-semibold">Categoria</th>
                  <th scope="col" className="px-5 py-3.5 font-semibold">Data</th>
                  <th scope="col" className="px-5 py-3.5 text-right font-semibold">Valor</th>
                  <th scope="col" className="px-5 py-3.5 text-right font-semibold">Ações</th>
                </tr>
              </thead>
              <tbody>
                {resultado.conteudo.map((transacao) => (
                  <LinhaTransacao key={transacao.id} transacao={transacao} aoEditar={() => setFormulario({ aberto: true, transacao })} />
                ))}
              </tbody>
            </table>
          </div>
          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-borda px-5 py-3.5 text-xs text-texto-suave">
            <span>{resumirPagina(resultado.pagina, resultado.tamanho, resultado.conteudo.length, resultado.totalElementos)}</span>
            <div className="flex items-center gap-3">
              <span>Mais recentes primeiro</span>
              {resultado.totalPaginas > 1 && (
                <div className="flex gap-2">
                  <Botao variante="secundario" className="h-9" disabled={pagina === 0} onClick={() => setPagina(pagina - 1)}>Anterior</Botao>
                  <Botao variante="secundario" className="h-9" disabled={pagina + 1 >= resultado.totalPaginas} onClick={() => setPagina(pagina + 1)}>Próxima</Botao>
                </div>
              )}
            </div>
          </div>
        </section>
      )}
    </>
  )
}

function resumirPagina(pagina: number, tamanho: number, nestaPagina: number, total: number): string {
  if (total <= tamanho) {
    return `${total} ${total === 1 ? 'transação' : 'transações'}`
  }
  const primeiro = pagina * tamanho + 1
  return `Mostrando ${primeiro}–${primeiro + nestaPagina - 1} de ${total} transações`
}

function LinhaTransacao({ transacao, aoEditar }: { transacao: Transacao; aoEditar: () => void }) {
  const [confirmando, setConfirmando] = useState(false)
  const excluir = useExcluirTransacao()
  const receita = transacao.tipo === 'RECEITA'
  const nome = transacao.descricao ?? 'transação'

  return (
    <tr className="border-t border-borda">
      <td className="px-5 py-3.5">
        <span className="flex items-center gap-3">
          <SeloCategoria icone={transacao.categoria.icone} cor={transacao.categoria.cor} tamanho="pequeno" />
          <span className="font-semibold">{transacao.descricao ?? '—'}</span>
        </span>
      </td>
      <td className="px-5 py-3.5 text-texto-medio">
        <span className="inline-flex items-center gap-2">
          <span aria-hidden="true" className="size-2 rounded-sm" style={{ backgroundColor: transacao.categoria.cor }} />
          {transacao.categoria.nome}
        </span>
      </td>
      <td className="px-5 py-3.5 whitespace-nowrap text-texto-medio">{formatarData(transacao.dataOcorrencia)}</td>
      <td className={`px-5 py-3.5 text-right font-bold whitespace-nowrap ${receita ? 'text-receita' : ''}`}>
        {receita ? '+ ' : '− '}{formatarMoeda(transacao.valor)}
      </td>
      <td className="px-3 py-2 text-right whitespace-nowrap">
        {confirmando ? (
          <span className="inline-flex items-center gap-1">
            <Botao variante="texto" className="h-9" onClick={() => setConfirmando(false)}>Voltar</Botao>
            <Botao className="h-9" disabled={excluir.isPending} onClick={() => excluir.mutate(transacao.id)}>Excluir</Botao>
          </span>
        ) : (
          <span className="inline-flex gap-1">
            <BotaoIcone Icone={Pencil} aria-label={`Editar ${nome}`} onClick={aoEditar} />
            <BotaoIcone Icone={Trash2} aria-label={`Excluir ${nome}`} onClick={() => setConfirmando(true)} />
          </span>
        )}
      </td>
    </tr>
  )
}
