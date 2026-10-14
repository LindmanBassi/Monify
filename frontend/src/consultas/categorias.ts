import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as api from '../api/categorias'

const CHAVE = ['categorias'] as const

export function useCategorias() {
  return useQuery({
    queryKey: CHAVE,
    queryFn: () => api.listarCategorias({ incluirArquivadas: true }),
  })
}

function useMutacaoCategoria<T>(funcao: (dados: T) => Promise<unknown>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: funcao,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: CHAVE })
      void queryClient.invalidateQueries({ queryKey: ['transacoes'] })
    },
  })
}

export function useCriarCategoria() {
  return useMutacaoCategoria(api.criarCategoria)
}

export function useEditarCategoria() {
  return useMutacaoCategoria(({ id, ...dados }: { id: string; nome: string; cor: string }) =>
    api.editarCategoria(id, dados),
  )
}

export function useArquivarCategoria() {
  return useMutacaoCategoria(api.arquivarCategoria)
}

export function useDesarquivarCategoria() {
  return useMutacaoCategoria(api.desarquivarCategoria)
}
