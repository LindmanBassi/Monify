import type { IconeCategoria } from '../../api/categorias'
import { ICONES } from '../../componentes/mapaIcones'

type Props = {
  valor: IconeCategoria
  cor: string
  aoMudar: (icone: IconeCategoria) => void
}

export function SeletorIcone({ valor, cor, aoMudar }: Props) {
  return (
    <div role="radiogroup" aria-label="Ícone" className="flex flex-wrap gap-2">
      {(Object.keys(ICONES) as IconeCategoria[]).map((icone) => {
        const { Desenho, rotulo } = ICONES[icone]
        const selecionado = valor === icone
        return (
          <button
            key={icone}
            type="button"
            role="radio"
            aria-checked={selecionado}
            aria-label={rotulo}
            title={rotulo}
            onClick={() => aoMudar(icone)}
            className={`flex size-11 cursor-pointer items-center justify-center rounded-xl border ${
              selecionado ? 'border-transparent ring-2 ring-tinta' : 'border-borda text-texto-medio hover:bg-fundo'
            }`}
            style={selecionado ? { backgroundColor: `${cor}1F`, color: cor } : undefined}
          >
            <Desenho size={19} aria-hidden="true" />
          </button>
        )
      })}
    </div>
  )
}
