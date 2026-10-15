import { CabecalhoPagina } from '../../componentes/ui'
import { MetasEconomia } from './MetasEconomia'
import { MetasPoupanca } from './MetasPoupanca'

export function Metas() {
  return (
    <>
      <CabecalhoPagina
        sobretitulo="Seu próximo capítulo"
        titulo="Metas"
        descricao="Transforme seus planos em conquistas, um passo de cada vez."
      />
      <MetasEconomia />
      <MetasPoupanca />
    </>
  )
}
