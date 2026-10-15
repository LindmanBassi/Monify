import { requisitar } from './cliente'
import { comParametros } from './parametros'

export type TipoTransacao = 'RECEITA' | 'DESPESA'

export type IconeCategoria =
  | 'TALHERES' | 'CARRINHO' | 'CASA' | 'CARRO' | 'CORACAO' | 'CAPELO' | 'CLAQUETE' | 'SACOLA' | 'FORMAS' | 'MALETA'
  | 'BANCO' | 'MOEDAS' | 'PATA' | 'AVIAO' | 'PRESENTE' | 'RECIBO' | 'HALTERE' | 'XICARA' | 'CELULAR' | 'ETIQUETA'

export type Categoria = {
  id: string
  nome: string
  tipo: TipoTransacao
  cor: string
  icone: IconeCategoria
  arquivada: boolean
}

export type CategoriaResumo = Pick<Categoria, 'id' | 'nome' | 'cor' | 'icone' | 'arquivada'>

export function listarCategorias(filtros: { tipo?: TipoTransacao; incluirArquivadas?: boolean } = {}) {
  return requisitar<Categoria[]>(comParametros('/api/categorias', filtros))
}

export function criarCategoria(dados: { nome: string; tipo: TipoTransacao; cor: string; icone: IconeCategoria }) {
  return requisitar<Categoria>('/api/categorias', { metodo: 'POST', corpo: dados })
}

export function editarCategoria(id: string, dados: { nome: string; cor: string; icone: IconeCategoria }) {
  return requisitar<void>(`/api/categorias/${id}`, { metodo: 'PUT', corpo: dados })
}

// remover = arquivar: o histórico permanece
export function arquivarCategoria(id: string) {
  return requisitar<void>(`/api/categorias/${id}`, { metodo: 'DELETE' })
}

export function desarquivarCategoria(id: string) {
  return requisitar<void>(`/api/categorias/${id}/desarquivar`, { metodo: 'PATCH' })
}
