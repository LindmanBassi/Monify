import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { Categoria } from '../../api/categorias'
import type { MetaEconomia } from '../../api/metasEconomia'
import { Alerta, Botao, Campo } from '../../componentes/ui'
import { classeCampo, classeFormulario } from '../../componentes/estilos'
import { useCriarMetaEconomia, useEditarMetaEconomia } from '../../consultas/metas'
import { mensagemDeErro } from '../../lib/erros'
import { valorPositivo } from '../../lib/esquemas'
import { converterValor, formatarMes, valorParaCampo } from '../../lib/formatacao'
import { tiposReferencia } from './rotulos'

const esquema = z.object({
  categoriaId: z.string().min(1, 'Categoria é obrigatória'),
  tipoReferencia: z.enum(['ORCAMENTO', 'MES_ANTERIOR', 'MEDIA_TRES_MESES']),
  valorEconomia: valorPositivo(),
})

type Campos = z.infer<typeof esquema>

type Props = {
  mes: string
  // na criação: categorias de despesa ativas sem meta ativa no mês
  categorias: Categoria[]
  meta?: MetaEconomia
  aoConcluir: () => void
}

export function FormularioMetaEconomia({ mes, categorias, meta, aoConcluir }: Props) {
  const criar = useCriarMetaEconomia()
  const editar = useEditarMetaEconomia()
  const salvar = meta ? editar : criar
  const titulo = meta ? `Editar meta de ${meta.categoria.nome}` : `Nova meta de economia · ${formatarMes(mes)}`

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<Campos>({
    resolver: zodResolver(esquema),
    defaultValues: {
      categoriaId: meta?.categoria.id ?? '',
      tipoReferencia: meta?.tipoReferencia ?? 'MES_ANTERIOR',
      valorEconomia: meta ? valorParaCampo(meta.valorEconomia) : '',
    },
  })

  function enviar(campos: Campos) {
    const dados = { tipoReferencia: campos.tipoReferencia, valorEconomia: converterValor(campos.valorEconomia) ?? 0 }
    if (meta) {
      editar.mutate({ id: meta.id, dados }, { onSuccess: aoConcluir })
    } else {
      criar.mutate({ ...dados, categoriaId: campos.categoriaId, mesReferencia: mes }, { onSuccess: aoConcluir })
    }
  }

  const semCategorias = !meta && categorias.length === 0

  return (
    <form
      noValidate
      aria-label={titulo}
      onSubmit={handleSubmit(enviar)}
      className={classeFormulario}
    >
      <h3 className="text-base font-bold">{titulo}</h3>

      {semCategorias ? (
        <p className="text-sm text-texto-suave">Todas as categorias de despesa já têm meta ativa neste mês.</p>
      ) : (
        <>
          <div className="grid gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(200px,1fr))]">
            {!meta && (
              <Campo rotulo="Categoria" erro={errors.categoriaId?.message}>
                <select {...register('categoriaId')} className={classeCampo(!!errors.categoriaId)}>
                  <option value="">Selecione</option>
                  {categorias.map((categoria) => (
                    <option key={categoria.id} value={categoria.id}>{categoria.nome}</option>
                  ))}
                </select>
              </Campo>
            )}
            <Campo rotulo="Quanto economizar" erro={errors.valorEconomia?.message}>
              <input type="text" inputMode="decimal" placeholder="0,00" {...register('valorEconomia')} className={classeCampo(!!errors.valorEconomia)} />
            </Campo>
          </div>

          <fieldset className="flex flex-col gap-2">
            <legend className="mb-1.5 text-[13px] font-semibold">Economizar em relação a</legend>
            {tiposReferencia.map(({ tipo, rotulo, descricao }) => (
              <label key={tipo} className="flex min-h-11 items-start gap-2.5 text-sm">
                <input type="radio" value={tipo} {...register('tipoReferencia')} className="mt-1 size-4 accent-destaque-escuro" />
                <span className="flex flex-col">
                  <span className="font-semibold">{rotulo}</span>
                  <span className="text-[13px] text-texto-suave">{descricao}</span>
                </span>
              </label>
            ))}
          </fieldset>

          <p className="text-[13px] text-texto-suave">
            A referência é congelada quando o mês começa. Até lá a meta pode ser editada; depois, só cancelada.
          </p>
        </>
      )}

      {salvar.error && <Alerta>{mensagemDeErro(salvar.error)}</Alerta>}

      <div className="flex justify-end gap-2.5">
        <Botao variante="secundario" onClick={aoConcluir}>Cancelar</Botao>
        {!semCategorias && (
          <Botao type="submit" disabled={salvar.isPending}>{salvar.isPending ? 'Salvando…' : 'Salvar'}</Botao>
        )}
      </div>
    </form>
  )
}
