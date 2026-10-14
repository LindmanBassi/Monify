import type { ReactNode } from 'react'
type PropsIcone = {
  tamanho?: number
}

function Svg({ tamanho = 18, children }: PropsIcone & { children: ReactNode }) {
  return (
    <svg
      width={tamanho}
      height={tamanho}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {children}
    </svg>
  )
}

export function Logo({ tamanho = 30 }: PropsIcone) {
  return (
    <svg width={tamanho} height={tamanho} viewBox="0 0 36 36" aria-hidden="true">
      <rect width="36" height="36" rx="10" fill="#4338CA" />
      <path d="M9 25V12l9 8 9-8v13" fill="none" stroke="#FFFFFF" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

export function IconeDashboard(props: PropsIcone) {
  return <Svg {...props}><path d="M4 13h6V4H4zM14 20h6v-9h-6zM4 20h6v-4H4zM14 8h6V4h-6z" /></Svg>
}

export function IconeTransacoes(props: PropsIcone) {
  return <Svg {...props}><path d="M7 7h13M17 4l3 3-3 3M17 17H4M7 14l-3 3 3 3" /></Svg>
}

export function IconeCategorias(props: PropsIcone) {
  return <Svg {...props}><path d="M4 5h7v7H4zM13 5h7v7h-7zM4 14h7v6H4zM13 14h7v6h-7z" /></Svg>
}

export function IconeOrcamentos(props: PropsIcone) {
  return <Svg {...props}><path d="M12 3v9l7 4M21 12a9 9 0 1 1-9-9" /></Svg>
}

export function IconeMetas(props: PropsIcone) {
  return (
    <Svg {...props}>
      <circle cx="12" cy="12" r="8" />
      <circle cx="12" cy="12" r="3.5" />
    </Svg>
  )
}

export function IconeConfirmar(props: PropsIcone) {
  return <Svg {...props}><path d="M5 12l5 5 9-10" /></Svg>
}

export function IconeAlerta(props: PropsIcone) {
  return (
    <Svg {...props}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v6M12 16.5v.5" />
    </Svg>
  )
}
