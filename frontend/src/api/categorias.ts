import { requisitar } from './cliente'
import { comParametros } from './parametros'

export type TipoTransacao = 'RECEITA' | 'DESPESA'

export type Categoria = {
  id: string
  nome: string
  tipo: TipoTransacao
  cor: string
  arquivada: boolean
}

export type CategoriaResumo = Pick<Categoria, 'id' | 'nome' | 'cor' | 'arquivada'>

export function listarCategorias(filtros: { tipo?: TipoTransacao; incluirArquivadas?: boolean } = {}) {
  return requisitar<Categoria[]>(comParametros('/api/categorias', filtros))
}

export function criarCategoria(dados: { nome: string; tipo: TipoTransacao; cor: string }) {
  return requisitar<Categoria>('/api/categorias', { metodo: 'POST', corpo: dados })
}

export function editarCategoria(id: string, dados: { nome: string; cor: string }) {
  return requisitar<void>(`/api/categorias/${id}`, { metodo: 'PUT', corpo: dados })
}

// remover = arquivar: o histórico permanece
export function arquivarCategoria(id: string) {
  return requisitar<void>(`/api/categorias/${id}`, { metodo: 'DELETE' })
}

export function desarquivarCategoria(id: string) {
  return requisitar<void>(`/api/categorias/${id}/desarquivar`, { metodo: 'PATCH' })
}
