import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createBrowserRouter, RouterProvider } from 'react-router'
import { ProvedorAutenticacao } from './autenticacao/ContextoAutenticacao'
import { rotas } from './rotas'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: false, refetchOnWindowFocus: false },
  },
})

const router = createBrowserRouter(rotas)

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ProvedorAutenticacao>
        <RouterProvider router={router} />
      </ProvedorAutenticacao>
    </QueryClientProvider>
  )
}
