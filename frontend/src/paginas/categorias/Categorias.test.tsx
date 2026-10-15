import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import type { Categoria } from '../../api/categorias'
import { definirTokenAcesso } from '../../api/cliente'
import { respostaJson, rotasDeSessao, simularApi, type RequisicaoSimulada } from '../../testes/api'
import { renderizarAplicacao } from '../../testes/renderizar'

function categoriasIniciais(): Categoria[] {
  return [
    { id: 'c1', nome: 'Alimentação', tipo: 'DESPESA', cor: '#F97316', icone: 'ETIQUETA', arquivada: false },
    { id: 'c2', nome: 'Moradia', tipo: 'DESPESA', cor: '#8B5CF6', icone: 'ETIQUETA', arquivada: false },
    { id: 'c3', nome: 'Viagens', tipo: 'DESPESA', cor: '#3B82F6', icone: 'ETIQUETA', arquivada: true },
    { id: 'c4', nome: 'Salário', tipo: 'RECEITA', cor: '#22C55E', icone: 'ETIQUETA', arquivada: false },
  ]
}

function simularCategorias(categorias: Categoria[], aoCriar?: (requisicao: RequisicaoSimulada) => Response) {
  const chamadas: RequisicaoSimulada[] = []
  const registrar = (requisicao: RequisicaoSimulada) => chamadas.push(requisicao)

  simularApi({
    ...rotasDeSessao(),
    '/api/categorias': (requisicao) => {
      registrar(requisicao)
      if (requisicao.metodo === 'POST') {
        if (aoCriar) {
          return aoCriar(requisicao)
        }
        const nova = { id: 'nova', arquivada: false, ...(requisicao.corpo as Omit<Categoria, 'id' | 'arquivada'>) }
        categorias.push(nova)
        return respostaJson(201, nova)
      }
      return respostaJson(200, categorias)
    },
    '/api/categorias/c2': (requisicao) => {
      registrar(requisicao)
      categorias[1] = { ...categorias[1], arquivada: true }
      return respostaJson(204)
    },
    '/api/categorias/c3/desarquivar': (requisicao) => {
      registrar(requisicao)
      categorias[2] = { ...categorias[2], arquivada: false }
      return respostaJson(204)
    },
  })
  return chamadas
}

function lista() {
  return within(screen.getByRole('list', { name: 'Categorias' }))
}

describe('Categorias', () => {
  beforeEach(() => {
    definirTokenAcesso(null)
  })

  it('deve listar as despesas ativas e mostrar as arquivadas quando pedido', async () => {
    // Arrange
    simularCategorias(categoriasIniciais())
    renderizarAplicacao('/categorias')
    const usuario = userEvent.setup()
    await screen.findByRole('list', { name: 'Categorias' })

    // Act & Assert
    expect(lista().getByText('Alimentação')).toBeInTheDocument()
    expect(lista().queryByText('Viagens')).not.toBeInTheDocument()
    expect(lista().queryByText('Salário')).not.toBeInTheDocument()

    await usuario.click(screen.getByLabelText('Mostrar arquivadas'))
    expect(lista().getByText('Viagens')).toBeInTheDocument()
    expect(lista().getByText('Arquivada')).toBeInTheDocument()

    await usuario.click(screen.getByRole('tab', { name: 'Receitas' }))
    expect(lista().getByText('Salário')).toBeInTheDocument()
  })

  it('deve criar a categoria com o tipo e a cor escolhidos', async () => {
    // Arrange
    const chamadas = simularCategorias(categoriasIniciais())
    renderizarAplicacao('/categorias')
    const usuario = userEvent.setup()
    await usuario.click(await screen.findByRole('button', { name: 'Nova categoria' }))
    const formulario = within(screen.getByRole('form', { name: 'Nova categoria' }))

    // Act
    await usuario.type(formulario.getByLabelText('Nome'), 'Pets')
    await usuario.click(formulario.getByRole('radio', { name: 'Pata' }))
    await usuario.click(formulario.getByRole('radio', { name: 'Cor #8B5CF6' }))
    await usuario.click(formulario.getByRole('button', { name: 'Salvar' }))

    // Assert
    expect(await lista().findByText('Pets')).toBeInTheDocument()
    expect(chamadas.find((c) => c.metodo === 'POST')?.corpo).toEqual({ nome: 'Pets', tipo: 'DESPESA', cor: '#8B5CF6', icone: 'PATA' })
    expect(screen.queryByRole('form', { name: 'Nova categoria' })).not.toBeInTheDocument()
  })

  it('deve mostrar a mensagem da API quando o nome já existir', async () => {
    // Arrange
    simularCategorias(categoriasIniciais(), () =>
      respostaJson(409, { title: 'Conflito', detail: 'Já existe uma categoria de despesa com esse nome' }),
    )
    renderizarAplicacao('/categorias')
    const usuario = userEvent.setup()
    await usuario.click(await screen.findByRole('button', { name: 'Nova categoria' }))

    // Act
    await usuario.type(screen.getByLabelText('Nome'), 'Moradia')
    await usuario.click(screen.getByRole('button', { name: 'Salvar' }))

    // Assert
    expect(await screen.findByRole('alert')).toHaveTextContent('Já existe uma categoria de despesa com esse nome')
  })

  it('deve validar o nome antes de enviar', async () => {
    // Arrange
    const chamadas = simularCategorias(categoriasIniciais())
    renderizarAplicacao('/categorias')
    const usuario = userEvent.setup()
    await usuario.click(await screen.findByRole('button', { name: 'Nova categoria' }))

    // Act
    await usuario.click(screen.getByRole('button', { name: 'Salvar' }))

    // Assert
    expect(await screen.findByText('Nome é obrigatório')).toBeInTheDocument()
    expect(chamadas.some((c) => c.metodo === 'POST')).toBe(false)
  })

  it('deve pedir confirmação antes de arquivar e então arquivar', async () => {
    // Arrange
    const chamadas = simularCategorias(categoriasIniciais())
    renderizarAplicacao('/categorias')
    const usuario = userEvent.setup()
    await screen.findByRole('list', { name: 'Categorias' })
    const moradia = within(lista().getByText('Moradia').closest('li')!)

    // Act
    await usuario.click(moradia.getByRole('button', { name: 'Arquivar Moradia' }))
    expect(moradia.getByText(/metas de economia pendentes dela serão canceladas/)).toBeInTheDocument()
    await usuario.click(moradia.getByRole('button', { name: 'Arquivar' }))

    // Assert
    expect(chamadas.find((c) => c.metodo === 'DELETE')).toBeDefined()
    expect(await screen.findByRole('list', { name: 'Categorias' })).not.toHaveTextContent('Moradia')
  })

  it('deve desarquivar uma categoria arquivada', async () => {
    // Arrange
    const chamadas = simularCategorias(categoriasIniciais())
    renderizarAplicacao('/categorias')
    const usuario = userEvent.setup()
    await usuario.click(await screen.findByLabelText('Mostrar arquivadas'))
    const viagens = within(lista().getByText('Viagens').closest('li')!)

    // Act
    await usuario.click(viagens.getByRole('button', { name: 'Desarquivar' }))

    // Assert
    expect(chamadas.find((c) => c.metodo === 'PATCH')).toBeDefined()
    expect(await lista().findAllByText('Arquivada').catch(() => [])).toHaveLength(0)
  })
})
