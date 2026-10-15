import type { CategoriaResumo } from './categorias'
import { requisitar } from './cliente'
import { comParametros } from './parametros'

export type TipoReferencia = 'ORCAMENTO' | 'MES_ANTERIOR' | 'MEDIA_TRES_MESES'

export type StatusMetaEconomia = 'CANCELADA' | 'AGUARDANDO_INICIO' | 'NAO_ATINGIDA' | 'ATINGIDA' | 'EM_ANDAMENTO'

export type MetaEconomia = {
  id: string
  categoria: CategoriaResumo
  mesReferencia: string
  tipoReferencia: TipoReferencia
  orcamentoId: string | null
  valorEconomia: number
  // nulos até o congelamento, no início do mês da meta
  valorReferencia: number | null
  valorLimite: number | null
  gasto: number
  economiaRealizada: number | null
  status: StatusMetaEconomia
  congeladaEm: string | null
  canceladaEm: string | null
}

export type DadosMetaEconomia = {
  tipoReferencia: TipoReferencia
  valorEconomia: number
}

export function listarMetasEconomia(mes: string) {
  return requisitar<MetaEconomia[]>(comParametros('/api/metas-economia', { mes }))
}

export function criarMetaEconomia(dados: DadosMetaEconomia & { categoriaId: string; mesReferencia: string }) {
  return requisitar<MetaEconomia>('/api/metas-economia', { metodo: 'POST', corpo: dados })
}

export function editarMetaEconomia(id: string, dados: DadosMetaEconomia) {
  return requisitar<void>(`/api/metas-economia/${id}`, { metodo: 'PUT', corpo: dados })
}

export function cancelarMetaEconomia(id: string) {
  return requisitar<void>(`/api/metas-economia/${id}/cancelar`, { metodo: 'PATCH' })
}
