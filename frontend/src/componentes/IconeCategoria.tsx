import type { IconeCategoria as Icone } from '../api/categorias'
import { ICONES } from './mapaIcones'

type Props = {
  icone: Icone
  cor: string
  tamanho?: 'pequeno' | 'medio'
}

// ícone sobre um fundo claro na cor da categoria
export function SeloCategoria({ icone, cor, tamanho = 'medio' }: Props) {
  const { Desenho } = ICONES[icone] ?? ICONES.ETIQUETA
  const classe = tamanho === 'medio' ? 'size-11 rounded-xl' : 'size-9 rounded-lg'
  return (
    <span aria-hidden="true" className={`flex flex-none items-center justify-center ${classe}`} style={{ backgroundColor: `${cor}1F`, color: cor }}>
      <Desenho size={tamanho === 'medio' ? 20 : 17} strokeWidth={2} />
    </span>
  )
}
