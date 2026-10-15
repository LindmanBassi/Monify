import {
  ArrowLeftRight,
  ArrowRight,
  ChartPie,
  ChevronRight,
  LayoutGrid,
  LogOut,
  PanelLeft,
  Shapes,
  ShieldCheck,
  Sparkles,
  Target,
} from 'lucide-react'
import { useState } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router'
import { useAutenticacao } from '../autenticacao/ContextoAutenticacao'
import { Marca } from './Icones'

const itensMenu = [
  { para: '/', rotulo: 'Visão geral', Icone: LayoutGrid },
  { para: '/transacoes', rotulo: 'Transações', Icone: ArrowLeftRight },
  { para: '/categorias', rotulo: 'Categorias', Icone: Shapes },
  { para: '/orcamentos', rotulo: 'Orçamentos', Icone: ChartPie },
  { para: '/metas', rotulo: 'Metas', Icone: Target },
]

const ANO_ATUAL = new Date().getFullYear()

function telaLarga() {
  return typeof window.matchMedia !== 'function' || window.matchMedia('(min-width: 1024px)').matches
}

function iniciais(nome: string) {
  const partes = nome.trim().split(/\s+/)
  return ((partes[0]?.[0] ?? '') + (partes.length > 1 ? partes[partes.length - 1][0] : '')).toUpperCase()
}

export function LayoutAplicacao() {
  const { estado, sair } = useAutenticacao()
  const { pathname } = useLocation()
  const [menuAberto, setMenuAberto] = useState(telaLarga)
  const nome = estado.situacao === 'autenticado' ? estado.usuario.nome : ''
  const paginaAtual = itensMenu.find(({ para }) => (para === '/' ? pathname === '/' : pathname.startsWith(para)))

  function fecharNaTelaEstreita() {
    if (!telaLarga()) {
      setMenuAberto(false)
    }
  }

  return (
    <div className="flex min-h-screen">
      {menuAberto && (
        <nav
          aria-label="Menu principal"
          className="fixed inset-y-0 left-0 z-20 flex w-[248px] flex-none flex-col gap-8 overflow-y-auto border-r border-borda bg-superficie px-4 py-7 shadow-xl lg:sticky lg:top-0 lg:h-screen lg:shadow-none"
        >
          <div className="px-2">
            <Marca />
          </div>

          <div className="flex flex-col gap-3">
            <span className="px-2 text-[11px] font-bold tracking-[0.14em] text-texto-suave uppercase">Seu dinheiro, em ordem</span>
            <ul className="flex flex-col gap-1">
              {itensMenu.map(({ para, rotulo, Icone }) => (
                <li key={para}>
                  <NavLink
                    to={para}
                    end={para === '/'}
                    onClick={fecharNaTelaEstreita}
                    className={({ isActive }) =>
                      `flex min-h-11 items-center gap-3 rounded-[10px] px-3 text-sm no-underline ${
                        isActive ? 'bg-destaque-suave font-semibold text-destaque-escuro' : 'font-medium text-texto-medio hover:bg-fundo'
                      }`
                    }
                  >
                    {({ isActive }) => (
                      <>
                        <Icone size={19} aria-hidden="true" />
                        <span className="flex-1">{rotulo}</span>
                        {isActive && <span aria-hidden="true" className="size-1.5 rounded-full bg-destaque-escuro" />}
                      </>
                    )}
                  </NavLink>
                </li>
              ))}
            </ul>
          </div>

          <div className="mt-auto flex flex-col gap-2 rounded-xl border border-borda bg-fundo p-4">
            <Sparkles size={20} className="text-destaque-escuro" aria-hidden="true" />
            <p className="text-sm font-bold">Pequenos passos. Grandes planos.</p>
            <p className="text-[13px] leading-relaxed text-texto-suave">Cada escolha de hoje aproxima você dos seus objetivos.</p>
            <Link
              to="/metas"
              onClick={fecharNaTelaEstreita}
              className="mt-1 inline-flex items-center gap-1.5 text-[13px] font-semibold text-destaque-escuro no-underline hover:underline"
            >
              Ver minhas metas <ArrowRight size={15} aria-hidden="true" />
            </Link>
          </div>

          <div className="flex items-center gap-3 border-t border-borda px-1 pt-4">
            <Avatar nome={nome} />
            <div className="flex min-w-0 flex-1 flex-col">
              <span className="truncate text-sm font-bold">{nome}</span>
              <span className="text-xs text-texto-suave">Conta pessoal</span>
            </div>
            <button
              type="button"
              aria-label="Sair"
              title="Sair"
              onClick={() => void sair()}
              className="flex size-10 cursor-pointer items-center justify-center rounded-[10px] text-texto-medio hover:bg-fundo hover:text-tinta"
            >
              <LogOut size={18} aria-hidden="true" />
            </button>
          </div>
        </nav>
      )}

      {menuAberto && (
        <button
          type="button"
          aria-label="Fechar menu"
          onClick={() => setMenuAberto(false)}
          className="fixed inset-0 z-10 bg-tinta/30 lg:hidden"
        />
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-[76px] items-center justify-between gap-4 border-b border-borda bg-superficie px-5 sm:px-8">
          <div className="flex min-w-0 items-center gap-4">
            <button
              type="button"
              aria-label={menuAberto ? 'Recolher menu' : 'Mostrar menu'}
              aria-expanded={menuAberto}
              onClick={() => setMenuAberto((aberto) => !aberto)}
              className="flex size-10 cursor-pointer items-center justify-center rounded-[10px] text-texto-medio hover:bg-fundo"
            >
              <PanelLeft size={19} aria-hidden="true" />
            </button>
            <nav aria-label="Você está em" className="flex min-w-0 items-center gap-2 text-sm">
              <span className="text-texto-suave">Meu espaço</span>
              <ChevronRight size={15} className="text-texto-suave" aria-hidden="true" />
              <span aria-current="page" className="truncate font-semibold">{paginaAtual?.rotulo ?? 'Visão geral'}</span>
            </nav>
          </div>
          <Avatar nome={nome} />
        </header>

        <main className="flex w-full max-w-[1240px] flex-1 flex-col gap-8 px-5 py-8 sm:px-10">
          <Outlet />
        </main>

        <footer className="flex w-full max-w-[1240px] flex-wrap items-center justify-between gap-2 px-5 pb-6 text-xs text-texto-suave sm:px-10">
          <span>Monify © {ANO_ATUAL} · Mais clareza. Mais possibilidades.</span>
          <span className="flex items-center gap-1.5">
            <ShieldCheck size={14} aria-hidden="true" /> Seu dinheiro merece cuidado.
          </span>
        </footer>
      </div>
    </div>
  )
}

function Avatar({ nome }: { nome: string }) {
  return (
    <span
      aria-hidden="true"
      className="flex size-10 flex-none items-center justify-center rounded-full bg-destaque-suave text-sm font-bold text-destaque-escuro"
    >
      {iniciais(nome)}
    </span>
  )
}
