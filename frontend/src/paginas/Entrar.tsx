import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { z } from 'zod'
import { ErroApi } from '../api/cliente'
import { useAutenticacao } from '../autenticacao/ContextoAutenticacao'
import { CircleCheck } from 'lucide-react'
import { Marca } from '../componentes/Icones'
import { Alerta, Campo } from '../componentes/ui'
import { classeBotao, classeCampo, estilosBotao } from '../componentes/estilos'

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
      ativa ? 'bg-superficie font-semibold text-tinta shadow-sm' : 'font-medium text-texto-suave'
    }`

  return (
    <div className="flex min-h-screen flex-wrap">
      <section className="relative flex flex-[1_1_420px] flex-col justify-between gap-12 overflow-hidden bg-escuro p-10 text-white sm:p-14">
        <span aria-hidden="true" className="absolute -top-24 -right-20 size-80 rounded-full border-[40px] border-white/[0.04]" />
        <span aria-hidden="true" className="absolute -right-32 -bottom-40 size-96 rounded-full border-[40px] border-white/[0.03]" />
        <span className="relative">
          <Marca escuro />
        </span>
        <div className="relative flex max-w-[460px] flex-col gap-5">
          <span className="text-[11px] font-bold tracking-[0.14em] text-destaque uppercase">Seu dinheiro, em ordem</span>
          <h1 className="text-[42px] leading-tight font-bold tracking-tight">Mais clareza. Mais possibilidades.</h1>
          <p className="text-base leading-relaxed text-[#C9D2CD]">
            Registre receitas e despesas, defina orçamentos por categoria e acompanhe metas de economia e de poupança
            em um só lugar.
          </p>
        </div>
        <ul className="relative flex flex-col gap-3.5 text-[15px] text-[#DCE3DF]">
          {destaques.map((texto) => (
            <li key={texto} className="flex items-center gap-2.5">
              <CircleCheck size={18} className="flex-none text-destaque" aria-hidden="true" />
              {texto}
            </li>
          ))}
        </ul>
      </section>

      <section className="flex min-w-0 flex-[999_1_480px] items-center justify-center px-6 py-12">
        <div className="flex w-full max-w-[420px] flex-col gap-6 rounded-2xl border border-borda bg-superficie p-9 shadow-sm">
          <div role="tablist" aria-label="Acesso" className="grid grid-cols-2 gap-1 rounded-xl bg-[#E8ECEA] p-1">
            <button type="button" role="tab" aria-selected={modo === 'entrar'} className={estiloAba(modo === 'entrar')} onClick={() => trocarModo('entrar')}>
              Entrar
            </button>
            <button type="button" role="tab" aria-selected={modo === 'criar'} className={estiloAba(modo === 'criar')} onClick={() => trocarModo('criar')}>
              Criar conta
            </button>
          </div>

          <div className="flex flex-col gap-1.5">
            <h2 className="text-2xl font-bold tracking-tight">
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
                <input type="text" autoComplete="name" placeholder="Seu nome" {...register('nome')} className={classeCampo(!!errors.nome)} />
              </Campo>
            )}
            <Campo rotulo="E-mail" erro={errors.email?.message}>
              <input type="email" autoComplete="email" placeholder="voce@email.com" {...register('email')} className={classeCampo(!!errors.email)} />
            </Campo>
            <Campo rotulo="Senha" erro={errors.senha?.message}>
              <input
                type="password"
                autoComplete={modo === 'entrar' ? 'current-password' : 'new-password'}
                placeholder={modo === 'entrar' ? 'Sua senha' : 'Mínimo de 8 caracteres'}
                {...register('senha')}
                className={classeCampo(!!errors.senha)}
              />
            </Campo>

            {erroGeral && (
              <Alerta>{erroGeral}</Alerta>
            )}

            <button
              type="submit"
              disabled={isSubmitting}
              className={`${classeBotao} ${estilosBotao.primario} h-12 text-[15px]`}
            >
              {isSubmitting ? 'Aguarde…' : modo === 'entrar' ? 'Entrar' : 'Criar conta e entrar'}
            </button>
          </form>
        </div>
      </section>
    </div>
  )
}
