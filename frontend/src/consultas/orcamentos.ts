import { useQuery } from '@tanstack/react-query'
import * as api from '../api/orcamentos'
import { useMutacao } from './mutacao'

export function useOrcamentos(mes: string) {
  return useQuery({
    queryKey: ['orcamentos', mes],
    queryFn: () => api.listarOrcamentos(mes),
  })
}

export function useDefinirOrcamento() {
  return useMutacao(api.definirOrcamento)
}

export function useAlterarLimite() {
  return useMutacao(({ id, valorLimite }: { id: string; valorLimite: number }) => api.alterarLimite(id, valorLimite))
}

export function useExcluirOrcamento() {
  return useMutacao(api.excluirOrcamento)
}
