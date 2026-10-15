export type CampoInvalido = {
  campo: string
  mensagem: string
}

type ProblemDetail = {
  title?: string
  detail?: string
  status?: number
  'campos-invalidos'?: CampoInvalido[]
}

export class ErroApi extends Error {
  readonly status: number
  readonly titulo: string
  readonly camposInvalidos: CampoInvalido[]

  constructor(status: number, problema: ProblemDetail) {
    super(problema.detail ?? 'Não foi possível concluir a operação')
    this.name = 'ErroApi'
    this.status = status
    this.titulo = problema.title ?? 'Erro'
    this.camposInvalidos = problema['campos-invalidos'] ?? []
  }
}

type Opcoes = {
  metodo?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  corpo?: unknown
  semRenovacao?: boolean
}

type TokenAcessoResponse = {
  tokenAcesso: string
  tipo: string
  expiraEmSegundos: number
}

// o token de acesso fica só na memória; o de renovação vive em cookie httpOnly
let tokenAcesso: string | null = null
let renovacaoEmAndamento: Promise<string | null> | null = null
let aoExpirarSessao: () => void = () => {}

export function definirTokenAcesso(token: string | null) {
  tokenAcesso = token
}

export function registrarAoExpirarSessao(callback: () => void) {
  aoExpirarSessao = callback
}

export function renovarSessao(): Promise<string | null> {
  renovacaoEmAndamento ??= executarRenovacao().finally(() => {
    renovacaoEmAndamento = null
  })
  return renovacaoEmAndamento
}

async function executarRenovacao(): Promise<string | null> {
  const resposta = await fetch('/api/auth/renovar', { method: 'POST', credentials: 'same-origin' })
  if (!resposta.ok) {
    definirTokenAcesso(null)
    return null
  }
  const corpo = (await resposta.json()) as TokenAcessoResponse
  definirTokenAcesso(corpo.tokenAcesso)
  return corpo.tokenAcesso
}

export async function requisitar<T>(caminho: string, opcoes: Opcoes = {}): Promise<T> {
  let resposta = await enviar(caminho, opcoes)

  if (resposta.status === 401 && !opcoes.semRenovacao) {
    const novoToken = await renovarSessao()
    if (novoToken === null) {
      aoExpirarSessao()
    } else {
      resposta = await enviar(caminho, opcoes)
    }
  }

  return interpretar<T>(resposta)
}

function enviar(caminho: string, opcoes: Opcoes): Promise<Response> {
  const cabecalhos: Record<string, string> = {}
  if (tokenAcesso) {
    cabecalhos.Authorization = `Bearer ${tokenAcesso}`
  }
  if (opcoes.corpo !== undefined) {
    cabecalhos['Content-Type'] = 'application/json'
  }

  return fetch(caminho, {
    method: opcoes.metodo ?? 'GET',
    headers: cabecalhos,
    body: opcoes.corpo !== undefined ? JSON.stringify(opcoes.corpo) : undefined,
    credentials: 'same-origin',
  })
}

async function interpretar<T>(resposta: Response): Promise<T> {
  if (resposta.ok) {
    if (resposta.status === 204) {
      return undefined as T
    }
    return (await resposta.json()) as T
  }

  const problema = (await resposta.json().catch(() => ({}))) as ProblemDetail
  throw new ErroApi(resposta.status, problema)
}
