import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { buscarDashboard } from '../api/dashboard'

export function useDashboard(mes: string) {
  return useQuery({
    queryKey: ['dashboard', mes],
    queryFn: () => buscarDashboard(mes),
    placeholderData: keepPreviousData,
  })
}
