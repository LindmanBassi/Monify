import { describe, expect, it } from 'vitest'
import { converterValor, formatarData, formatarMoeda, hojeEmSaoPaulo, valorParaCampo } from './formatacao'

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
