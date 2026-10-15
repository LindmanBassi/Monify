import { Navigate, type RouteObject } from 'react-router'
import { RotaProtegida } from './autenticacao/RotaProtegida'
import { LayoutAplicacao } from './componentes/LayoutAplicacao'
import { Entrar } from './paginas/Entrar'
import { Categorias } from './paginas/categorias/Categorias'
import { Dashboard } from './paginas/dashboard/Dashboard'
import { Metas } from './paginas/metas/Metas'
import { Orcamentos } from './paginas/orcamentos/Orcamentos'
import { Transacoes } from './paginas/transacoes/Transacoes'

export const rotas: RouteObject[] = [
  { path: '/entrar', element: <Entrar /> },
  {
    element: <RotaProtegida />,
    children: [
      {
        element: <LayoutAplicacao />,
        children: [
          { index: true, element: <Dashboard /> },
          { path: 'transacoes', element: <Transacoes /> },
          { path: 'categorias', element: <Categorias /> },
          { path: 'orcamentos', element: <Orcamentos /> },
          { path: 'metas', element: <Metas /> },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
]
