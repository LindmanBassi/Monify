import { vi } from 'vitest'

export type RequisicaoSimulada = {
  metodo: string
  corpo: unknown
  autorizacao: string | null
  parametros: URLSearchParams
}

type Rota = (requisicao: RequisicaoSimulada) => Response

export function respostaJson(status: number, corpo?: unknown): Response {
  if (corpo === undefined) {
    return new Response(null, { status })
  }
  return new Response(JSON.stringify(corpo), { status, headers: { 'Content-Type': 'application/json' } })
}

// cada caminho (sem a query string) responde com uma função; caminhos não previstos falham o teste
export function simularApi(rotas: Record<string, Rota>) {
  const fetchSimulado = vi.fn(async (entrada: RequestInfo | URL, init?: RequestInit) => {
    const url = new URL(String(entrada), 'http://localhost')
    const rota = rotas[url.pathname]
    if (!rota) {
      throw new Error(`Chamada inesperada para ${url.pathname}`)
    }
    const cabecalhos = (init?.headers ?? {}) as Record<string, string>
    return rota({
      metodo: init?.method ?? 'GET',
      corpo: init?.body ? JSON.parse(String(init.body)) : undefined,
      autorizacao: cabecalhos.Authorization ?? null,
      parametros: url.searchParams,
    })
  })
  vi.stubGlobal('fetch', fetchSimulado)
  return fetchSimulado
}

export const usuarioDeTeste = { id: 'u1', nome: 'Ana Souza', email: 'ana@email.com' }

// sessão já restaurada pelo cookie: o app abre direto nas telas logadas
export function rotasDeSessao(): Record<string, Rota> {
  return {
    '/api/auth/renovar': () => respostaJson(200, { tokenAcesso: 'token', tipo: 'Bearer', expiraEmSegundos: 900 }),
    '/api/usuarios/me': () => respostaJson(200, usuarioDeTeste),
  }
}
