const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function formatarMoeda(valor: number): string {
  return moeda.format(valor)
}

export function formatarData(dataIso: string): string {
  const [ano, mes, dia] = dataIso.split('-')
  return `${dia}/${mes}/${ano}`
}

// "hoje" no fuso de São Paulo, igual ao backend, no formato yyyy-MM-dd
export function hojeEmSaoPaulo(agora: Date = new Date()): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo' }).format(agora)
}

// aceita "1.234,56", "1234,56" ou "1234.56"; nulo quando não é um valor com até 2 casas
export function converterValor(texto: string): number | null {
  const limpo = texto.trim().replace(/\s|R\$/g, '')
  const normalizado = limpo.includes(',') ? limpo.replace(/\./g, '').replace(',', '.') : limpo
  return /^\d+(\.\d{1,2})?$/.test(normalizado) ? Number(normalizado) : null
}

export function valorParaCampo(valor: number): string {
  return valor.toFixed(2).replace('.', ',')
}
