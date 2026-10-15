import { Archive, ArchiveRestore, Pencil, Plus, Shapes } from 'lucide-react'
import { useState } from 'react'
import type { Categoria, TipoTransacao } from '../../api/categorias'
import { SeloCategoria } from '../../componentes/IconeCategoria'
import { Botao, BotaoIcone, CabecalhoPagina, EstadoVazio, RodapeCartao, Selo } from '../../componentes/ui'
import {
  useArquivarCategoria,
  useCategorias,
  useCriarCategoria,
  useDesarquivarCategoria,
  useEditarCategoria,
} from '../../consultas/categorias'
import { mensagemDeErro } from '../../lib/erros'
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

  function abrirCriacao() {
    criar.reset()
    setCriando(true)
  }

  return (
    <>
      <CabecalhoPagina
        sobretitulo="Tudo no seu lugar"
        titulo="Categorias"
        descricao="Uma organização que faz sentido para você. O tipo de uma categoria não muda depois de criada."
        acoes={!criando && <Botao Icone={Plus} onClick={abrirCriacao}>Nova categoria</Botao>}
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
        <div role="tablist" aria-label="Tipo de categoria" className="grid grid-cols-2 gap-1 rounded-xl bg-[#E8ECEA] p-1">
          {abas.map((aba) => (
            <button
              key={aba.tipo}
              type="button"
              role="tab"
              aria-selected={tipo === aba.tipo}
              onClick={() => setTipo(aba.tipo)}
              className={`h-10 cursor-pointer rounded-[9px] px-5 text-sm ${
                tipo === aba.tipo ? 'bg-superficie font-semibold shadow-sm' : 'font-medium text-texto-suave'
              }`}
            >
              {aba.rotulo}
            </button>
          ))}
        </div>
        <label className="flex min-h-11 items-center gap-2 text-sm text-texto-medio">
          <input
            type="checkbox"
            checked={mostrarArquivadas}
            onChange={(evento) => setMostrarArquivadas(evento.target.checked)}
            className="size-4 accent-destaque-escuro"
          />
          Mostrar arquivadas
        </label>
      </div>

      {categorias.isPending && <p role="status" className="text-sm text-texto-suave">Carregando categorias…</p>}
      {categorias.isError && <p role="alert" className="text-sm text-erro-texto">{mensagemDeErro(categorias.error)}</p>}

      {categorias.isSuccess && visiveis.length === 0 && (
        <EstadoVazio
          Icone={Shapes}
          titulo="Nenhuma categoria por aqui"
          texto="Crie categorias para organizar suas receitas e despesas do seu jeito."
          acao={<Botao variante="secundario" Icone={Plus} onClick={abrirCriacao}>Criar categoria</Botao>}
        />
      )}

      {categorias.isSuccess && visiveis.length > 0 && (
        <ul aria-label="Categorias" className="grid gap-5 [grid-template-columns:repeat(auto-fill,minmax(250px,1fr))]">
          {visiveis.map((categoria) => (
            <ItemCategoria key={categoria.id} categoria={categoria} />
          ))}
        </ul>
      )}
    </>
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
      <li className="col-span-full">
        <FormularioCategoria
          titulo={`Editar ${categoria.nome}`}
          inicial={{ nome: categoria.nome, tipo: categoria.tipo, cor: categoria.cor, icone: categoria.icone }}
          permitirTipo={false}
          erro={mensagemDeErro(editar.error)}
          salvando={editar.isPending}
          aoSalvar={({ nome, cor, icone }) =>
            editar.mutate({ id: categoria.id, nome, cor, icone }, { onSuccess: () => setEditando(false) })
          }
          aoCancelar={() => setEditando(false)}
        />
      </li>
    )
  }

  return (
    <li className="flex flex-col gap-5 rounded-2xl border border-borda bg-superficie p-6">
      <div className="flex items-start gap-3.5">
        <SeloCategoria icone={categoria.icone} cor={categoria.cor} />
        <div className="flex min-w-0 flex-1 flex-col gap-0.5">
          <h2 className={`truncate text-base font-semibold ${categoria.arquivada ? 'text-texto-suave' : ''}`}>{categoria.nome}</h2>
          <span className="text-xs text-texto-suave">
            Categoria de {categoria.tipo === 'RECEITA' ? 'receita' : 'despesa'}
          </span>
        </div>
        {categoria.arquivada && <Selo tom="neutro">Arquivada</Selo>}
      </div>

      {confirmandoArquivo ? (
        <div className="mt-auto flex flex-col gap-2 border-t border-borda pt-3 text-[13px]">
          <span className="font-semibold text-texto-medio">Arquivar? As metas de economia pendentes dela serão canceladas.</span>
          <div className="flex gap-2">
            <Botao variante="secundario" onClick={() => setConfirmandoArquivo(false)}>Voltar</Botao>
            <Botao
              disabled={arquivar.isPending}
              onClick={() => arquivar.mutate(categoria.id, { onSuccess: () => setConfirmandoArquivo(false) })}
            >
              Arquivar
            </Botao>
          </div>
        </div>
      ) : (
        <RodapeCartao>
          {categoria.arquivada ? (
            <Botao
              variante="texto"
              Icone={ArchiveRestore}
              className="-ml-3"
              disabled={desarquivar.isPending}
              onClick={() => desarquivar.mutate(categoria.id)}
            >
              Desarquivar
            </Botao>
          ) : (
            <>
              <Botao variante="texto" Icone={Pencil} className="-ml-3" onClick={() => { editar.reset(); setEditando(true) }}>
                Editar
              </Botao>
              <BotaoIcone Icone={Archive} aria-label={`Arquivar ${categoria.nome}`} onClick={() => setConfirmandoArquivo(true)} />
            </>
          )}
        </RodapeCartao>
      )}
    </li>
  )
}
