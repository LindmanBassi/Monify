import { requisitar } from './cliente'
import { comParametros } from './parametros'
import type { Pagina } from './transacoes'

export type StatusMetaPoupanca = 'ATIVA' | 'CONCLUIDA' | 'CANCELADA'

export type MetaPoupanca = {
  id: string
  nome: string
  valorAlvo: number
  prazo: string | null
  status: StatusMetaPoupanca
  acumulado: number
  percentual: number
  faltante: number
  // nulo sem prazo, com a meta não ativa ou com o prazo vencido
  valorPorMes: number | null
  prazoVencido: boolean
}

export type DadosMetaPoupanca = {
  nome: string
  valorAlvo: number
  prazo: string | null
}

// valor positivo = aporte, negativo = resgate
export type Aporte = {
  id: string
  valor: number
  data: string
  observacao: string | null
}

export const TAMANHO_EXTRATO = 10

export function listarMetasPoupanca() {
  return requisitar<MetaPoupanca[]>('/api/metas-poupanca')
}

export function criarMetaPoupanca(dados: DadosMetaPoupanca) {
  return requisitar<MetaPoupanca>('/api/metas-poupanca', { metodo: 'POST', corpo: dados })
}

export function editarMetaPoupanca(id: string, dados: DadosMetaPoupanca) {
  return requisitar<void>(`/api/metas-poupanca/${id}`, { metodo: 'PUT', corpo: dados })
}

export function cancelarMetaPoupanca(id: string) {
  return requisitar<void>(`/api/metas-poupanca/${id}/cancelar`, { metodo: 'PATCH' })
}

export function registrarAporte(id: string, dados: { valor: number; data: string; observacao: string }) {
  return requisitar<Aporte>(`/api/metas-poupanca/${id}/aportes`, { metodo: 'POST', corpo: dados })
}

export function listarAportes(id: string, pagina: number) {
  return requisitar<Pagina<Aporte>>(
    comParametros(`/api/metas-poupanca/${id}/aportes`, { page: pagina, size: TAMANHO_EXTRATO }),
  )
}
