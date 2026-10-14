import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { definirTokenAcesso } from '../api/cliente'
import { respostaJson, simularApi } from '../testes/api'
import { renderizarAplicacao } from '../testes/renderizar'

const usuario = { id: '1', nome: 'Ana Souza', email: 'ana@email.com' }

describe('Entrar', () => {
  beforeEach(() => {
    definirTokenAcesso(null)
  })

  it('deve redirecionar para a tela de entrar quando não houver sessão', async () => {
    // Arrange
    simularApi({ '/api/auth/renovar': () => respostaJson(401, {}) })

    // Act
    const router = renderizarAplicacao('/transacoes')

    // Assert
    expect(await screen.findByRole('heading', { name: 'Bem-vindo de volta' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/entrar')
  })

  it('deve restaurar a sessão pelo cookie de renovação sem pedir login', async () => {
    // Arrange
    simularApi({
      '/api/auth/renovar': () => respostaJson(200, { tokenAcesso: 'token', tipo: 'Bearer', expiraEmSegundos: 900 }),
      '/api/usuarios/me': () => respostaJson(200, usuario),
    })

    // Act
    renderizarAplicacao('/')

    // Assert
    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByText('Ana Souza')).toBeInTheDocument()
  })

  it('deve validar os campos antes de enviar', async () => {
    // Arrange
    simularApi({ '/api/auth/renovar': () => respostaJson(401, {}) })
    renderizarAplicacao('/entrar')
    const usuarioEvento = userEvent.setup()
    await screen.findByRole('heading', { name: 'Bem-vindo de volta' })

    // Act
    await usuarioEvento.click(screen.getByRole('button', { name: 'Entrar' }))

    // Assert
    expect(await screen.findByText('E-mail inválido')).toBeInTheDocument()
    expect(screen.getByText('Senha é obrigatória')).toBeInTheDocument()
  })

  it('deve exigir senha de 8 caracteres ao criar conta', async () => {
    // Arrange
    simularApi({ '/api/auth/renovar': () => respostaJson(401, {}) })
    renderizarAplicacao('/entrar')
    const usuarioEvento = userEvent.setup()
    await usuarioEvento.click(await screen.findByRole('tab', { name: 'Criar conta' }))

    // Act
    await usuarioEvento.type(screen.getByLabelText('Nome'), 'Ana')
    await usuarioEvento.type(screen.getByLabelText('E-mail'), 'ana@email.com')
    await usuarioEvento.type(screen.getByLabelText('Senha'), '123456')
    await usuarioEvento.click(screen.getByRole('button', { name: 'Criar conta e entrar' }))

    // Assert
    expect(await screen.findByText('Senha deve ter entre 8 e 72 caracteres')).toBeInTheDocument()
  })

  it('deve entrar e abrir o dashboard quando as credenciais forem válidas', async () => {
    // Arrange
    simularApi({
      '/api/auth/renovar': () => respostaJson(401, {}),
      '/api/auth/login': () => respostaJson(200, { tokenAcesso: 'token', tipo: 'Bearer', expiraEmSegundos: 900 }),
      '/api/usuarios/me': () => respostaJson(200, usuario),
    })
    const router = renderizarAplicacao('/entrar')
    const usuarioEvento = userEvent.setup()
    await screen.findByRole('heading', { name: 'Bem-vindo de volta' })

    // Act
    await usuarioEvento.type(screen.getByLabelText('E-mail'), 'ana@email.com')
    await usuarioEvento.type(screen.getByLabelText('Senha'), 'senha-forte')
    await usuarioEvento.click(screen.getByRole('button', { name: 'Entrar' }))

    // Assert
    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
    await waitFor(() => expect(router.state.location.pathname).toBe('/'))
  })

  it('deve mostrar a mensagem da API quando as credenciais forem inválidas', async () => {
    // Arrange
    simularApi({
      '/api/auth/renovar': () => respostaJson(401, {}),
      '/api/auth/login': () => respostaJson(401, { title: 'Não autenticado', detail: 'Credenciais inválidas' }),
    })
    renderizarAplicacao('/entrar')
    const usuarioEvento = userEvent.setup()
    await screen.findByRole('heading', { name: 'Bem-vindo de volta' })

    // Act
    await usuarioEvento.type(screen.getByLabelText('E-mail'), 'ana@email.com')
    await usuarioEvento.type(screen.getByLabelText('Senha'), 'errada')
    await usuarioEvento.click(screen.getByRole('button', { name: 'Entrar' }))

    // Assert
    expect(await screen.findByRole('alert')).toHaveTextContent('Credenciais inválidas')
  })

  it('deve encerrar a sessão ao clicar em sair', async () => {
    // Arrange
    simularApi({
      '/api/auth/renovar': () => respostaJson(200, { tokenAcesso: 'token', tipo: 'Bearer', expiraEmSegundos: 900 }),
      '/api/usuarios/me': () => respostaJson(200, usuario),
      '/api/auth/logout': () => respostaJson(204),
    })
    const router = renderizarAplicacao('/')
    const usuarioEvento = userEvent.setup()

    // Act
    await usuarioEvento.click(await screen.findByRole('button', { name: 'Sair' }))

    // Assert
    expect(await screen.findByRole('heading', { name: 'Bem-vindo de volta' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/entrar')
  })
})
