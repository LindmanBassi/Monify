import { keepPreviousData, useQuery } from '@tanstack/react-query'
import * as economia from '../api/metasEconomia'
import * as poupanca from '../api/metasPoupanca'
import { useMutacao } from './mutacao'

export function useMetasEconomia(mes: string) {
  return useQuery({
    queryKey: ['metas-economia', mes],
    queryFn: () => economia.listarMetasEconomia(mes),
  })
}

export function useCriarMetaEconomia() {
  return useMutacao(economia.criarMetaEconomia)
}

export function useEditarMetaEconomia() {
  return useMutacao(({ id, dados }: { id: string; dados: economia.DadosMetaEconomia }) =>
    economia.editarMetaEconomia(id, dados),
  )
}

export function useCancelarMetaEconomia() {
  return useMutacao(economia.cancelarMetaEconomia)
}

export function useMetasPoupanca() {
  return useQuery({
    queryKey: ['metas-poupanca'],
    queryFn: poupanca.listarMetasPoupanca,
  })
}

export function useSalvarMetaPoupanca() {
  return useMutacao(({ id, dados }: { id?: string; dados: poupanca.DadosMetaPoupanca }) =>
    id ? poupanca.editarMetaPoupanca(id, dados) : poupanca.criarMetaPoupanca(dados),
  )
}

export function useCancelarMetaPoupanca() {
  return useMutacao(poupanca.cancelarMetaPoupanca)
}

export function useRegistrarAporte() {
  return useMutacao(({ id, dados }: { id: string; dados: Parameters<typeof poupanca.registrarAporte>[1] }) =>
    poupanca.registrarAporte(id, dados),
  )
}

export function useAportes(id: string, pagina: number) {
  return useQuery({
    queryKey: ['metas-poupanca', id, 'aportes', pagina],
    queryFn: () => poupanca.listarAportes(id, pagina),
    placeholderData: keepPreviousData,
  })
}
