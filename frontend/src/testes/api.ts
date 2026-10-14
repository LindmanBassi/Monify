import { vi } from 'vitest'

type Rota = (requisicao: { metodo: string; corpo: unknown; autorizacao: string | null }) => Response

export function respostaJson(status: number, corpo?: unknown): Response {
  if (corpo === undefined) {
    return new Response(null, { status })
  }
  return new Response(JSON.stringify(corpo), { status, headers: { 'Content-Type': 'application/json' } })
}

// simula a API: cada caminho responde com uma função; caminhos não previstos falham o teste
export function simularApi(rotas: Record<string, Rota>) {
  const fetchSimulado = vi.fn(async (entrada: RequestInfo | URL, init?: RequestInit) => {
    const caminho = String(entrada)
    const rota = rotas[caminho]
    if (!rota) {
      throw new Error(`Chamada inesperada para ${caminho}`)
    }
    const cabecalhos = (init?.headers ?? {}) as Record<string, string>
    return rota({
      metodo: init?.method ?? 'GET',
      corpo: init?.body ? JSON.parse(String(init.body)) : undefined,
      autorizacao: cabecalhos.Authorization ?? null,
    })
  })
  vi.stubGlobal('fetch', fetchSimulado)
  return fetchSimulado
}
