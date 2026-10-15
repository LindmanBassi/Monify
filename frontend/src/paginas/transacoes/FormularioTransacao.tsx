import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import type { Categoria } from '../../api/categorias'
import { ErroApi } from '../../api/cliente'
import type { Transacao } from '../../api/transacoes'
import { OpcoesPorTipo } from '../../componentes/OpcoesPorTipo'
import { Alerta, Botao, Campo } from '../../componentes/ui'
import { classeCampo, classeFormulario } from '../../componentes/estilos'
import { mensagemDeErro } from '../../lib/erros'
import { valorPositivo } from '../../lib/esquemas'
import { useSalvarTransacao } from '../../consultas/transacoes'
import { converterValor, hojeEmSaoPaulo, valorParaCampo } from '../../lib/formatacao'

function criarEsquema(hoje: string) {
  return z.object({
    categoriaId: z.string().min(1, 'Categoria é obrigatória'),
    valor: valorPositivo(),
    dataOcorrencia: z
      .string()
      .min(1, 'Data da transação é obrigatória')
      .refine((data) => data <= hoje, 'Data da transação não pode ser futura'),
    descricao: z.string().max(255, 'Descrição deve ter no máximo 255 caracteres'),
  })
}

type Campos = z.infer<ReturnType<typeof criarEsquema>>

function corDoTipo(categoria?: Categoria) {
  if (!categoria) {
    return ''
  }
  return categoria.tipo === 'RECEITA' ? 'text-receita' : 'text-despesa'
}

type Props = {
  transacao?: Transacao
  categorias: Categoria[]
  aoConcluir: () => void
}

export function FormularioTransacao({ transacao, categorias, aoConcluir }: Props) {
  const hoje = hojeEmSaoPaulo()
  const salvar = useSalvarTransacao()

  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<Campos>({
    resolver: zodResolver(criarEsquema(hoje)),
    defaultValues: {
      categoriaId: transacao?.categoria.id ?? '',
      valor: transacao ? valorParaCampo(transacao.valor) : '',
      dataOcorrencia: transacao?.dataOcorrencia ?? hoje,
      descricao: transacao?.descricao ?? '',
    },
  })

  // categoria arquivada não recebe transações novas, mas a atual pode ser mantida na edição
  const disponiveis = categorias.filter(
    (categoria) => !categoria.arquivada || categoria.id === transacao?.categoria.id,
  )
  const categoriaId = useWatch({ control, name: 'categoriaId' })
  const selecionada = categorias.find((categoria) => categoria.id === categoriaId)

  function enviar(campos: Campos) {
    const dados = {
      categoriaId: campos.categoriaId,
      valor: converterValor(campos.valor) ?? 0,
      descricao: campos.descricao,
      dataOcorrencia: campos.dataOcorrencia,
    }
    salvar.mutate(
      { id: transacao?.id, dados },
      {
        onSuccess: aoConcluir,
        onError: (erro) => {
          if (erro instanceof ErroApi) {
            erro.camposInvalidos.forEach(({ campo, mensagem }) => setError(campo as keyof Campos, { message: mensagem }))
          }
        },
      },
    )
  }

  const errosPorCampo = salvar.error instanceof ErroApi && salvar.error.camposInvalidos.length > 0
  const erroGeral = errosPorCampo ? null : mensagemDeErro(salvar.error)

  return (
    <form
      noValidate
      aria-label={transacao ? 'Editar transação' : 'Nova transação'}
      onSubmit={handleSubmit(enviar)}
      className={classeFormulario}
    >
      <h2 className="text-base font-bold">{transacao ? 'Editar transação' : 'Nova transação'}</h2>

      <div className="grid gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(180px,1fr))]">
        <Campo rotulo="Categoria" erro={errors.categoriaId?.message}>
          <select {...register('categoriaId')} className={`${classeCampo(!!errors.categoriaId)} ${corDoTipo(selecionada)}`}>
            <option value="">Selecione</option>
            <OpcoesPorTipo categorias={disponiveis} />
          </select>
        </Campo>
        <Campo rotulo="Valor" erro={errors.valor?.message}>
          <input type="text" inputMode="decimal" placeholder="0,00" {...register('valor')} className={classeCampo(!!errors.valor)} />
        </Campo>
        <Campo rotulo="Data" erro={errors.dataOcorrencia?.message}>
          <input type="date" max={hoje} {...register('dataOcorrencia')} className={classeCampo(!!errors.dataOcorrencia)} />
        </Campo>
        <Campo rotulo="Descrição (opcional)" erro={errors.descricao?.message}>
          <input type="text" placeholder="Ex.: mercado do mês" {...register('descricao')} className={classeCampo(!!errors.descricao)} />
        </Campo>
      </div>

      <p className="text-[13px] text-texto-suave">
        {selecionada ? (
          <>
            Será registrada como{' '}
            <strong className={corDoTipo(selecionada)}>
              {selecionada.tipo === 'RECEITA' ? 'receita' : 'despesa'}
            </strong>
            , o tipo da categoria escolhida.
          </>
        ) : (
          'O tipo (receita ou despesa) vem da categoria escolhida.'
        )}
      </p>

      {erroGeral && <Alerta>{erroGeral}</Alerta>}

      <div className="flex justify-end gap-2.5">
        <Botao variante="secundario" onClick={aoConcluir}>Cancelar</Botao>
        <Botao type="submit" disabled={salvar.isPending}>{salvar.isPending ? 'Salvando…' : 'Salvar'}</Botao>
      </div>
    </form>
  )
}
