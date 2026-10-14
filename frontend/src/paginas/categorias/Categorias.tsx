import { useState } from 'react'
import type { Categoria, TipoTransacao } from '../../api/categorias'
import { Botao, CabecalhoPagina } from '../../componentes/ui'
import { mensagemDeErro } from '../../lib/erros'
import {
  useArquivarCategoria,
  useCategorias,
  useCriarCategoria,
  useDesarquivarCategoria,
  useEditarCategoria,
} from '../../consultas/categorias'
import { FormularioCategoria, type DadosCategoria } from './FormularioCategoria'

const abas: { tipo: TipoTransacao; rotulo: string }[] = [
  { tipo: 'DESPESA', rotulo: 'Despesas' },
  { tipo: 'RECEITA', rotulo: 'Receitas' },
]

export function Categorias() {
  const [tipo, setTipo] = useState<TipoTransacao>('DESPESA')
  const [mostrarArquivadas, setMostrarArquivadas] = useState(false)
  const [criando, setCriando] = useState(false)

  const categorias = useCategorias()
  const criar = useCriarCategoria()

  const visiveis = (categorias.data ?? []).filter(
    (categoria) => categoria.tipo === tipo && (mostrarArquivadas || !categoria.arquivada),
  )

  function salvarNova(dados: DadosCategoria) {
    criar.mutate(dados, {
      onSuccess: () => {
        setCriando(false)
        setTipo(dados.tipo)
      },
    })
  }

  return (
    <div className="flex flex-col gap-5">
      <CabecalhoPagina
        titulo="Categorias"
        descricao="Organize receitas e despesas. O tipo de uma categoria não muda depois de criada."
        acoes={!criando && <Botao onClick={() => { criar.reset(); setCriando(true) }}>Nova categoria</Botao>}
      />

      {criando && (
        <FormularioCategoria
          titulo="Nova categoria"
          inicial={{ tipo }}
          permitirTipo
          erro={mensagemDeErro(criar.error)}
          salvando={criar.isPending}
          aoSalvar={salvarNova}
          aoCancelar={() => setCriando(false)}
        />
      )}

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div role="tablist" aria-label="Tipo de categoria" className="grid grid-cols-2 gap-1 rounded-xl bg-[#E9EBEF] p-1">
          {abas.map((aba) => (
            <button
              key={aba.tipo}
              type="button"
              role="tab"
              aria-selected={tipo === aba.tipo}
              onClick={() => setTipo(aba.tipo)}
              className={`h-10 cursor-pointer rounded-[9px] px-5 text-sm ${
                tipo === aba.tipo ? 'bg-superficie font-bold shadow-sm' : 'font-semibold text-texto-suave'
              }`}
            >
              {aba.rotulo}
            </button>
          ))}
        </div>
        <label className="flex min-h-11 items-center gap-2 text-sm font-semibold text-texto-medio">
          <input
            type="checkbox"
            checked={mostrarArquivadas}
            onChange={(evento) => setMostrarArquivadas(evento.target.checked)}
            className="size-4 accent-destaque"
          />
          Mostrar arquivadas
        </label>
      </div>

      {categorias.isPending && <p role="status" className="text-sm text-texto-suave">Carregando categorias…</p>}
      {categorias.isError && <p role="alert" className="text-sm text-erro-texto">{mensagemDeErro(categorias.error)}</p>}

      {categorias.isSuccess && (
        <ul aria-label="Categorias" className="flex flex-col overflow-hidden rounded-[14px] border border-borda bg-superficie">
          {visiveis.length === 0 && (
            <li className="p-5 text-sm text-texto-suave">Nenhuma categoria por aqui.</li>
          )}
          {visiveis.map((categoria) => (
            <ItemCategoria key={categoria.id} categoria={categoria} />
          ))}
        </ul>
      )}
    </div>
  )
}

function ItemCategoria({ categoria }: { categoria: Categoria }) {
  const [editando, setEditando] = useState(false)
  const [confirmandoArquivo, setConfirmandoArquivo] = useState(false)
  const editar = useEditarCategoria()
  const arquivar = useArquivarCategoria()
  const desarquivar = useDesarquivarCategoria()

  if (editando) {
    return (
      <li className="border-t border-[#EEF0F3] p-3 first:border-t-0">
        <FormularioCategoria
          titulo={`Editar ${categoria.nome}`}
          inicial={{ nome: categoria.nome, tipo: categoria.tipo, cor: categoria.cor }}
          permitirTipo={false}
          erro={mensagemDeErro(editar.error)}
          salvando={editar.isPending}
          aoSalvar={({ nome, cor }) =>
            editar.mutate({ id: categoria.id, nome, cor }, { onSuccess: () => setEditando(false) })
          }
          aoCancelar={() => setEditando(false)}
        />
      </li>
    )
  }

  return (
    <li className="flex flex-wrap items-center justify-between gap-3 border-t border-[#EEF0F3] px-5 py-3 first:border-t-0">
      <div className="flex min-w-0 items-center gap-3">
        <span aria-hidden="true" className="size-3.5 shrink-0 rounded-full" style={{ backgroundColor: categoria.cor }} />
        <span className={`truncate text-sm font-bold ${categoria.arquivada ? 'text-texto-suave' : ''}`}>{categoria.nome}</span>
        {categoria.arquivada && (
          <span className="rounded-full bg-[#ECEDF0] px-2.5 py-0.5 text-xs font-bold text-texto-medio">Arquivada</span>
        )}
      </div>

      {confirmandoArquivo ? (
        <div className="flex flex-wrap items-center gap-2 text-[13px]">
          <span className="font-semibold text-texto-medio">Arquivar? As metas de economia pendentes dela serão canceladas.</span>
          <Botao variante="secundario" onClick={() => setConfirmandoArquivo(false)}>Voltar</Botao>
          <Botao
            disabled={arquivar.isPending}
            onClick={() => arquivar.mutate(categoria.id, { onSuccess: () => setConfirmandoArquivo(false) })}
          >
            Arquivar
          </Botao>
        </div>
      ) : (
        <div className="flex gap-1">
          {!categoria.arquivada && (
            <Botao variante="texto" onClick={() => { editar.reset(); setEditando(true) }}>Editar</Botao>
          )}
          {categoria.arquivada ? (
            <Botao variante="texto" disabled={desarquivar.isPending} onClick={() => desarquivar.mutate(categoria.id)}>
              Desarquivar
            </Botao>
          ) : (
            <Botao variante="texto" onClick={() => setConfirmandoArquivo(true)}>Arquivar</Botao>
          )}
        </div>
      )}
    </li>
  )
}
