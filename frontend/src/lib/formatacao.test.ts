import { describe, expect, it } from 'vitest'
import {
  converterValor,
  formatarData,
  formatarMes,
  formatarMesAbreviado,
  formatarMoeda,
  formatarPercentual,
  hojeEmSaoPaulo,
  mesAtualEmSaoPaulo,
  partesDaMoeda,
  somarMeses,
  valorParaCampo,
} from './formatacao'

describe('formatarMoeda', () => {
  it('deve formatar em reais com separadores brasileiros', () => {
    // Act & Assert
    expect(formatarMoeda(1234.5)).toBe('R$ 1.234,50')
  })
})

describe('formatarData', () => {
  it('deve converter a data ISO para dia/mês/ano', () => {
    // Act & Assert
    expect(formatarData('2026-10-05')).toBe('05/10/2026')
  })
})

describe('hojeEmSaoPaulo', () => {
  it('deve usar o fuso de São Paulo na virada do dia', () => {
    // Act & Assert
    expect(hojeEmSaoPaulo(new Date('2026-10-15T01:30:00Z'))).toBe('2026-10-14')
  })
})

describe('converterValor', () => {
  it.each([
    ['52,90', 52.9],
    ['1.234,56', 1234.56],
    ['1234.5', 1234.5],
    ['R$ 10', 10],
  ])('deve converter "%s" em %s', (texto, esperado) => {
    // Act & Assert
    expect(converterValor(texto)).toBe(esperado)
  })

  it.each(['', 'abc', '10,999', '-5'])('deve rejeitar "%s"', (texto) => {
    // Act & Assert
    expect(converterValor(texto)).toBeNull()
  })
})

describe('valorParaCampo', () => {
  it('deve exibir o valor com vírgula e duas casas', () => {
    // Act & Assert
    expect(valorParaCampo(52.9)).toBe('52,90')
  })
})

describe('mesAtualEmSaoPaulo', () => {
  it('deve continuar no mês anterior até a meia-noite de São Paulo', () => {
    // Act & Assert
    expect(mesAtualEmSaoPaulo(new Date('2026-11-01T02:00:00Z'))).toBe('2026-10')
  })
})

describe('somarMeses', () => {
  it.each([
    ['2026-10', 1, '2026-11'],
    ['2026-12', 1, '2027-01'],
    ['2026-01', -1, '2025-12'],
    ['2026-03', -5, '2025-10'],
  ])('deve somar a %s o total de %s mês(es)', (mes, quantidade, esperado) => {
    // Act & Assert
    expect(somarMeses(mes, quantidade)).toBe(esperado)
  })
})

describe('formatarMes', () => {
  it('deve escrever o mês por extenso', () => {
    // Act & Assert
    expect(formatarMes('2026-10')).toBe('outubro de 2026')
    expect(formatarMesAbreviado('2026-10')).toBe('out.')
  })
})

describe('formatarPercentual', () => {
  it('deve usar no máximo uma casa decimal', () => {
    // Act & Assert
    expect(formatarPercentual(83.33)).toBe('83,3%')
    expect(formatarPercentual(112)).toBe('112%')
  })
})

describe('partesDaMoeda', () => {
  it('deve separar sinal, símbolo e número', () => {
    // Act & Assert
    expect(partesDaMoeda(1234.5)).toEqual({ sinal: '', simbolo: 'R$', numero: '1.234,50' })
    expect(partesDaMoeda(-850.5)).toEqual({ sinal: '−', simbolo: 'R$', numero: '850,50' })
  })
})
