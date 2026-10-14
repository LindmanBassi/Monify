import { Navigate, type RouteObject } from 'react-router'
import { RotaProtegida } from './autenticacao/RotaProtegida'
import { LayoutAplicacao } from './componentes/LayoutAplicacao'
import { EmConstrucao } from './paginas/EmConstrucao'
import { Entrar } from './paginas/Entrar'

export const rotas: RouteObject[] = [
  { path: '/entrar', element: <Entrar /> },
  {
    element: <RotaProtegida />,
    children: [
      {
        element: <LayoutAplicacao />,
        children: [
          { index: true, element: <EmConstrucao titulo="Dashboard" descricao="Resumo do mês, comparação com o mês anterior, gastos por categoria e evolução dos últimos 6 meses." /> },
          { path: 'transacoes', element: <EmConstrucao titulo="Transações" descricao="Histórico com filtros por período, categoria, tipo e texto." /> },
          { path: 'categorias', element: <EmConstrucao titulo="Categorias" descricao="Categorias de receita e despesa, com cores, arquivar e desarquivar." /> },
          { path: 'orcamentos', element: <EmConstrucao titulo="Orçamentos" descricao="Limite mensal por categoria, com gasto, restante e status." /> },
          { path: 'metas', element: <EmConstrucao titulo="Metas" descricao="Metas de economia e metas de poupança." /> },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
]
