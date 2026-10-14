import { useState } from 'react'
import type { Categoria, TipoTransacao } from '../../api/categorias'
import type { FiltrosTransacoes, Transacao } from '../../api/transacoes'
import { Botao, CabecalhoPagina, Campo } from '../../componentes/ui'
import { classeCampo } from '../../componentes/estilos'
import { mensagemDeErro } from '../../lib/erros'
import { useCategorias } from '../../consultas/categorias'
import { useExcluirTransacao, useTransacoes } from '../../consultas/transacoes'
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

  function alterarFiltro(mudanca: Partial<FiltrosTransacoes>) {
    setFiltros((atuais) => ({ ...atuais, ...mudanca }))
    setPagina(0)
  }

  const resultado = consulta.data
  const primeiro = resultado ? resultado.pagina * resultado.tamanho + 1 : 0
  const ultimo = resultado ? primeiro + resultado.conteudo.length - 1 : 0

  return (
    <div className="flex flex-col gap-5">
      <CabecalhoPagina
        titulo="Transações"
        descricao="Histórico do mais recente para o mais antigo."
        acoes={!formulario.aberto && <Botao onClick={() => setFormulario({ aberto: true })}>Nova transação</Botao>}
      />

      {formulario.aberto && (
        <FormularioTransacao
          key={formulario.transacao?.id ?? 'nova'}
          transacao={formulario.transacao}
          categorias={categorias.data ?? []}
          aoConcluir={() => setFormulario({ aberto: false })}
        />
      )}

      <section aria-label="Filtros" className="grid items-end gap-3 rounded-[14px] border border-borda bg-superficie p-4 [grid-template-columns:repeat(auto-fit,minmax(160px,1fr))]">
        <Campo rotulo="De">
          <input type="date" value={filtros.inicio ?? ''} onChange={(e) => alterarFiltro({ inicio: e.target.value })} className={classeCampo()} />
        </Campo>
        <Campo rotulo="Até" erro={periodoInvalido ? 'A data inicial não pode ser depois da final' : undefined}>
          <input type="date" value={filtros.fim ?? ''} onChange={(e) => alterarFiltro({ fim: e.target.value })} className={classeCampo(periodoInvalido)} />
        </Campo>
        <Campo rotulo="Categoria">
          <select value={filtros.categoriaId ?? ''} onChange={(e) => alterarFiltro({ categoriaId: e.target.value })} className={classeCampo()}>
            <option value="">Todas</option>
            {(categorias.data ?? []).map((categoria: Categoria) => (
              <option key={categoria.id} value={categoria.id}>
                {categoria.nome}{categoria.arquivada ? ' (arquivada)' : ''} · {categoria.tipo === 'RECEITA' ? 'receita' : 'despesa'}
              </option>
            ))}
          </select>
        </Campo>
        <Campo rotulo="Tipo">
          <select
            value={filtros.tipo ?? ''}
            onChange={(e) => alterarFiltro({ tipo: (e.target.value || undefined) as TipoTransacao | undefined })}
            className={classeCampo()}
          >
            <option value="">Receitas e despesas</option>
            <option value="RECEITA">Receitas</option>
            <option value="DESPESA">Despesas</option>
          </select>
        </Campo>
        <Campo rotulo="Buscar na descrição">
          <input type="search" placeholder="Ex.: mercado" value={filtros.texto ?? ''} onChange={(e) => alterarFiltro({ texto: e.target.value })} className={classeCampo()} />
        </Campo>
      </section>

      <section aria-label="Lista de transações" className="overflow-hidden rounded-[14px] border border-borda bg-superficie">
        {consulta.isError && <p role="alert" className="p-5 text-sm text-erro-texto">{mensagemDeErro(consulta.error)}</p>}
        {consulta.isPending && !periodoInvalido && <p role="status" className="p-5 text-sm text-texto-suave">Carregando transações…</p>}

        {resultado && resultado.conteudo.length === 0 && (
          <p className="p-5 text-sm text-texto-suave">Nenhuma transação encontrada com esses filtros.</p>
        )}

        {resultado && resultado.conteudo.length > 0 && (
          <>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[720px] border-collapse text-sm">
                <thead>
                  <tr className="bg-[#F8F9FB] text-left text-xs tracking-wide text-texto-medio uppercase">
                    <th scope="col" className="px-4 py-3 font-bold">Data</th>
                    <th scope="col" className="px-4 py-3 font-bold">Descrição</th>
                    <th scope="col" className="px-4 py-3 font-bold">Categoria</th>
                    <th scope="col" className="px-4 py-3 text-right font-bold">Valor</th>
                    <th scope="col" className="px-4 py-3 text-right font-bold">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {resultado.conteudo.map((transacao) => (
                    <LinhaTransacao
                      key={transacao.id}
                      transacao={transacao}
                      aoEditar={() => setFormulario({ aberto: true, transacao })}
                    />
                  ))}
                </tbody>
              </table>
            </div>
            <div className="flex flex-wrap items-center justify-between gap-3 border-t border-[#EEF0F3] px-4 py-3 text-[13px] text-texto-medio">
              <span>Mostrando {primeiro}–{ultimo} de {resultado.totalElementos} transações</span>
              <div className="flex gap-2">
                <Botao variante="secundario" disabled={pagina === 0} onClick={() => setPagina(pagina - 1)}>Anterior</Botao>
                <Botao variante="secundario" disabled={pagina + 1 >= resultado.totalPaginas} onClick={() => setPagina(pagina + 1)}>Próxima</Botao>
              </div>
            </div>
          </>
        )}
      </section>
    </div>
  )
}

function LinhaTransacao({ transacao, aoEditar }: { transacao: Transacao; aoEditar: () => void }) {
  const [confirmando, setConfirmando] = useState(false)
  const excluir = useExcluirTransacao()
  const receita = transacao.tipo === 'RECEITA'

  return (
    <tr className="border-t border-[#EEF0F3]">
      <td className="px-4 py-3 whitespace-nowrap text-texto-medio">{formatarData(transacao.dataOcorrencia)}</td>
      <td className="px-4 py-3 font-semibold">{transacao.descricao ?? '—'}</td>
      <td className="px-4 py-3">
        <span className="inline-flex items-center gap-2">
          <span aria-hidden="true" className="size-2.5 rounded-full" style={{ backgroundColor: transacao.categoria.cor }} />
          {transacao.categoria.nome}
        </span>
      </td>
      <td className={`px-4 py-3 text-right font-bold whitespace-nowrap ${receita ? 'text-receita' : ''}`}>
        {receita ? '+ ' : '− '}{formatarMoeda(transacao.valor)}
      </td>
      <td className="px-2 py-1 text-right whitespace-nowrap">
        {confirmando ? (
          <span className="inline-flex items-center gap-1">
            <Botao variante="texto" onClick={() => setConfirmando(false)}>Voltar</Botao>
            <Botao disabled={excluir.isPending} onClick={() => excluir.mutate(transacao.id)}>Excluir</Botao>
          </span>
        ) : (
          <span className="inline-flex gap-1">
            <Botao variante="texto" aria-label={`Editar ${transacao.descricao ?? 'transação'}`} onClick={aoEditar}>Editar</Botao>
            <Botao variante="texto" aria-label={`Excluir ${transacao.descricao ?? 'transação'}`} onClick={() => setConfirmando(true)}>Excluir</Botao>
          </span>
        )}
      </td>
    </tr>
  )
}
