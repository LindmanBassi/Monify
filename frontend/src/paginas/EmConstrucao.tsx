type Props = {
  titulo: string
  descricao: string
}

export function EmConstrucao({ titulo, descricao }: Props) {
  return (
    <section className="flex flex-col gap-2">
      <h1 className="text-[28px] font-extrabold tracking-tight">{titulo}</h1>
      <p className="max-w-xl text-sm text-texto-suave">{descricao}</p>
    </section>
  )
}
