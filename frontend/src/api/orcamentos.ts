import type { CategoriaResumo } from './categorias'
import { requisitar } from './cliente'
import { comParametros } from './parametros'

export type StatusOrcamento = 'DENTRO_DO_LIMITE' | 'PROXIMO_DO_LIMITE' | 'ESTOURADO'

export type Orcamento = {
  id: string
  categoria: CategoriaResumo
  mesReferencia: string
  valorLimite: number
  gasto: number
  restante: number
  percentual: number
  status: StatusOrcamento
}

export function listarOrcamentos(mes: string) {
  return requisitar<Orcamento[]>(comParametros('/api/orcamentos', { mes }))
}

// cria ou atualiza o orçamento da categoria no mês
export function definirOrcamento(dados: { categoriaId: string; mesReferencia: string; valorLimite: number }) {
  return requisitar<Orcamento>('/api/orcamentos', { metodo: 'POST', corpo: dados })
}

export function alterarLimite(id: string, valorLimite: number) {
  return requisitar<void>(`/api/orcamentos/${id}`, { metodo: 'PUT', corpo: { valorLimite } })
}

export function excluirOrcamento(id: string) {
  return requisitar<void>(`/api/orcamentos/${id}`, { metodo: 'DELETE' })
}
