import type { CategoriaResumo, TipoTransacao } from './categorias'
import { requisitar } from './cliente'
import { comParametros } from './parametros'

export type Transacao = {
  id: string
  categoria: CategoriaResumo
  tipo: TipoTransacao
  valor: number
  descricao: string | null
  dataOcorrencia: string
}

export type Pagina<T> = {
  conteudo: T[]
  pagina: number
  tamanho: number
  totalElementos: number
  totalPaginas: number
}

export type FiltrosTransacoes = {
  inicio?: string
  fim?: string
  categoriaId?: string
  tipo?: TipoTransacao
  texto?: string
}

export type DadosTransacao = {
  categoriaId: string
  valor: number
  descricao: string
  dataOcorrencia: string
}

export const TAMANHO_PAGINA = 20

export function listarTransacoes(filtros: FiltrosTransacoes, pagina: number) {
  return requisitar<Pagina<Transacao>>(
    comParametros('/api/transacoes', { ...filtros, page: pagina, size: TAMANHO_PAGINA }),
  )
}

export function criarTransacao(dados: DadosTransacao) {
  return requisitar<Transacao>('/api/transacoes', { metodo: 'POST', corpo: dados })
}

export function editarTransacao(id: string, dados: DadosTransacao) {
  return requisitar<void>(`/api/transacoes/${id}`, { metodo: 'PUT', corpo: dados })
}

export function excluirTransacao(id: string) {
  return requisitar<void>(`/api/transacoes/${id}`, { metodo: 'DELETE' })
}
