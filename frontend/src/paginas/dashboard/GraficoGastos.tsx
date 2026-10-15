import { Pie, PieChart, Tooltip } from 'recharts'
import type { TotalCategoria } from '../../api/dashboard'
import { formatarMoeda, formatarPercentual } from '../../lib/formatacao'

type Props = {
  gastos: TotalCategoria[]
  total: number
}

export function GraficoGastos({ gastos, total }: Props) {
  if (gastos.length === 0) {
    return <p className="text-sm text-texto-suave">Nenhuma despesa neste mês.</p>
  }

  const fatias = gastos.map(({ categoria, total }) => ({ nome: categoria.nome, total, fill: categoria.cor }))

  return (
    <div className="flex flex-col gap-6">
      <div role="img" aria-label="Gráfico de rosca dos gastos por categoria" className="relative mx-auto size-[180px]">
        <PieChart responsive style={{ width: '100%', height: '100%' }}>
          <Pie
            data={fatias}
            dataKey="total"
            nameKey="nome"
            innerRadius="80%"
            outerRadius="100%"
            paddingAngle={gastos.length > 1 ? 2 : 0}
            stroke="none"
            isAnimationActive={false}
          />
          <Tooltip formatter={(valor) => formatarMoeda(Number(valor))} />
        </PieChart>
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-xs text-texto-suave">Total de despesas</span>
          <span className="text-lg font-bold tracking-tight">{formatarMoeda(total)}</span>
        </div>
      </div>

      <ul aria-label="Gastos por categoria" className="flex flex-col gap-3.5">
        {gastos.map(({ categoria, total, percentual }) => (
          <li key={categoria.id} className="flex items-center gap-2.5 text-[13px]">
            <span aria-hidden="true" className="size-2.5 flex-none rounded-sm" style={{ backgroundColor: categoria.cor }} />
            <span className="flex-1 truncate">{categoria.nome}</span>
            <span className="font-bold">{formatarMoeda(total)}</span>
            <span className="w-10 text-right text-xs text-texto-suave">{formatarPercentual(percentual)}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
