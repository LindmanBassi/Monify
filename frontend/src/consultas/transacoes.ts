import { keepPreviousData, useQuery } from '@tanstack/react-query'
import * as api from '../api/transacoes'
import { useMutacao } from './mutacao'

export function useTransacoes(filtros: api.FiltrosTransacoes, pagina: number, habilitada = true) {
  return useQuery({
    queryKey: ['transacoes', filtros, pagina],
    queryFn: () => api.listarTransacoes(filtros, pagina),
    placeholderData: keepPreviousData,
    enabled: habilitada,
  })
}

export function useSalvarTransacao() {
  return useMutacao(({ id, dados }: { id?: string; dados: api.DadosTransacao }) =>
    id ? api.editarTransacao(id, dados) : api.criarTransacao(dados),
  )
}

export function useExcluirTransacao() {
  return useMutacao(api.excluirTransacao)
}
