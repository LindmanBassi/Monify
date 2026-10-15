import { useQuery } from '@tanstack/react-query'
import * as api from '../api/categorias'
import { useMutacao } from './mutacao'

export function useCategorias() {
  return useQuery({
    queryKey: ['categorias'],
    queryFn: () => api.listarCategorias({ incluirArquivadas: true }),
  })
}

export function useCriarCategoria() {
  return useMutacao(api.criarCategoria)
}

export function useEditarCategoria() {
  return useMutacao(({ id, ...dados }: { id: string; nome: string; cor: string; icone: api.IconeCategoria }) => api.editarCategoria(id, dados))
}

export function useArquivarCategoria() {
  return useMutacao(api.arquivarCategoria)
}

export function useDesarquivarCategoria() {
  return useMutacao(api.desarquivarCategoria)
}
