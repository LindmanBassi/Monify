export function Logo({ tamanho = 34, escuro = false }: { tamanho?: number; escuro?: boolean }) {
  return (
    <svg width={tamanho} height={tamanho} viewBox="0 0 36 36" aria-hidden="true">
      <rect x="0.5" y="0.5" width="35" height="35" rx="9" fill={escuro ? '#232D28' : '#141A17'} stroke={escuro ? '#3A4640' : 'none'} />
      <path d="M10 25V12l8 7.5 8-7.5v13" fill="none" stroke="#5FDDA0" strokeWidth="2.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

export function Marca({ tamanho = 34, escuro = false }: { tamanho?: number; escuro?: boolean }) {
  return (
    <span className="flex items-center gap-2.5">
      <Logo tamanho={tamanho} escuro={escuro} />
      <span className="text-[26px] leading-none font-extrabold tracking-tight">
        monify<span className="text-destaque">.</span>
      </span>
    </span>
  )
}
