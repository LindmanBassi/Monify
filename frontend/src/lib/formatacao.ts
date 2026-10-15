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

// "mês atual" no fuso de São Paulo, igual ao backend, no formato yyyy-MM
export function mesAtualEmSaoPaulo(agora: Date = new Date()): string {
  return hojeEmSaoPaulo(agora).slice(0, 7)
}

export function somarMeses(mes: string, quantidade: number): string {
  const [ano, numero] = mes.split('-').map(Number)
  const total = ano * 12 + (numero - 1) + quantidade
  return `${Math.floor(total / 12)}-${String((total % 12) + 1).padStart(2, '0')}`
}

function dataDoMes(mes: string): Date {
  const [ano, numero] = mes.split('-').map(Number)
  return new Date(Date.UTC(ano, numero - 1, 1))
}

const mesPorExtenso = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric', timeZone: 'UTC' })
const nomeDoMes = new Intl.DateTimeFormat('pt-BR', { month: 'long', timeZone: 'UTC' })
const mesAbreviado = new Intl.DateTimeFormat('pt-BR', { month: 'short', timeZone: 'UTC' })

// "outubro de 2026"
export function formatarMes(mes: string): string {
  return mesPorExtenso.format(dataDoMes(mes))
}

// "outubro"
export function formatarNomeDoMes(mes: string): string {
  return nomeDoMes.format(dataDoMes(mes))
}

// "out."
export function formatarMesAbreviado(mes: string): string {
  return mesAbreviado.format(dataDoMes(mes))
}

const percentual = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 })

export function formatarPercentual(valor: number): string {
  return `${percentual.format(valor)}%`
}

// separa sinal, símbolo e número para exibir o "R$" menor
export function partesDaMoeda(valor: number): { sinal: string; simbolo: string; numero: string } {
  const partes = moeda.formatToParts(valor)
  const juntar = (tipos: string[]) =>
    partes.filter((parte) => tipos.includes(parte.type)).map((parte) => parte.value).join('')
  return {
    sinal: valor < 0 ? '−' : '',
    simbolo: juntar(['currency']),
    numero: juntar(['integer', 'group', 'decimal', 'fraction']),
  }
}
