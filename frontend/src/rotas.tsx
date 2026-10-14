import { Navigate, type RouteObject } from 'react-router'
import { RotaProtegida } from './autenticacao/RotaProtegida'
import { LayoutAplicacao } from './componentes/LayoutAplicacao'
import { EmConstrucao } from './paginas/EmConstrucao'
import { Entrar } from './paginas/Entrar'
import { Categorias } from './paginas/categorias/Categorias'
import { Transacoes } from './paginas/transacoes/Transacoes'

export const rotas: RouteObject[] = [
  { path: '/entrar', element: <Entrar /> },
  {
    element: <RotaProtegida />,
    children: [
      {
        element: <LayoutAplicacao />,
        children: [
          { index: true, element: <EmConstrucao titulo="Dashboard" descricao="Resumo do mês, comparação com o mês anterior, gastos por categoria e evolução dos últimos 6 meses." /> },
          { path: 'transacoes', element: <Transacoes /> },
          { path: 'categorias', element: <Categorias /> },
          { path: 'orcamentos', element: <EmConstrucao titulo="Orçamentos" descricao="Limite mensal por categoria, com gasto, restante e status." /> },
          { path: 'metas', element: <EmConstrucao titulo="Metas" descricao="Metas de economia e metas de poupança." /> },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
]
