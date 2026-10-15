import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { Categoria } from '../../api/categorias'
import { definirTokenAcesso } from '../../api/cliente'
import type { MetaEconomia } from '../../api/metasEconomia'
import type { Aporte, MetaPoupanca } from '../../api/metasPoupanca'
import { respostaJson, rotasDeSessao, simularApi, type RequisicaoSimulada } from '../../testes/api'
import { renderizarAplicacao } from '../../testes/renderizar'

const categorias: Categoria[] = [
  { id: 'c1', nome: 'Alimentação', tipo: 'DESPESA', cor: '#F97316', icone: 'ETIQUETA', arquivada: false },
  { id: 'c2', nome: 'Transporte', tipo: 'DESPESA', cor: '#3B82F6', icone: 'ETIQUETA', arquivada: false },
  { id: 'c3', nome: 'Lazer', tipo: 'DESPESA', cor: '#EC4899', icone: 'ETIQUETA', arquivada: false },
  { id: 'c4', nome: 'Saúde', tipo: 'DESPESA', cor: '#EF4444', icone: 'ETIQUETA', arquivada: false },
  { id: 'c5', nome: 'Salário', tipo: 'RECEITA', cor: '#22C55E', icone: 'ETIQUETA', arquivada: false },
]

function resumo(categoria: Categoria) {
  return { id: categoria.id, nome: categoria.nome, cor: categoria.cor, icone: categoria.icone, arquivada: categoria.arquivada }
}

const metaBase = { orcamentoId: null, congeladaEm: '2026-10-01T03:00:00Z', canceladaEm: null, mesReferencia: '2026-10' }

const metasEconomia: MetaEconomia[] = [
  { ...metaBase, id: 'e1', categoria: resumo(categorias[0]), tipoReferencia: 'MES_ANTERIOR', valorEconomia: 300, valorReferencia: 1600, valorLimite: 1300, gasto: 520, economiaRealizada: 1080, status: 'EM_ANDAMENTO' },
  { ...metaBase, id: 'e2', categoria: resumo(categorias[1]), tipoReferencia: 'ORCAMENTO', valorEconomia: 50, valorReferencia: 250, valorLimite: 200, gasto: 280, economiaRealizada: -30, status: 'NAO_ATINGIDA' },
]

const metaAguardando: MetaEconomia = {
  ...metaBase, id: 'e3', mesReferencia: '2026-11', congeladaEm: null, categoria: resumo(categorias[2]), tipoReferencia: 'MEDIA_TRES_MESES',
  valorEconomia: 100, valorReferencia: null, valorLimite: null, gasto: 0, economiaRealizada: null, status: 'AGUARDANDO_INICIO',
}

const poupancaBase = { prazo: null, valorPorMes: null, prazoVencido: false }

const metasPoupanca: MetaPoupanca[] = [
  { ...poupancaBase, id: 'p1', nome: 'Viagem ao Japão', valorAlvo: 12000, prazo: '2027-12-31', status: 'ATIVA', acumulado: 4800, percentual: 40, faltante: 7200, valorPorMes: 480 },
  { ...poupancaBase, id: 'p2', nome: 'Reserva de emergência', valorAlvo: 15000, status: 'CONCLUIDA', acumulado: 15000, percentual: 100, faltante: 0 },
  { ...poupancaBase, id: 'p3', nome: 'Notebook novo', valorAlvo: 6000, status: 'CANCELADA', acumulado: 1500, percentual: 25, faltante: 4500 },
]

const aportes: Aporte[] = [
  { id: 'a2', valor: -200, data: '2026-10-10', observacao: 'Ajuste' },
  { id: 'a1', valor: 5000, data: '2026-09-05', observacao: null },
]

type Opcoes = { aoCriarMetaEconomia?: () => Response }

function simularMetas({ aoCriarMetaEconomia }: Opcoes = {}) {
  const chamadas: RequisicaoSimulada[] = []
  const registrar = (requisicao: RequisicaoSimulada) => {
    chamadas.push(requisicao)
    return respostaJson(requisicao.metodo === 'POST' ? 201 : 204, requisicao.metodo === 'POST' ? {} : undefined)
  }
  simularApi({
    ...rotasDeSessao(),
    '/api/categorias': () => respostaJson(200, categorias),
    '/api/metas-economia': (requisicao) => {
      if (requisicao.metodo === 'GET') {
        return respostaJson(200, requisicao.parametros.get('mes') === '2026-11' ? [metaAguardando] : metasEconomia)
      }
      if (aoCriarMetaEconomia) {
        chamadas.push(requisicao)
        return aoCriarMetaEconomia()
      }
      return registrar(requisicao)
    },
    '/api/metas-economia/e1/cancelar': registrar,
    '/api/metas-economia/e3': registrar,
    '/api/metas-poupanca': (requisicao) => (requisicao.metodo === 'GET' ? respostaJson(200, metasPoupanca) : registrar(requisicao)),
    '/api/metas-poupanca/p1/aportes': (requisicao) =>
      requisicao.metodo === 'GET'
        ? respostaJson(200, { conteudo: aportes, pagina: 0, tamanho: 10, totalElementos: 2, totalPaginas: 1 })
        : registrar(requisicao),
    '/api/metas-poupanca/p2/aportes': registrar,
  })
  return chamadas
}

function cartaoDe(titulo: string) {
  return within(screen.getByRole('heading', { name: titulo }).closest('article')!)
}

describe('Metas', () => {
  beforeEach(() => {
    definirTokenAcesso(null)
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-10-14T15:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('metas de economia', () => {
    it('deve mostrar status, limite congelado e resumo de cada meta', async () => {
      // Arrange
      simularMetas()

      // Act
      renderizarAplicacao('/metas')

      // Assert
      expect(await screen.findByRole('heading', { name: 'Metas de economia · outubro' })).toBeInTheDocument()
      await screen.findByRole('heading', { name: 'Alimentação' })
      const alimentacao = cartaoDe('Alimentação')
      expect(alimentacao.getByText('Em andamento')).toBeInTheDocument()
      expect(alimentacao.getByText(/gasto do mês anterior \(R\$\s1\.600,00\) · economizar R\$\s300,00/)).toBeInTheDocument()
      expect(alimentacao.getByText(/Restam R\$\s780,00 até o limite\./)).toBeInTheDocument()
      const transporte = cartaoDe('Transporte')
      expect(transporte.getByText('Não atingida')).toBeInTheDocument()
      expect(transporte.getByText(/Passou R\$\s80,00 do limite\. Resultado definitivo\./)).toBeInTheDocument()
      expect(transporte.queryByRole('button', { name: 'Cancelar' })).not.toBeInTheDocument()
    })

    it('deve criar meta para categoria sem meta ativa no mês com a referência escolhida', async () => {
      // Arrange
      const chamadas = simularMetas()
      renderizarAplicacao('/metas')
      const usuario = userEvent.setup()
      await usuario.click(await screen.findByRole('button', { name: 'Nova meta de economia' }))
      const formulario = within(screen.getByRole('form', { name: 'Nova meta de economia · outubro de 2026' }))

      // Act
      const opcoes = within(formulario.getByLabelText('Categoria')).getAllByRole('option').map((opcao) => opcao.textContent)
      await usuario.selectOptions(formulario.getByLabelText('Categoria'), 'Lazer')
      await usuario.type(formulario.getByLabelText('Quanto economizar'), '150')
      await usuario.click(formulario.getByRole('radio', { name: /Média dos últimos 3 meses/ }))
      await usuario.click(formulario.getByRole('button', { name: 'Salvar' }))

      // Assert
      expect(opcoes).toEqual(['Selecione', 'Lazer', 'Saúde'])
      await waitFor(() => expect(chamadas).toHaveLength(1))
      expect(chamadas[0].corpo).toEqual({
        categoriaId: 'c3',
        mesReferencia: '2026-10',
        tipoReferencia: 'MEDIA_TRES_MESES',
        valorEconomia: 150,
      })
    })

    it('deve mostrar a mensagem da API quando a referência estiver indisponível', async () => {
      // Arrange
      simularMetas({
        aoCriarMetaEconomia: () =>
          respostaJson(422, { title: 'Regra de negócio violada', detail: 'Histórico insuficiente para a referência escolhida' }),
      })
      renderizarAplicacao('/metas')
      const usuario = userEvent.setup()
      await usuario.click(await screen.findByRole('button', { name: 'Nova meta de economia' }))
      const formulario = within(screen.getByRole('form', { name: 'Nova meta de economia · outubro de 2026' }))

      // Act
      await usuario.selectOptions(formulario.getByLabelText('Categoria'), 'Saúde')
      await usuario.type(formulario.getByLabelText('Quanto economizar'), '50')
      await usuario.click(formulario.getByRole('button', { name: 'Salvar' }))

      // Assert
      expect(await formulario.findByRole('alert')).toHaveTextContent('Histórico insuficiente para a referência escolhida')
    })

    it('deve permitir editar só a meta que ainda não começou', async () => {
      // Arrange
      const chamadas = simularMetas()
      renderizarAplicacao('/metas')
      const usuario = userEvent.setup()
      await screen.findByRole('heading', { name: 'Alimentação' })
      expect(cartaoDe('Alimentação').queryByRole('button', { name: 'Editar' })).not.toBeInTheDocument()

      // Act
      await usuario.click(screen.getByRole('button', { name: 'Próximo mês' }))
      await screen.findByRole('heading', { name: 'Lazer' })
      await usuario.click(cartaoDe('Lazer').getByRole('button', { name: 'Editar' }))
      const formulario = within(screen.getByRole('form', { name: 'Editar meta de Lazer' }))
      await usuario.clear(formulario.getByLabelText('Quanto economizar'))
      await usuario.type(formulario.getByLabelText('Quanto economizar'), '120')
      await usuario.click(formulario.getByRole('button', { name: 'Salvar' }))

      // Assert
      await waitFor(() => expect(chamadas).toHaveLength(1))
      expect(chamadas[0]).toMatchObject({ metodo: 'PUT', corpo: { tipoReferencia: 'MEDIA_TRES_MESES', valorEconomia: 120 } })
    })

    it('deve cancelar uma meta pendente após confirmação', async () => {
      // Arrange
      const chamadas = simularMetas()
      renderizarAplicacao('/metas')
      const usuario = userEvent.setup()
      await screen.findByRole('heading', { name: 'Alimentação' })

      // Act
      await usuario.click(cartaoDe('Alimentação').getByRole('button', { name: 'Cancelar' }))
      await usuario.click(cartaoDe('Alimentação').getByRole('button', { name: 'Cancelar meta' }))

      // Assert
      await waitFor(() => expect(chamadas).toHaveLength(1))
      expect(chamadas[0].metodo).toBe('PATCH')
    })
  })

  describe('metas de poupança', () => {
    it('deve mostrar o acompanhamento e esconder as canceladas por padrão', async () => {
      // Arrange
      simularMetas()
      renderizarAplicacao('/metas')
      const usuario = userEvent.setup()

      // Act
      await screen.findByRole('heading', { name: 'Viagem ao Japão' })
      const canceladaAntes = screen.queryByRole('heading', { name: 'Notebook novo' })
      await usuario.click(screen.getByRole('checkbox', { name: 'Mostrar canceladas' }))

      // Assert
      expect(cartaoDe('Viagem ao Japão').getByText(/Prazo: 31\/12\/2027 · guarde R\$\s480,00 por mês/)).toBeInTheDocument()
      expect(cartaoDe('Reserva de emergência').getByText('Objetivo atingido · aceita apenas resgates')).toBeInTheDocument()
      expect(cartaoDe('Reserva de emergência').getByRole('button', { name: 'Registrar resgate' })).toBeInTheDocument()
      expect(canceladaAntes).not.toBeInTheDocument()
      expect(cartaoDe('Notebook novo').queryByRole('button', { name: /Registrar/ })).not.toBeInTheDocument()
    })

    it('deve registrar resgate como valor negativo e barrar resgate maior que o acumulado', async () => {
      // Arrange
      const chamadas = simularMetas()
      renderizarAplicacao('/metas')
      const usuario = userEvent.setup()
      await screen.findByRole('heading', { name: 'Viagem ao Japão' })
      await usuario.click(cartaoDe('Viagem ao Japão').getByRole('button', { name: 'Registrar aporte' }))
      const cartao = cartaoDe('Viagem ao Japão')
      await usuario.click(cartao.getByRole('radio', { name: 'Resgate' }))

      // Act
      await usuario.type(cartao.getByLabelText('Valor'), '5000')
      await usuario.click(cartao.getByRole('button', { name: 'Registrar resgate' }))
      const erro = (await cartao.findByText(/O resgate não pode passar do acumulado/)).textContent
      await usuario.clear(cartao.getByLabelText(/^Valor/))
      await usuario.type(cartao.getByLabelText(/^Valor/), '300')
      await usuario.click(cartao.getByRole('button', { name: 'Registrar resgate' }))

      // Assert
      expect(erro).toMatch(/O resgate não pode passar do acumulado \(R\$\s4\.800,00\)/)
      await waitFor(() => expect(chamadas).toHaveLength(1))
      expect(chamadas[0].corpo).toEqual({ valor: -300, data: '2026-10-14', observacao: '' })
    })

    it('deve criar meta de poupança sem prazo', async () => {
      // Arrange
      const chamadas = simularMetas()
      renderizarAplicacao('/metas')
      const usuario = userEvent.setup()
      await usuario.click(await screen.findByRole('button', { name: 'Nova meta de poupança' }))
      const formulario = within(screen.getByRole('form', { name: 'Nova meta de poupança' }))

      // Act
      await usuario.type(formulario.getByLabelText('Nome'), 'Carro')
      await usuario.type(formulario.getByLabelText('Valor alvo'), '30.000,00')
      await usuario.click(formulario.getByRole('button', { name: 'Salvar' }))

      // Assert
      await waitFor(() => expect(chamadas).toHaveLength(1))
      expect(chamadas[0].corpo).toEqual({ nome: 'Carro', valorAlvo: 30000, prazo: null })
    })

    it('deve mostrar o extrato de aportes e resgates', async () => {
      // Arrange
      simularMetas()
      renderizarAplicacao('/metas')
      const usuario = userEvent.setup()

      await screen.findByRole('heading', { name: 'Viagem ao Japão' })

      // Act
      await usuario.click(cartaoDe('Viagem ao Japão').getByRole('button', { name: 'Ver extrato' }))

      // Assert
      const extrato = within(await screen.findByRole('list', { name: 'Extrato de Viagem ao Japão' }))
      const itens = extrato.getAllByRole('listitem').map((item) => item.textContent?.replace(/\s/g, ' '))
      expect(itens).toEqual(['Resgate · 10/10/2026Ajuste− R$ 200,00', 'Aporte · 05/09/2026+ R$ 5.000,00'])
    })
  })
})
