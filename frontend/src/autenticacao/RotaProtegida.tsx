import { Navigate, Outlet, useLocation } from 'react-router'
import { useAutenticacao } from './ContextoAutenticacao'

export function RotaProtegida() {
  const { estado } = useAutenticacao()
  const location = useLocation()

  if (estado.situacao === 'carregando') {
    return (
      <div role="status" className="flex min-h-screen items-center justify-center text-sm font-semibold text-texto-suave">
        Carregando sua sessão…
      </div>
    )
  }

  if (estado.situacao === 'anonimo') {
    return <Navigate to="/entrar" replace state={{ de: location.pathname }} />
  }

  return <Outlet />
}
