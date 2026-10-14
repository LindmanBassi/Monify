import { NavLink, Outlet } from 'react-router'
import { useAutenticacao } from '../autenticacao/ContextoAutenticacao'
import {
  IconeCategorias,
  IconeDashboard,
  IconeMetas,
  IconeOrcamentos,
  IconeTransacoes,
  Logo,
} from './Icones'

const itensMenu = [
  { para: '/', rotulo: 'Dashboard', Icone: IconeDashboard },
  { para: '/transacoes', rotulo: 'Transações', Icone: IconeTransacoes },
  { para: '/categorias', rotulo: 'Categorias', Icone: IconeCategorias },
  { para: '/orcamentos', rotulo: 'Orçamentos', Icone: IconeOrcamentos },
  { para: '/metas', rotulo: 'Metas', Icone: IconeMetas },
]

export function LayoutAplicacao() {
  const { estado, sair } = useAutenticacao()
  const nome = estado.situacao === 'autenticado' ? estado.usuario.nome : ''

  return (
    <div className="flex min-h-screen flex-wrap">
      <nav
        aria-label="Menu principal"
        className="flex flex-[1_1_220px] flex-col gap-7 border-r border-borda bg-superficie px-[18px] py-7"
      >
        <div className="flex items-center gap-2.5 px-2">
          <Logo />
          <span className="text-[19px] font-extrabold tracking-tight">Monify</span>
        </div>

        <ul className="flex flex-col gap-1">
          {itensMenu.map(({ para, rotulo, Icone }) => (
            <li key={para}>
              <NavLink
                to={para}
                end={para === '/'}
                className={({ isActive }) =>
                  `flex min-h-11 items-center gap-3 rounded-[10px] px-3 text-sm no-underline ${
                    isActive
                      ? 'bg-destaque-suave font-bold text-destaque-escuro'
                      : 'font-semibold text-texto-medio hover:bg-fundo'
                  }`
                }
              >
                <Icone />
                {rotulo}
              </NavLink>
            </li>
          ))}
        </ul>

        <div className="mt-auto flex items-center gap-2.5 border-t border-borda p-3">
          <div
            aria-hidden="true"
            className="flex size-9 items-center justify-center rounded-full bg-[#E0E7FF] text-sm font-extrabold text-destaque-escuro"
          >
            {nome.charAt(0).toUpperCase()}
          </div>
          <div className="flex min-w-0 flex-col items-start">
            <span className="truncate text-sm font-bold">{nome}</span>
            <button
              type="button"
              onClick={() => void sair()}
              className="cursor-pointer text-[13px] font-semibold text-destaque hover:text-destaque-escuro"
            >
              Sair
            </button>
          </div>
        </div>
      </nav>

      <main className="min-w-0 flex-[999_1_560px] p-8">
        <Outlet />
      </main>
    </div>
  )
}
