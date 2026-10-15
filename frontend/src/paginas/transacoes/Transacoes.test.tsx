import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import type { Categoria } from '../../api/categorias'
import { definirTokenAcesso } from '../../api/cliente'
import type { Transacao } from '../../api/transacoes'
import { hojeEmSaoPaulo } from '../../lib/formatacao'
import { respostaJson, rotasDeSessao, simularApi, type RequisicaoSimulada } from '../../testes/api'
import { renderizarAplicacao } from '../../testes/renderizar'

const categorias: Categoria[] = [
  { id: 'c1', nome: 'Alimentação', tipo: 'DESPESA', cor: '#F97316', icone: 'ETIQUETA', arquivada: false },
  { id: 'c2', nome: 'Viagens', tipo: 'DESPESA', cor: '#3B82F6', icone: 'ETIQUETA', arquivada: true },
  { id: 'c3', nome: 'Salário', tipo: 'RECEITA', cor: '#22C55E', icone: 'ETIQUETA', arquivada: false },
]

function transacoesIniciais(): Transacao[] {
  return [
    { id: 't1', categoria: { id: 'c1', nome: 'Alimentação', cor: '#F97316', icone: 'ETIQUETA', arquivada: false }, tipo: 'DESPESA', valor: 320, descricao: 'Mercado do mês', dataOcorrencia: '2026-10-14' },
    { id: 't2', categoria: { id: 'c3', nome: 'Salário', cor: '#22C55E', icone: 'ETIQUETA', arquivada: false }, tipo: 'RECEITA', valor: 5000, descricao: 'Salário de outubro', dataOcorrencia: '2026-10-05' },
    { id: 't3', categoria: { id: 'c2', nome: 'Viagens', cor: '#3B82F6', icone: 'ETIQUETA', arquivada: true }, tipo: 'DESPESA', valor: 1200, descricao: 'Passagem', dataOcorrencia: '2026-09-20' },
  ]
}

function simularTransacoes(transacoes: Transacao[]) {
  const chamadas: RequisicaoSimulada[] = []
  const pagina = () => ({ conteudo: transacoes, pagina: 0, tamanho: 20, totalElementos: transacoes.length, totalPaginas: 1 })

  simularApi({
    ...rotasDeSessao(),
    '/api/categorias': () => respostaJson(200, categorias),
    '/api/transacoes': (requisicao) => {
      chamadas.push(requisicao)
      if (requisicao.metodo === 'POST') {
        return respostaJson(201, { id: 'nova' })
      }
      return respostaJson(200, pagina())
    },
    '/api/transacoes/t1': (requisicao) => {
      chamadas.push(requisicao)
      transacoes.splice(0, 1)
      return respostaJson(204)
    },
    '/api/transacoes/t3': (requisicao) => {
      chamadas.push(requisicao)
      return respostaJson(204)
    },
  })
  return chamadas
}

function linhaDe(descricao: string) {
  return within(screen.getByText(descricao).closest('tr')!)
}

describe('Transações', () => {
  beforeEach(() => {
    definirTokenAcesso(null)
  })

  it('deve listar as transações com valores e datas formatados', async () => {
    // Arrange
    simularTransacoes(transacoesIniciais())

    // Act
    renderizarAplicacao('/transacoes')

    // Assert
    expect(await screen.findByText('Mercado do mês')).toBeInTheDocument()
    expect(linhaDe('Mercado do mês').getByText('14/10/2026')).toBeInTheDocument()
    expect(linhaDe('Mercado do mês').getByText(/− R\$\s320,00/)).toBeInTheDocument()
    expect(linhaDe('Salário de outubro').getByText(/\+ R\$\s5\.000,00/)).toBeInTheDocument()
    expect(screen.getByText('3 transações')).toBeInTheDocument()
  })

  it('deve enviar os filtros escolhidos para a API', async () => {
    // Arrange
    const chamadas = simularTransacoes(transacoesIniciais())
    renderizarAplicacao('/transacoes')
    const usuario = userEvent.setup()
    await screen.findByText('Mercado do mês')

    // Act
    await usuario.selectOptions(screen.getByLabelText('Tipo'), 'DESPESA')
    await usuario.type(screen.getByLabelText('Buscar na descrição'), 'mercado')

    // Assert
    await waitFor(() => {
      const ultima = chamadas.filter((c) => c.metodo === 'GET').at(-1)!
      expect(ultima.parametros.get('tipo')).toBe('DESPESA')
      expect(ultima.parametros.get('texto')).toBe('mercado')
      expect(ultima.parametros.get('page')).toBe('0')
    })
  })

  it('deve avisar e não buscar quando a data inicial for depois da final', async () => {
    // Arrange
    const chamadas = simularTransacoes(transacoesIniciais())
    renderizarAplicacao('/transacoes')
    const usuario = userEvent.setup()
    await screen.findByText('Mercado do mês')

    // Act
    await usuario.type(screen.getByLabelText('De'), '2026-10-20')
    await usuario.type(screen.getByLabelText(/Até/), '2026-10-01')

    // Assert
    expect(await screen.findByText('A data inicial não pode ser depois da final')).toBeInTheDocument()
    expect(chamadas.some((c) => c.parametros.get('inicio') === '2026-10-20' && c.parametros.get('fim') === '2026-10-01')).toBe(false)
  })

  it('deve registrar a transação convertendo o valor digitado', async () => {
    // Arrange
    const chamadas = simularTransacoes(transacoesIniciais())
    renderizarAplicacao('/transacoes')
    const usuario = userEvent.setup()
    await usuario.click(await screen.findByRole('button', { name: 'Nova transação' }))
    const formulario = within(screen.getByRole('form', { name: 'Nova transação' }))

    // Act
    await usuario.selectOptions(formulario.getByLabelText('Categoria'), 'c1')
    await usuario.type(formulario.getByLabelText('Valor'), '1.234,56')
    await usuario.type(formulario.getByLabelText('Descrição (opcional)'), 'Compra grande')
    await usuario.click(formulario.getByRole('button', { name: 'Salvar' }))

    // Assert
    await waitFor(() => expect(screen.queryByRole('form', { name: 'Nova transação' })).not.toBeInTheDocument())
    expect(chamadas.find((c) => c.metodo === 'POST')?.corpo).toEqual({
      categoriaId: 'c1',
      valor: 1234.56,
      descricao: 'Compra grande',
      dataOcorrencia: hojeEmSaoPaulo(),
    })
  })

  it('deve validar valor, categoria e data futura antes de enviar', async () => {
    // Arrange
    const chamadas = simularTransacoes(transacoesIniciais())
    renderizarAplicacao('/transacoes')
    const usuario = userEvent.setup()
    await usuario.click(await screen.findByRole('button', { name: 'Nova transação' }))
    const formulario = within(screen.getByRole('form', { name: 'Nova transação' }))

    // Act
    await usuario.type(formulario.getByLabelText('Valor'), '10,999')
    await usuario.clear(formulario.getByLabelText('Data'))
    await usuario.type(formulario.getByLabelText('Data'), '2999-01-01')
    await usuario.click(formulario.getByRole('button', { name: 'Salvar' }))

    // Assert
    expect(await formulario.findByText('Categoria é obrigatória')).toBeInTheDocument()
    expect(formulario.getByText('Informe um valor maior que zero, com até 2 casas decimais')).toBeInTheDocument()
    expect(formulario.getByText('Data da transação não pode ser futura')).toBeInTheDocument()
    expect(chamadas.some((c) => c.metodo === 'POST')).toBe(false)
  })

  it('deve oferecer só categorias ativas na criação, mas manter a arquivada ao editar', async () => {
    // Arrange
    const chamadas = simularTransacoes(transacoesIniciais())
    renderizarAplicacao('/transacoes')
    const usuario = userEvent.setup()
    await usuario.click(await screen.findByRole('button', { name: 'Nova transação' }))
    expect(within(screen.getByRole('form', { name: 'Nova transação' })).queryByRole('option', { name: 'Viagens' })).not.toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Cancelar' }))

    // Act
    await usuario.click(linhaDe('Passagem').getByRole('button', { name: 'Editar Passagem' }))
    const formulario = within(screen.getByRole('form', { name: 'Editar transação' }))
    expect(formulario.getByRole('option', { name: 'Viagens (arquivada)' })).toBeInTheDocument()
    await usuario.clear(formulario.getByLabelText('Valor'))
    await usuario.type(formulario.getByLabelText('Valor'), '1300')
    await usuario.click(formulario.getByRole('button', { name: 'Salvar' }))

    // Assert
    await waitFor(() => expect(chamadas.find((c) => c.metodo === 'PUT')?.corpo).toMatchObject({ categoriaId: 'c2', valor: 1300 }))
  })

  it('deve pedir confirmação antes de excluir', async () => {
    // Arrange
    const chamadas = simularTransacoes(transacoesIniciais())
    renderizarAplicacao('/transacoes')
    const usuario = userEvent.setup()
    await screen.findByText('Mercado do mês')

    // Act
    await usuario.click(linhaDe('Mercado do mês').getByRole('button', { name: 'Excluir Mercado do mês' }))
    await usuario.click(linhaDe('Mercado do mês').getByRole('button', { name: 'Excluir' }))

    // Assert
    await waitFor(() => expect(screen.queryByText('Mercado do mês')).not.toBeInTheDocument())
    expect(chamadas.find((c) => c.metodo === 'DELETE')).toBeDefined()
  })
})
