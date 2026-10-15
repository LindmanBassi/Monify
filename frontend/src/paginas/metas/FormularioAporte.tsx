import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import type { MetaPoupanca } from '../../api/metasPoupanca'
import { Alerta, Botao, Campo } from '../../componentes/ui'
import { classeCampo } from '../../componentes/estilos'
import { useRegistrarAporte } from '../../consultas/metas'
import { mensagemDeErro } from '../../lib/erros'
import { valorPositivo } from '../../lib/esquemas'
import { converterValor, formatarMoeda, hojeEmSaoPaulo } from '../../lib/formatacao'

function criarEsquema(hoje: string, acumulado: number) {
  return z
    .object({
      tipo: z.enum(['APORTE', 'RESGATE']),
      valor: valorPositivo(),
      data: z
        .string()
        .min(1, 'Data é obrigatória')
        .refine((data) => data <= hoje, 'Data não pode ser futura'),
      observacao: z.string().max(255, 'Observação deve ter no máximo 255 caracteres'),
    })
    // o acumulado nunca fica negativo
    .refine((campos) => campos.tipo === 'APORTE' || (converterValor(campos.valor) ?? 0) <= acumulado, {
      path: ['valor'],
      message: `O resgate não pode passar do acumulado (${formatarMoeda(acumulado)})`,
    })
}

type Campos = z.infer<ReturnType<typeof criarEsquema>>

type Props = {
  meta: MetaPoupanca
  aoConcluir: () => void
}

export function FormularioAporte({ meta, aoConcluir }: Props) {
  const hoje = hojeEmSaoPaulo()
  const registrar = useRegistrarAporte()
  // concluída é marco permanente: só aceita resgates
  const somenteResgate = meta.status === 'CONCLUIDA'

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<Campos>({
    resolver: zodResolver(criarEsquema(hoje, meta.acumulado)),
    defaultValues: { tipo: somenteResgate ? 'RESGATE' : 'APORTE', valor: '', data: hoje, observacao: '' },
  })
  const tipo = useWatch({ control, name: 'tipo' })

  function enviar(campos: Campos) {
    const valor = converterValor(campos.valor) ?? 0
    registrar.mutate(
      {
        id: meta.id,
        dados: { valor: campos.tipo === 'RESGATE' ? -valor : valor, data: campos.data, observacao: campos.observacao },
      },
      { onSuccess: aoConcluir },
    )
  }

  const titulo = tipo === 'RESGATE' ? 'Registrar resgate' : 'Registrar aporte'

  return (
    <form noValidate aria-label={titulo} onSubmit={handleSubmit(enviar)} className="flex flex-col gap-3 border-t border-borda pt-3">
      {!somenteResgate && (
        <fieldset className="flex gap-4 text-sm font-semibold">
          <legend className="sr-only">Tipo de lançamento</legend>
          <label className="flex min-h-11 items-center gap-2">
            <input type="radio" value="APORTE" {...register('tipo')} className="size-4 accent-destaque-escuro" />
            Aporte
          </label>
          <label className="flex min-h-11 items-center gap-2">
            <input type="radio" value="RESGATE" {...register('tipo')} className="size-4 accent-destaque-escuro" />
            Resgate
          </label>
        </fieldset>
      )}

      <div className="grid grid-cols-2 gap-3">
        <Campo rotulo="Valor" erro={errors.valor?.message}>
          <input type="text" inputMode="decimal" placeholder="0,00" {...register('valor')} className={classeCampo(!!errors.valor)} />
        </Campo>
        <Campo rotulo="Data" erro={errors.data?.message}>
          <input type="date" max={hoje} {...register('data')} className={classeCampo(!!errors.data)} />
        </Campo>
      </div>
      <Campo rotulo="Observação (opcional)" erro={errors.observacao?.message}>
        <input type="text" {...register('observacao')} className={classeCampo(!!errors.observacao)} />
      </Campo>

      {registrar.error && <Alerta>{mensagemDeErro(registrar.error)}</Alerta>}

      <div className="flex gap-2">
        <Botao variante="secundario" onClick={aoConcluir}>Voltar</Botao>
        <Botao type="submit" disabled={registrar.isPending}>{titulo}</Botao>
      </div>
    </form>
  )
}
