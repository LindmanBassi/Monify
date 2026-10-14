import { useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import * as api from '../api/autenticacao'
import { definirTokenAcesso, registrarAoExpirarSessao, renovarSessao } from '../api/cliente'

type Estado =
  | { situacao: 'carregando' }
  | { situacao: 'anonimo' }
  | { situacao: 'autenticado'; usuario: api.Usuario }

type ContextoValor = {
  estado: Estado
  entrar: (email: string, senha: string) => Promise<void>
  cadastrar: (nome: string, email: string, senha: string) => Promise<void>
  sair: () => Promise<void>
}

const Contexto = createContext<ContextoValor | null>(null)

export function ProvedorAutenticacao({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [estado, setEstado] = useState<Estado>({ situacao: 'carregando' })

  const encerrarSessaoLocal = useCallback(() => {
    definirTokenAcesso(null)
    queryClient.clear()
    setEstado({ situacao: 'anonimo' })
  }, [queryClient])

  // ao abrir o app, o cookie de renovação restaura a sessão sem pedir login de novo
  useEffect(() => {
    registrarAoExpirarSessao(encerrarSessaoLocal)

    let ativo = true
    renovarSessao()
      .then((token) => (token ? api.buscarUsuarioAtual() : null))
      .then((usuario) => {
        if (ativo) {
          setEstado(usuario ? { situacao: 'autenticado', usuario } : { situacao: 'anonimo' })
        }
      })
      .catch(() => ativo && setEstado({ situacao: 'anonimo' }))

    return () => {
      ativo = false
    }
  }, [encerrarSessaoLocal])

  const entrar = useCallback(async (email: string, senha: string) => {
    await api.entrar(email, senha)
    const usuario = await api.buscarUsuarioAtual()
    setEstado({ situacao: 'autenticado', usuario })
  }, [])

  const cadastrar = useCallback(
    async (nome: string, email: string, senha: string) => {
      await api.cadastrar(nome, email, senha)
      await entrar(email, senha)
    },
    [entrar],
  )

  const sair = useCallback(async () => {
    try {
      await api.sair()
    } finally {
      encerrarSessaoLocal()
    }
  }, [encerrarSessaoLocal])

  const valor = useMemo(() => ({ estado, entrar, cadastrar, sair }), [estado, entrar, cadastrar, sair])

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>
}

// eslint-disable-next-line react/only-export-components
export function useAutenticacao(): ContextoValor {
  const contexto = useContext(Contexto)
  if (!contexto) {
    throw new Error('useAutenticacao deve ser usado dentro de ProvedorAutenticacao')
  }
  return contexto
}
