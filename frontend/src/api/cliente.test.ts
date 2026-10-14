import { beforeEach, describe, expect, it, vi } from 'vitest'
import { respostaJson, simularApi } from '../testes/api'
import { definirTokenAcesso, ErroApi, registrarAoExpirarSessao, requisitar } from './cliente'

describe('requisitar', () => {
  beforeEach(() => {
    definirTokenAcesso(null)
    registrarAoExpirarSessao(() => {})
  })

  it('deve enviar o token de acesso no cabeçalho Authorization', async () => {
    // Arrange
    definirTokenAcesso('token-atual')
    simularApi({
      '/api/usuarios/me': ({ autorizacao }) => respostaJson(200, { recebido: autorizacao }),
    })

    // Act
    const resposta = await requisitar<{ recebido: string }>('/api/usuarios/me')

    // Assert
    expect(resposta.recebido).toBe('Bearer token-atual')
  })

  it('deve renovar a sessão e repetir a requisição quando receber 401', async () => {
    // Arrange
    definirTokenAcesso('token-expirado')
    const fetchSimulado = simularApi({
      '/api/usuarios/me': ({ autorizacao }) =>
        autorizacao === 'Bearer token-novo' ? respostaJson(200, { nome: 'Ana' }) : respostaJson(401, {}),
      '/api/auth/renovar': () => respostaJson(200, { tokenAcesso: 'token-novo', tipo: 'Bearer', expiraEmSegundos: 900 }),
    })

    // Act
    const usuario = await requisitar<{ nome: string }>('/api/usuarios/me')

    // Assert
    expect(usuario.nome).toBe('Ana')
    expect(fetchSimulado).toHaveBeenCalledTimes(3)
  })

  it('deve compartilhar uma única renovação entre requisições simultâneas', async () => {
    // Arrange
    definirTokenAcesso('token-expirado')
    const renovar = vi.fn(() => respostaJson(200, { tokenAcesso: 'token-novo', tipo: 'Bearer', expiraEmSegundos: 900 }))
    simularApi({
      '/api/categorias': ({ autorizacao }) => (autorizacao === 'Bearer token-novo' ? respostaJson(200, []) : respostaJson(401, {})),
      '/api/auth/renovar': renovar,
    })

    // Act
    await Promise.all([requisitar('/api/categorias'), requisitar('/api/categorias')])

    // Assert
    expect(renovar).toHaveBeenCalledTimes(1)
  })

  it('deve encerrar a sessão quando a renovação falhar', async () => {
    // Arrange
    const aoExpirar = vi.fn()
    registrarAoExpirarSessao(aoExpirar)
    simularApi({
      '/api/usuarios/me': () => respostaJson(401, { title: 'Não autenticado', detail: 'Token de acesso ausente, inválido ou expirado' }),
      '/api/auth/renovar': () => respostaJson(401, { detail: 'Sessão expirada. Faça login novamente' }),
    })

    // Act & Assert
    await expect(requisitar('/api/usuarios/me')).rejects.toMatchObject({ status: 401 })
    expect(aoExpirar).toHaveBeenCalledOnce()
  })

  it('deve converter o ProblemDetail em ErroApi com os campos inválidos', async () => {
    // Arrange
    simularApi({
      '/api/auth/cadastro': () =>
        respostaJson(400, {
          title: 'Parâmetros inválidos',
          detail: 'Existem campos inválidos na requisição',
          'campos-invalidos': [{ campo: 'senha', mensagem: 'Senha deve ter entre 8 e 72 caracteres' }],
        }),
    })

    // Act
    const erro = (await requisitar('/api/auth/cadastro', { metodo: 'POST', corpo: {}, semRenovacao: true }).catch((e: unknown) => e)) as ErroApi

    // Assert
    expect(erro).toBeInstanceOf(ErroApi)
    expect(erro.status).toBe(400)
    expect(erro.camposInvalidos).toEqual([{ campo: 'senha', mensagem: 'Senha deve ter entre 8 e 72 caracteres' }])
  })

  it('não deve tentar renovar quando a rota for de autenticação', async () => {
    // Arrange
    const fetchSimulado = simularApi({
      '/api/auth/login': () => respostaJson(401, { detail: 'Credenciais inválidas' }),
    })

    // Act & Assert
    await expect(requisitar('/api/auth/login', { metodo: 'POST', corpo: {}, semRenovacao: true })).rejects.toThrow(
      'Credenciais inválidas',
    )
    expect(fetchSimulado).toHaveBeenCalledOnce()
  })
})
