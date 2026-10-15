import type { StatusOrcamento } from '../../api/orcamentos'
import type { Tom } from '../../componentes/ui'

export const statusOrcamento: Record<StatusOrcamento, { rotulo: string; tom: Tom }> = {
  DENTRO_DO_LIMITE: { rotulo: 'Dentro do limite', tom: 'sucesso' },
  PROXIMO_DO_LIMITE: { rotulo: 'Próximo do limite', tom: 'atencao' },
  ESTOURADO: { rotulo: 'Acima do limite', tom: 'perigo' },
}
