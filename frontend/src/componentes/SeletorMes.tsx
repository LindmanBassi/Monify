import { ChevronLeft, ChevronRight } from 'lucide-react'
import { formatarMes, somarMeses } from '../lib/formatacao'

type Props = {
  mes: string
  aoMudar: (mes: string) => void
}

export function SeletorMes({ mes, aoMudar }: Props) {
  return (
    <div className="flex h-11 items-center rounded-[10px] border border-borda bg-superficie shadow-sm">
      <button
        type="button"
        aria-label="Mês anterior"
        onClick={() => aoMudar(somarMeses(mes, -1))}
        className="flex h-full w-10 cursor-pointer items-center justify-center rounded-l-[10px] text-texto-medio hover:bg-fundo"
      >
        <ChevronLeft size={17} aria-hidden="true" />
      </button>
      <span aria-live="polite" className="min-w-32 text-center text-[13px] font-semibold first-letter:uppercase">
        {formatarMes(mes)}
      </span>
      <button
        type="button"
        aria-label="Próximo mês"
        onClick={() => aoMudar(somarMeses(mes, 1))}
        className="flex h-full w-10 cursor-pointer items-center justify-center rounded-r-[10px] text-texto-medio hover:bg-fundo"
      >
        <ChevronRight size={17} aria-hidden="true" />
      </button>
    </div>
  )
}
