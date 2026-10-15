import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { MetaPoupanca } from '../../api/metasPoupanca'
import { Alerta, Botao, Campo } from '../../componentes/ui'
import { classeCampo, classeFormulario } from '../../componentes/estilos'
import { useSalvarMetaPoupanca } from '../../consultas/metas'
import { mensagemDeErro } from '../../lib/erros'
import { valorPositivo } from '../../lib/esquemas'
import { converterValor, hojeEmSaoPaulo, valorParaCampo } from '../../lib/formatacao'

// mesmas regras do CriarMetaPoupancaRequest da API
function criarEsquema(hoje: string) {
  return z.object({
    nome: z.string().trim().min(1, 'Nome é obrigatório').max(100, 'Nome deve ter no máximo 100 caracteres'),
    valorAlvo: valorPositivo(),
    prazo: z.string().refine((prazo) => prazo === '' || prazo >= hoje, 'Prazo não pode estar no passado'),
  })
}

type Campos = z.infer<ReturnType<typeof criarEsquema>>

type Props = {
  meta?: MetaPoupanca
  aoConcluir: () => void
}

export function FormularioMetaPoupanca({ meta, aoConcluir }: Props) {
  const hoje = hojeEmSaoPaulo()
  const salvar = useSalvarMetaPoupanca()
  const titulo = meta ? `Editar ${meta.nome}` : 'Nova meta de poupança'

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<Campos>({
    resolver: zodResolver(criarEsquema(hoje)),
    defaultValues: {
      nome: meta?.nome ?? '',
      valorAlvo: meta ? valorParaCampo(meta.valorAlvo) : '',
      prazo: meta?.prazo ?? '',
    },
  })

  function enviar(campos: Campos) {
    const dados = { nome: campos.nome, valorAlvo: converterValor(campos.valorAlvo) ?? 0, prazo: campos.prazo || null }
    salvar.mutate({ id: meta?.id, dados }, { onSuccess: aoConcluir })
  }

  return (
    <form
      noValidate
      aria-label={titulo}
      onSubmit={handleSubmit(enviar)}
      className={classeFormulario}
    >
      <h3 className="text-base font-bold">{titulo}</h3>

      <div className="grid gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(180px,1fr))]">
        <Campo rotulo="Nome" erro={errors.nome?.message}>
          <input type="text" placeholder="Ex.: Viagem ao Japão" {...register('nome')} className={classeCampo(!!errors.nome)} />
        </Campo>
        <Campo rotulo="Valor alvo" erro={errors.valorAlvo?.message}>
          <input type="text" inputMode="decimal" placeholder="0,00" {...register('valorAlvo')} className={classeCampo(!!errors.valorAlvo)} />
        </Campo>
        <Campo rotulo="Prazo (opcional)" erro={errors.prazo?.message}>
          <input type="date" min={hoje} {...register('prazo')} className={classeCampo(!!errors.prazo)} />
        </Campo>
      </div>

      {meta && (
        <p className="text-[13px] text-texto-suave">Reduzir o alvo até o valor já guardado conclui a meta.</p>
      )}

      {salvar.error && <Alerta>{mensagemDeErro(salvar.error)}</Alerta>}

      <div className="flex justify-end gap-2.5">
        <Botao variante="secundario" onClick={aoConcluir}>Cancelar</Botao>
        <Botao type="submit" disabled={salvar.isPending}>{salvar.isPending ? 'Salvando…' : 'Salvar'}</Botao>
      </div>
    </form>
  )
}
