import type { CategoriaResumo } from './categorias'
import { requisitar } from './cliente'
import { comParametros } from './parametros'

export type ResumoMensal = {
  mes: string
  receitas: number
  despesas: number
  saldo: number
}

// nulo quando o mês anterior é zero: não há base de comparação
export type VariacaoMensal = {
  receitas: number | null
  despesas: number | null
  saldo: number | null
}

export type TotalCategoria = {
  categoria: CategoriaResumo
  total: number
  percentual: number
}

export type Dashboard = {
  mes: string
  resumo: ResumoMensal
  comparacaoMesAnterior: VariacaoMensal
  gastosPorCategoria: TotalCategoria[]
  receitasPorCategoria: TotalCategoria[]
  evolucaoMensal: ResumoMensal[]
}

export function buscarDashboard(mes: string) {
  return requisitar<Dashboard>(comParametros('/api/dashboard', { mes }))
}
