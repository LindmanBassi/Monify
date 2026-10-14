type Valor = string | number | boolean | null | undefined

export function comParametros(caminho: string, parametros: Record<string, Valor>): string {
  const busca = new URLSearchParams()
  Object.entries(parametros).forEach(([nome, valor]) => {
    if (valor !== undefined && valor !== null && valor !== '') {
      busca.set(nome, String(valor))
    }
  })
  const texto = busca.toString()
  return texto ? `${caminho}?${texto}` : caminho
}
