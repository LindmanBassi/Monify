import { useMutation, useQueryClient } from '@tanstack/react-query'

// lançamentos mudam saldos, gastos e status de várias telas: tudo é recarregado
export function useMutacao<T>(funcao: (dados: T) => Promise<unknown>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: funcao,
    onSuccess: () => queryClient.invalidateQueries(),
  })
}
