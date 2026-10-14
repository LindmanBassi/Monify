export function classeCampo(comErro = false) {
  return `h-11 w-full rounded-[10px] border bg-superficie px-3 text-sm font-medium outline-none focus:border-destaque focus:ring-2 focus:ring-destaque/20 ${
    comErro ? 'border-erro-texto' : 'border-borda-campo'
  }`
}
