import { zodResolver } from '@hookform/resolvers/zod'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import type { TipoTransacao } from '../../api/categorias'
import { Alerta, Botao, Campo } from '../../componentes/ui'
import { classeCampo } from '../../componentes/estilos'
import { PALETA } from './paleta'
import { SeletorCor } from './SeletorCor'

// mesmas regras do CriarCategoriaRequest da API
const esquema = z.object({
  nome: z.string().trim().min(1, 'Nome é obrigatório').max(50, 'Nome deve ter no máximo 50 caracteres'),
  tipo: z.enum(['DESPESA', 'RECEITA']),
  cor: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Cor deve estar no formato hexadecimal #RRGGBB'),
})

export type DadosCategoria = z.infer<typeof esquema>

type Props = {
  titulo: string
  inicial?: Partial<DadosCategoria>
  // tipo fixo: só pode ser escolhido na criação
  permitirTipo: boolean
  erro: string | null
  salvando: boolean
  aoSalvar: (dados: DadosCategoria) => void
  aoCancelar: () => void
}

export function FormularioCategoria({ titulo, inicial, permitirTipo, erro, salvando, aoSalvar, aoCancelar }: Props) {
  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<DadosCategoria>({
    resolver: zodResolver(esquema),
    defaultValues: { nome: '', tipo: 'DESPESA' as TipoTransacao, cor: PALETA[0], ...inicial },
  })

  return (
    <form
      noValidate
      aria-label={titulo}
      onSubmit={handleSubmit(aoSalvar)}
      className="flex flex-col gap-4 rounded-[14px] border border-[#C7D2FE] bg-superficie p-5"
    >
      <h2 className="text-base font-extrabold">{titulo}</h2>

      <div className="grid gap-4 sm:grid-cols-2">
        <Campo rotulo="Nome" erro={errors.nome?.message}>
          <input type="text" placeholder="Ex.: Pets" {...register('nome')} className={classeCampo(!!errors.nome)} />
        </Campo>

        {permitirTipo && (
          <fieldset className="flex flex-col gap-1.5">
            <legend className="mb-1.5 text-[13px] font-semibold">Tipo</legend>
            <div className="flex gap-4 text-sm font-semibold">
              <label className="flex min-h-11 items-center gap-2">
                <input type="radio" value="DESPESA" {...register('tipo')} className="size-4 accent-destaque" />
                Despesa
              </label>
              <label className="flex min-h-11 items-center gap-2">
                <input type="radio" value="RECEITA" {...register('tipo')} className="size-4 accent-destaque" />
                Receita
              </label>
            </div>
          </fieldset>
        )}
      </div>

      <div className="flex flex-col gap-1.5">
        <span className="text-[13px] font-semibold">Cor</span>
        <Controller
          control={control}
          name="cor"
          render={({ field }) => <SeletorCor valor={field.value} aoMudar={field.onChange} />}
        />
        {errors.cor && <span className="text-[13px] font-semibold text-erro-texto">{errors.cor.message}</span>}
      </div>

      {erro && <Alerta>{erro}</Alerta>}

      <div className="flex justify-end gap-2.5">
        <Botao variante="secundario" onClick={aoCancelar}>Cancelar</Botao>
        <Botao type="submit" disabled={salvando}>{salvando ? 'Salvando…' : 'Salvar'}</Botao>
      </div>
    </form>
  )
}
