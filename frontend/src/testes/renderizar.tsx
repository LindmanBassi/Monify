import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { ProvedorAutenticacao } from '../autenticacao/ContextoAutenticacao'
import { rotas } from '../rotas'

export function renderizarAplicacao(caminhoInicial: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const router = createMemoryRouter(rotas, { initialEntries: [caminhoInicial] })

  render(
    <QueryClientProvider client={queryClient}>
      <ProvedorAutenticacao>
        <RouterProvider router={router} />
      </ProvedorAutenticacao>
    </QueryClientProvider>,
  )

  return router
}
