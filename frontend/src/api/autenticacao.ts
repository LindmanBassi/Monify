import { definirTokenAcesso, requisitar } from './cliente'

export type Usuario = {
  id: string
  nome: string
  email: string
}

type TokenAcessoResponse = {
  tokenAcesso: string
  tipo: string
  expiraEmSegundos: number
}

export async function entrar(email: string, senha: string): Promise<void> {
  const resposta = await requisitar<TokenAcessoResponse>('/api/auth/login', {
    metodo: 'POST',
    corpo: { email, senha },
    semRenovacao: true,
  })
  definirTokenAcesso(resposta.tokenAcesso)
}

export function cadastrar(nome: string, email: string, senha: string): Promise<Usuario> {
  return requisitar<Usuario>('/api/auth/cadastro', {
    metodo: 'POST',
    corpo: { nome, email, senha },
    semRenovacao: true,
  })
}

export async function sair(): Promise<void> {
  await requisitar<void>('/api/auth/logout', { metodo: 'POST', semRenovacao: true })
  definirTokenAcesso(null)
}

export function buscarUsuarioAtual(): Promise<Usuario> {
  return requisitar<Usuario>('/api/usuarios/me')
}
