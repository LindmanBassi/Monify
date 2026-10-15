import type { StatusMetaEconomia, TipoReferencia } from '../../api/metasEconomia'
import type { StatusMetaPoupanca } from '../../api/metasPoupanca'
import type { Tom } from '../../componentes/ui'

export const statusMetaEconomia: Record<StatusMetaEconomia, { rotulo: string; tom: Tom }> = {
  AGUARDANDO_INICIO: { rotulo: 'Aguardando início', tom: 'atencao' },
  EM_ANDAMENTO: { rotulo: 'Em andamento', tom: 'info' },
  ATINGIDA: { rotulo: 'Atingida', tom: 'sucesso' },
  NAO_ATINGIDA: { rotulo: 'Não atingida', tom: 'perigo' },
  CANCELADA: { rotulo: 'Cancelada', tom: 'neutro' },
}

export const tiposReferencia: { tipo: TipoReferencia; rotulo: string; descricao: string }[] = [
  { tipo: 'ORCAMENTO', rotulo: 'Orçamento do mês', descricao: 'O limite do orçamento da categoria no mês da meta.' },
  { tipo: 'MES_ANTERIOR', rotulo: 'Gasto do mês anterior', descricao: 'Quanto você gastou na categoria no mês anterior.' },
  { tipo: 'MEDIA_TRES_MESES', rotulo: 'Média dos últimos 3 meses', descricao: 'Exige 3 meses completos de histórico.' },
]

export function rotuloReferencia(tipo: TipoReferencia): string {
  return tiposReferencia.find((referencia) => referencia.tipo === tipo)?.rotulo ?? tipo
}

export const statusMetaPoupanca: Record<StatusMetaPoupanca, { rotulo: string; tom: Tom }> = {
  ATIVA: { rotulo: 'Ativa', tom: 'info' },
  CONCLUIDA: { rotulo: 'Concluída', tom: 'sucesso' },
  CANCELADA: { rotulo: 'Cancelada', tom: 'neutro' },
}
