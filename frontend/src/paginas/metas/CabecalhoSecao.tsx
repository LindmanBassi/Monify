import type { ReactNode } from 'react'

type Props = {
  id: string
  titulo: string
  subtitulo: string
  acoes?: ReactNode
}

export function CabecalhoSecao({ id, titulo, subtitulo, acoes }: Props) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-3">
      <div className="flex flex-col gap-1">
        <h2 id={id} className="text-lg font-semibold">{titulo}</h2>
        <p className="text-xs text-destaque-escuro">{subtitulo}</p>
      </div>
      {acoes && <div className="flex flex-wrap items-center gap-3">{acoes}</div>}
    </div>
  )
}
