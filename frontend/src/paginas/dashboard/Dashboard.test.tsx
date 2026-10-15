import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { definirTokenAcesso } from '../../api/cliente'
import type { CategoriaResumo } from '../../api/categorias'
import type { Dashboard } from '../../api/dashboard'
import type { Orcamento } from '../../api/orcamentos'
import { respostaJson, rotasDeSessao, simularApi, type RequisicaoSimulada } from '../../testes/api'
import { renderizarAplicacao } from '../../testes/renderizar'

const moradia: CategoriaResumo = { id: 'c1', nome: 'Moradia', cor: '#8B5CF6', icone: 'ETIQUETA', arquivada: false }
const alimentacao: CategoriaResumo = { id: 'c2', nome: 'Alimentação', cor: '#F97316', icone: 'ETIQUETA', arquivada: false }
const transporte: CategoriaResumo = { id: 'c3', nome: 'Transporte', cor: '#3B82F6', icone: 'ETIQUETA', arquivada: false }
const salario: CategoriaResumo = { id: 'c4', nome: 'Salário', cor: '#22C55E', icone: 'ETIQUETA', arquivada: false }

function dashboardDe(mes: string): Dashboard {
  return {
    mes,
    resumo: { mes, receitas: 6000, despesas: 2000, saldo: 4000 },
    comparacaoMesAnterior: { receitas: 50, despesas: -20, saldo: null },
    gastosPorCategoria: [
      { categoria: moradia, total: 1100, percentual: 55 },
      { categoria: alimentacao, total: 900, percentual: 45 },
    ],
    receitasPorCategoria: [{ categoria: salario, total: 6000, percentual: 100 }],
    evolucaoMensal: [
      { mes: '2026-09', receitas: 4000, despesas: 2500, saldo: 1500 },
      { mes, receitas: 6000, despesas: 2000, saldo: 4000 },
    ],
  }
}

const orcamentos: Orcamento[] = [
  { id: 'o1', categoria: alimentacao, mesReferencia: '2026-10', valorLimite: 1000, gasto: 900, restante: 100, percentual: 90, status: 'PROXIMO_DO_LIMITE' },
  { id: 'o2', categoria: moradia, mesReferencia: '2026-10', valorLimite: 2000, gasto: 1100, restante: 900, percentual: 55, status: 'DENTRO_DO_LIMITE' },
  { id: 'o3', categoria: transporte, mesReferencia: '2026-10', valorLimite: 250, gasto: 280, restante: -30, percentual: 112, status: 'ESTOURADO' },
]

function simularDashboard(orcamentosDoMes: Orcamento[] = orcamentos) {
  const chamadas: RequisicaoSimulada[] = []
  simularApi({
    ...rotasDeSessao(),
    '/api/dashboard': (requisicao) => {
      chamadas.push(requisicao)
      return respostaJson(200, dashboardDe(requisicao.parametros.get('mes') ?? ''))
    },
    '/api/orcamentos': () => respostaJson(200, orcamentosDoMes),
  })
  return chamadas
}

describe('Dashboard', () => {
  beforeEach(() => {
    definirTokenAcesso(null)
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-10-14T15:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('deve mostrar o resumo do mês com a comparação com o mês anterior', async () => {
    // Arrange
    simularDashboard()

    // Act
    renderizarAplicacao('/')

    // Assert
    expect(await screen.findByRole('heading', { name: 'Olá, Ana' })).toBeInTheDocument()
    const receitas = await screen.findByRole('group', { name: 'Receitas do mês' })
    expect(receitas).toHaveTextContent(/R\$\s6\.000,00/)
    expect(within(receitas).getByText('▲ 50% em relação a setembro')).toBeInTheDocument()
    expect(within(screen.getByRole('group', { name: 'Despesas do mês' })).getByText('▼ 20% em relação a setembro')).toBeInTheDocument()
    expect(within(screen.getByRole('group', { name: 'Saldo do mês' })).getByText('Receitas menos despesas')).toBeInTheDocument()
  })

  it('deve listar gastos e receitas por categoria e a evolução mensal', async () => {
    // Arrange
    simularDashboard()

    // Act
    renderizarAplicacao('/')

    // Assert
    const gastos = within(await screen.findByRole('list', { name: 'Gastos por categoria' }))
    expect(gastos.getAllByRole('listitem').map((item) => item.textContent?.replace(/\s/g, ' '))).toEqual([
      'MoradiaR$ 1.100,0055%',
      'AlimentaçãoR$ 900,0045%',
    ])
    expect(within(screen.getByRole('list', { name: 'Receitas por categoria' })).getByText('Salário')).toBeInTheDocument()
    const evolucao = within(screen.getByRole('table', { name: 'Receitas, despesas e saldo por mês' }))
    expect(evolucao.getByRole('rowheader', { name: 'setembro de 2026' })).toBeInTheDocument()
    expect(evolucao.getByRole('rowheader', { name: 'outubro de 2026' })).toBeInTheDocument()
  })

  it('deve buscar o mês anterior ao navegar pelos meses', async () => {
    // Arrange
    const chamadas = simularDashboard()
    renderizarAplicacao('/')
    const usuario = userEvent.setup()
    await screen.findByRole('group', { name: 'Receitas do mês' })

    // Act
    await usuario.click(screen.getByRole('button', { name: 'Mês anterior' }))

    // Assert
    await waitFor(() => expect(chamadas.map((chamada) => chamada.parametros.get('mes'))).toEqual(['2026-10', '2026-09']))
  })

  it('deve destacar apenas os orçamentos que pedem atenção, do mais usado ao menos usado', async () => {
    // Arrange
    simularDashboard()

    // Act
    renderizarAplicacao('/')

    // Assert
    const painel = within(await screen.findByRole('region', { name: 'Orçamentos em atenção' }))
    expect(await painel.findByText('Acima do limite · 112%')).toBeInTheDocument()
    expect(painel.getByText('Próximo do limite · 90%')).toBeInTheDocument()
    expect(painel.queryByText('Moradia')).not.toBeInTheDocument()
    const nomes = painel.getAllByText(/^(Transporte|Alimentação)$/).map((elemento) => elemento.textContent)
    expect(nomes).toEqual(['Transporte', 'Alimentação'])
  })

  it('deve avisar quando todos os orçamentos estão dentro do limite', async () => {
    // Arrange
    simularDashboard([orcamentos[1]])

    // Act
    renderizarAplicacao('/')

    // Assert
    expect(await screen.findByText('Todos os orçamentos estão dentro do limite.')).toBeInTheDocument()
  })
})
