import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as api from '../api/transacoes'

export function useTransacoes(filtros: api.FiltrosTransacoes, pagina: number, habilitada = true) {
  return useQuery({
    queryKey: ['transacoes', filtros, pagina],
    queryFn: () => api.listarTransacoes(filtros, pagina),
    placeholderData: keepPreviousData,
    enabled: habilitada,
  })
}

function useMutacaoTransacao<T>(funcao: (dados: T) => Promise<unknown>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: funcao,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['transacoes'] })
    },
  })
}

export function useSalvarTransacao() {
  return useMutacaoTransacao(({ id, dados }: { id?: string; dados: api.DadosTransacao }) =>
    id ? api.editarTransacao(id, dados) : api.criarTransacao(dados),
  )
}

export function useExcluirTransacao() {
  return useMutacaoTransacao(api.excluirTransacao)
}
