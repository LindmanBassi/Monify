import type { Categoria } from '../api/categorias'

const grupos = [
  { tipo: 'DESPESA', rotulo: 'Despesas', classe: 'text-despesa' },
  { tipo: 'RECEITA', rotulo: 'Receitas', classe: 'text-receita' },
] as const

// opções de um <select> agrupadas por tipo, cada grupo com a cor do tipo
export function OpcoesPorTipo({ categorias }: { categorias: Categoria[] }) {
  return grupos.map(({ tipo, rotulo, classe }) => (
    <optgroup key={tipo} label={rotulo} className={`font-bold ${classe}`}>
      {categorias
        .filter((categoria) => categoria.tipo === tipo)
        .map((categoria) => (
          <option key={categoria.id} value={categoria.id} className={`font-medium ${classe}`}>
            {categoria.nome}{categoria.arquivada ? ' (arquivada)' : ''}
          </option>
        ))}
    </optgroup>
  ))
}
