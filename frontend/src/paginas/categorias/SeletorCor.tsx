import { PALETA } from './paleta'

type Props = {
  valor: string
  aoMudar: (cor: string) => void
}

export function SeletorCor({ valor, aoMudar }: Props) {
  const selecionada = valor.toUpperCase()

  return (
    <div role="radiogroup" aria-label="Cor" className="flex flex-wrap items-center gap-2">
      {PALETA.map((cor) => (
        <button
          key={cor}
          type="button"
          role="radio"
          aria-checked={selecionada === cor}
          aria-label={`Cor ${cor}`}
          onClick={() => aoMudar(cor)}
          className={`size-8 cursor-pointer rounded-full ring-offset-2 ${selecionada === cor ? 'ring-2 ring-tinta' : ''}`}
          style={{ backgroundColor: cor }}
        />
      ))}
      <label className="flex items-center gap-2 text-[13px] font-semibold text-texto-medio">
        <input
          type="color"
          value={valor}
          onChange={(evento) => aoMudar(evento.target.value.toUpperCase())}
          className="size-8 cursor-pointer rounded-full border border-borda-campo bg-superficie p-0.5"
        />
        Outra cor
      </label>
    </div>
  )
}
