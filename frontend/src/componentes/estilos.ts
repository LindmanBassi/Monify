export function classeCampo(comErro = false) {
  return `h-11 w-full min-w-0 rounded-[10px] border bg-superficie px-3 text-sm font-medium outline-none focus:border-destaque-escuro focus:ring-2 focus:ring-destaque/40 ${
    comErro ? 'border-erro-texto' : 'border-borda'
  }`
}

export const classeFormulario = 'flex flex-col gap-4 rounded-2xl border border-destaque bg-superficie p-6 shadow-sm'

export type Variante = 'primario' | 'secundario' | 'texto'

export const estilosBotao: Record<Variante, string> = {
  primario: 'bg-destaque text-tinta shadow-sm hover:bg-destaque-hover',
  secundario: 'border border-borda bg-superficie text-tinta shadow-sm hover:bg-fundo',
  texto: 'bg-transparent text-texto-medio hover:bg-fundo',
}

export const classeBotao = 'inline-flex h-11 cursor-pointer items-center justify-center gap-2 rounded-[10px] px-4 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-60'
