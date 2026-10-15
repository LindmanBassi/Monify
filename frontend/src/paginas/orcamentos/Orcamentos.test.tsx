import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { Categoria } from '../../api/categorias'
import { definirTokenAcesso } from '../../api/cliente'
import type { Orcamento } from '../../api/orcamentos'
import { respostaJson, rotasDeSessao, rotasDoDashboardVazio, simularApi, type RequisicaoSimulada } from '../../testes/api'
import { renderizarAplicacao } from '../../testes/renderizar'

const categorias: Categoria[] = [
  { id: 'c1', nome: 'Alimentação', tipo: 'DESPESA', cor: '#F97316', icone: 'ETIQUETA', arquivada: false },
  { id: 'c2', nome: 'Transporte', tipo: 'DESPESA', cor: '#3B82F6', icone: 'ETIQUETA', arquivada: false },
  { id: 'c3', nome: 'Lazer', tipo: 'DESPESA', cor: '#EC4899', icone: 'ETIQUETA', arquivada: false },
  { id: 'c4', nome: 'Viagens', tipo: 'DESPESA', cor: '#14B8A6', icone: 'ETIQUETA', arquivada: true },
  { id: 'c5', nome: 'Salário', tipo: 'RECEITA', cor: '#22C55E', icone: 'ETIQUETA', arquivada: false },
]

function resumo(categoria: Categoria) {
  return { id: categoria.id, nome: categoria.nome, cor: categoria.cor, icone: categoria.icone, arquivada: categoria.arquivada }
}

const orcamentos: Orcamento[] = [
  { id: 'o1', categoria: resumo(categorias[0]), mesReferencia: '2026-10', valorLimite: 800, gasto: 520, restante: 280, percentual: 65, status: 'DENTRO_DO_LIMITE' },
  { id: 'o2', categoria: resumo(categorias[1]), mesReferencia: '2026-10', valorLimite: 250, gasto: 280, restante: -30, percentual: 112, status: 'ESTOURADO' },
]

function simularOrcamentos() {
  const chamadas: RequisicaoSimulada[] = []
  const registrar = (requisicao: RequisicaoSimulada) => {
    chamadas.push(requisicao)
    return respostaJson(requisicao.metodo === 'POST' ? 201 : 204, requisicao.metodo === 'POST' ? orcamentos[0] : undefined)
  }
  simularApi({
    ...rotasDeSessao(),
    ...rotasDoDashboardVazio(),
    '/api/categorias': () => respostaJson(200, categorias),
    '/api/orcamentos': (requisicao) => (requisicao.metodo === 'GET' ? respostaJson(200, orcamentos) : registrar(requisicao)),
    '/api/orcamentos/o1': registrar,
    '/api/orcamentos/o2': registrar,
  })
  return chamadas
}

function cartaoDe(categoria: string) {
  return within(screen.getByRole('heading', { name: categoria }).closest('article')!)
}

describe('Orçamentos', () => {
  beforeEach(() => {
    definirTokenAcesso(null)
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-10-14T15:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('deve listar os orçamentos do mês com gasto, status e restante', async () => {
    // Arrange
    simularOrcamentos()

    // Act
    renderizarAplicacao('/orcamentos')

    // Assert
    expect(await screen.findByRole('heading', { name: 'Orçamentos de outubro' })).toBeInTheDocument()
    await screen.findByRole('heading', { name: 'Alimentação' })
    expect(cartaoDe('Alimentação').getByText('Dentro do limite')).toBeInTheDocument()
    expect(cartaoDe('Alimentação').getByText(/Restam R\$\s280,00/)).toBeInTheDocument()
    expect(cartaoDe('Transporte').getByText('Acima do limite')).toBeInTheDocument()
    expect(cartaoDe('Transporte').getByText(/R\$\s30,00 acima/)).toBeInTheDocument()
  })

  it('deve definir orçamento só para categorias de despesa ativas ainda sem orçamento', async () => {
    // Arrange
    const chamadas = simularOrcamentos()
    renderizarAplicacao('/orcamentos')
    const usuario = userEvent.setup()
    await usuario.click(await screen.findByRole('button', { name: 'Definir orçamento' }))
    const formulario = within(screen.getByRole('form', { name: 'Definir orçamento' }))

    // Act
    const opcoes = within(formulario.getByLabelText('Categoria')).getAllByRole('option').map((opcao) => opcao.textContent)
    await usuario.selectOptions(formulario.getByLabelText('Categoria'), 'Lazer')
    await usuario.type(formulario.getByLabelText('Limite do mês'), '450,50')
    await usuario.click(formulario.getByRole('button', { name: 'Salvar' }))

    // Assert
    expect(opcoes).toEqual(['Selecione', 'Lazer'])
    await waitFor(() => expect(chamadas).toHaveLength(1))
    expect(chamadas[0].corpo).toEqual({ categoriaId: 'c3', mesReferencia: '2026-10', valorLimite: 450.5 })
  })

  it('deve alterar o limite de um orçamento', async () => {
    // Arrange
    const chamadas = simularOrcamentos()
    renderizarAplicacao('/orcamentos')
    const usuario = userEvent.setup()
    await screen.findByRole('heading', { name: 'Alimentação' })

    // Act
    await usuario.click(cartaoDe('Alimentação').getByRole('button', { name: 'Alterar limite' }))
    const campo = cartaoDe('Alimentação').getByLabelText('Novo limite')
    await usuario.clear(campo)
    await usuario.type(campo, '900')
    await usuario.click(cartaoDe('Alimentação').getByRole('button', { name: 'Salvar' }))

    // Assert
    await waitFor(() => expect(chamadas).toHaveLength(1))
    expect(chamadas[0]).toMatchObject({ metodo: 'PUT', corpo: { valorLimite: 900 } })
  })

  it('deve pedir confirmação antes de excluir um orçamento', async () => {
    // Arrange
    const chamadas = simularOrcamentos()
    renderizarAplicacao('/orcamentos')
    const usuario = userEvent.setup()
    await screen.findByRole('heading', { name: 'Transporte' })

    // Act
    await usuario.click(cartaoDe('Transporte').getByRole('button', { name: 'Excluir orçamento de Transporte' }))
    const aviso = cartaoDe('Transporte').getByText('Excluir? As metas de economia pendentes ligadas a ele serão canceladas.')
    await usuario.click(cartaoDe('Transporte').getByRole('button', { name: 'Excluir' }))

    // Assert
    expect(aviso).toBeInTheDocument()
    await waitFor(() => expect(chamadas).toHaveLength(1))
    expect(chamadas[0].metodo).toBe('DELETE')
  })

  it('deve tratar meses passados como histórico, sem edição', async () => {
    // Arrange
    simularOrcamentos()
    renderizarAplicacao('/orcamentos')
    const usuario = userEvent.setup()
    await screen.findByRole('button', { name: 'Definir orçamento' })

    // Act
    await usuario.click(screen.getByRole('button', { name: 'Mês anterior' }))

    // Assert
    expect(await screen.findByRole('heading', { name: 'Orçamentos de setembro' })).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Alimentação' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Definir orçamento' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Alterar limite' })).not.toBeInTheDocument()
  })
})
