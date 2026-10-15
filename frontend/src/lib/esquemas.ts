import { z } from 'zod'
import { converterValor } from './formatacao'

// valor em reais digitado no campo: maior que zero e com até 2 casas
export function valorPositivo() {
  return z
    .string()
    .refine((texto) => (converterValor(texto) ?? 0) > 0, 'Informe um valor maior que zero, com até 2 casas decimais')
}
