import { useEffect, useState } from 'react'

export function useValorAtrasado<T>(valor: T, milissegundos = 300): T {
  const [atrasado, setAtrasado] = useState(valor)

  useEffect(() => {
    const temporizador = setTimeout(() => setAtrasado(valor), milissegundos)
    return () => clearTimeout(temporizador)
  }, [valor, milissegundos])

  return atrasado
}
