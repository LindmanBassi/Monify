export function mensagemDeErro(erro: unknown): string | null {
  if (!erro) {
    return null
  }
  return erro instanceof Error ? erro.message : 'Não foi possível concluir a operação'
}
