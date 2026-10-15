import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { Categoria } from '../../api/categorias'
import { Alerta, Botao, Campo } from '../../componentes/ui'
import { classeCampo, classeFormulario } from '../../componentes/estilos'
import { useDefinirOrcamento } from '../../consultas/orcamentos'
import { mensagemDeErro } from '../../lib/erros'
import { valorPositivo } from '../../lib/esquemas'
import { converterValor, formatarMes } from '../../lib/formatacao'

const esquema = z.object({
  categoriaId: z.string().min(1, 'Categoria é obrigatória'),
  valorLimite: valorPositivo(),
})

type Campos = z.infer<typeof esquema>

type Props = {
  mes: string
  // categorias de despesa ativas que ainda não têm orçamento no mês
  categorias: Categoria[]
  aoConcluir: () => void
}

export function FormularioOrcamento({ mes, categorias, aoConcluir }: Props) {
  const definir = useDefinirOrcamento()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<Campos>({
    resolver: zodResolver(esquema),
    defaultValues: { categoriaId: '', valorLimite: '' },
  })

  function enviar(campos: Campos) {
    definir.mutate(
      { categoriaId: campos.categoriaId, mesReferencia: mes, valorLimite: converterValor(campos.valorLimite) ?? 0 },
      { onSuccess: aoConcluir },
    )
  }

  return (
    <form
      noValidate
      aria-label="Definir orçamento"
      onSubmit={handleSubmit(enviar)}
      className={classeFormulario}
    >
      <h2 className="text-base font-bold">Definir orçamento de {formatarMes(mes)}</h2>

      {categorias.length === 0 ? (
        <p className="text-sm text-texto-suave">Todas as categorias de despesa já têm orçamento neste mês.</p>
      ) : (
        <div className="grid gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(200px,1fr))]">
          <Campo rotulo="Categoria" erro={errors.categoriaId?.message}>
            <select {...register('categoriaId')} className={classeCampo(!!errors.categoriaId)}>
              <option value="">Selecione</option>
              {categorias.map((categoria) => (
                <option key={categoria.id} value={categoria.id}>{categoria.nome}</option>
              ))}
            </select>
          </Campo>
          <Campo rotulo="Limite do mês" erro={errors.valorLimite?.message}>
            <input type="text" inputMode="decimal" placeholder="0,00" {...register('valorLimite')} className={classeCampo(!!errors.valorLimite)} />
          </Campo>
        </div>
      )}

      {definir.error && <Alerta>{mensagemDeErro(definir.error)}</Alerta>}

      <div className="flex justify-end gap-2.5">
        <Botao variante="secundario" onClick={aoConcluir}>Cancelar</Botao>
        {categorias.length > 0 && (
          <Botao type="submit" disabled={definir.isPending}>{definir.isPending ? 'Salvando…' : 'Salvar'}</Botao>
        )}
      </div>
    </form>
  )
}
