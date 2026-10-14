import { zodResolver } from '@hookform/resolvers/zod'
import { useState, type ReactNode } from 'react'
import { useForm } from 'react-hook-form'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { z } from 'zod'
import { ErroApi } from '../api/cliente'
import { useAutenticacao } from '../autenticacao/ContextoAutenticacao'
import { IconeAlerta, IconeConfirmar, Logo } from '../componentes/Icones'

// mesmas regras do CadastroRequest da API
const esquemaEntrar = z.object({
  nome: z.string(),
  email: z.email('E-mail inválido'),
  senha: z.string().min(1, 'Senha é obrigatória'),
})

const esquemaCriar = esquemaEntrar.extend({
  nome: z.string().trim().min(1, 'Nome é obrigatório').max(100, 'Nome deve ter no máximo 100 caracteres'),
  senha: z.string().min(8, 'Senha deve ter entre 8 e 72 caracteres').max(72, 'Senha deve ter entre 8 e 72 caracteres'),
})

type Campos = z.infer<typeof esquemaEntrar>
type Modo = 'entrar' | 'criar'

const destaques = [
  'Orçamentos com alerta a partir de 80% do limite',
  'Metas de economia comparadas ao seu histórico',
  'Quanto guardar por mês para cada objetivo',
]

export function Entrar() {
  const { estado, entrar, cadastrar } = useAutenticacao()
  const navigate = useNavigate()
  const location = useLocation()
  const [modo, setModo] = useState<Modo>('entrar')
  const [erroGeral, setErroGeral] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    setError,
    clearErrors,
    formState: { errors, isSubmitting },
  } = useForm<Campos>({
    resolver: zodResolver(modo === 'entrar' ? esquemaEntrar : esquemaCriar),
    defaultValues: { nome: '', email: '', senha: '' },
  })

  if (estado.situacao === 'autenticado') {
    return <Navigate to="/" replace />
  }

  const destino = (location.state as { de?: string } | null)?.de ?? '/'

  function trocarModo(novo: Modo) {
    setModo(novo)
    setErroGeral(null)
    clearErrors()
  }

  async function enviar(campos: Campos) {
    setErroGeral(null)
    try {
      if (modo === 'entrar') {
        await entrar(campos.email, campos.senha)
      } else {
        await cadastrar(campos.nome, campos.email, campos.senha)
      }
      navigate(destino, { replace: true })
    } catch (erro) {
      if (erro instanceof ErroApi && erro.camposInvalidos.length > 0) {
        erro.camposInvalidos.forEach(({ campo, mensagem }) =>
          setError(campo as keyof Campos, { message: mensagem }),
        )
      } else {
        setErroGeral(erro instanceof ErroApi ? erro.message : 'Não foi possível conectar ao servidor')
      }
    }
  }

  const estiloAba = (ativa: boolean) =>
    `h-10 cursor-pointer rounded-[9px] text-sm ${
      ativa ? 'bg-superficie font-bold text-tinta shadow-sm' : 'font-semibold text-texto-suave'
    }`

  return (
    <div className="flex min-h-screen flex-wrap">
      <section className="flex flex-[1_1_420px] flex-col justify-between gap-12 bg-tinta p-14 text-fundo">
        <div className="flex items-center gap-3">
          <Logo tamanho={36} />
          <span className="text-[22px] font-extrabold tracking-tight">Monify</span>
        </div>
        <div className="flex max-w-[460px] flex-col gap-5">
          <h1 className="text-[44px] leading-tight font-extrabold tracking-tight">Seu dinheiro, com clareza.</h1>
          <p className="text-[17px] leading-relaxed text-[#C9CCD3]">
            Registre receitas e despesas, defina orçamentos por categoria e acompanhe metas de economia e de poupança
            em um só lugar.
          </p>
        </div>
        <ul className="flex flex-col gap-3.5 text-[15px] text-[#DDE0E6]">
          {destaques.map((texto) => (
            <li key={texto} className="flex items-center gap-2.5">
              <span className="text-[#A5B4FC]"><IconeConfirmar /></span>
              {texto}
            </li>
          ))}
        </ul>
      </section>

      <section className="flex min-w-0 flex-[999_1_480px] items-center justify-center px-6 py-12">
        <div className="flex w-full max-w-[420px] flex-col gap-6 rounded-[18px] border border-borda bg-superficie p-9">
          <div role="tablist" aria-label="Acesso" className="grid grid-cols-2 gap-1 rounded-xl bg-[#F0F1F4] p-1">
            <button type="button" role="tab" aria-selected={modo === 'entrar'} className={estiloAba(modo === 'entrar')} onClick={() => trocarModo('entrar')}>
              Entrar
            </button>
            <button type="button" role="tab" aria-selected={modo === 'criar'} className={estiloAba(modo === 'criar')} onClick={() => trocarModo('criar')}>
              Criar conta
            </button>
          </div>

          <div className="flex flex-col gap-1.5">
            <h2 className="text-2xl font-extrabold tracking-tight">
              {modo === 'entrar' ? 'Bem-vindo de volta' : 'Crie sua conta'}
            </h2>
            <p className="text-sm text-texto-suave">
              {modo === 'entrar'
                ? 'Entre para ver o resumo do seu mês.'
                : 'Já começamos com categorias como Alimentação, Moradia e Salário.'}
            </p>
          </div>

          <form noValidate onSubmit={handleSubmit(enviar)} className="flex flex-col gap-4">
            {modo === 'criar' && (
              <Campo rotulo="Nome" erro={errors.nome?.message}>
                <input type="text" autoComplete="name" placeholder="Seu nome" {...register('nome')} className={estiloCampo(!!errors.nome)} />
              </Campo>
            )}
            <Campo rotulo="E-mail" erro={errors.email?.message}>
              <input type="email" autoComplete="email" placeholder="voce@email.com" {...register('email')} className={estiloCampo(!!errors.email)} />
            </Campo>
            <Campo rotulo="Senha" erro={errors.senha?.message}>
              <input
                type="password"
                autoComplete={modo === 'entrar' ? 'current-password' : 'new-password'}
                placeholder={modo === 'entrar' ? 'Sua senha' : 'Mínimo de 8 caracteres'}
                {...register('senha')}
                className={estiloCampo(!!errors.senha)}
              />
            </Campo>

            {erroGeral && (
              <div role="alert" className="flex items-start gap-2.5 rounded-[10px] bg-erro-fundo px-3 py-2.5 text-[13px] leading-snug text-erro-texto">
                <span className="mt-px shrink-0"><IconeAlerta tamanho={16} /></span>
                <span>{erroGeral}</span>
              </div>
            )}

            <button
              type="submit"
              disabled={isSubmitting}
              className="h-12 cursor-pointer rounded-[10px] bg-destaque text-[15px] font-bold text-white hover:bg-destaque-escuro disabled:cursor-wait disabled:opacity-70"
            >
              {isSubmitting ? 'Aguarde…' : modo === 'entrar' ? 'Entrar' : 'Criar conta e entrar'}
            </button>
          </form>
        </div>
      </section>
    </div>
  )
}

function estiloCampo(comErro: boolean) {
  return `h-[46px] rounded-[10px] border px-3.5 text-[15px] font-medium outline-none focus:border-destaque focus:ring-2 focus:ring-destaque/20 ${
    comErro ? 'border-erro-texto' : 'border-borda-campo'
  }`
}

function Campo({ rotulo, erro, children }: { rotulo: string; erro?: string; children: ReactNode }) {
  return (
    <label className="flex flex-col gap-1.5 text-[13px] font-semibold">
      {rotulo}
      {children}
      {erro && <span className="text-[13px] font-semibold text-erro-texto">{erro}</span>}
    </label>
  )
}
