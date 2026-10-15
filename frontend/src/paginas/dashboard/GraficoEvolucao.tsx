import { Bar, BarChart, CartesianGrid, Tooltip, XAxis, YAxis } from 'recharts'
import type { ResumoMensal } from '../../api/dashboard'
import { formatarMes, formatarMesAbreviado, formatarMoeda } from '../../lib/formatacao'

const COR_RECEITAS = '#10A36F'
const COR_DESPESAS = '#1C2420'

const compacto = new Intl.NumberFormat('pt-BR', { notation: 'compact', maximumFractionDigits: 1 })

export function GraficoEvolucao({ meses }: { meses: ResumoMensal[] }) {
  const dados = meses.map((mes) => ({ ...mes, rotulo: formatarMesAbreviado(mes.mes).replace('.', '') }))
  const periodo = meses.length > 0 ? `de ${formatarMes(meses[0].mes)} a ${formatarMes(meses[meses.length - 1].mes)}` : ''

  return (
    <>
      <div className="flex gap-5 text-xs font-medium text-texto-medio">
        <Legenda cor={COR_RECEITAS} rotulo="Receitas" />
        <Legenda cor={COR_DESPESAS} rotulo="Despesas" />
      </div>
      <div role="img" aria-label={`Gráfico de barras: receitas e despesas ${periodo}`}>
        <BarChart responsive data={dados} style={{ width: '100%', height: 260 }} margin={{ top: 8, right: 4, bottom: 0, left: 0 }} barGap={8}>
          <CartesianGrid vertical={false} stroke="#E4E8E6" strokeDasharray="4 4" />
          <XAxis dataKey="rotulo" tickLine={false} axisLine={false} tick={{ fill: '#5C6661', fontSize: 12 }} tickMargin={10} />
          <YAxis width={52} tickCount={4} tickLine={false} axisLine={false} tick={{ fill: '#5C6661', fontSize: 12 }} tickFormatter={(valor: number) => compacto.format(valor)} />
          <Tooltip cursor={{ fill: '#F4F6F5' }} formatter={(valor) => formatarMoeda(Number(valor))} />
          <Bar dataKey="receitas" name="Receitas" fill={COR_RECEITAS} radius={[5, 5, 0, 0]} maxBarSize={24} />
          <Bar dataKey="despesas" name="Despesas" fill={COR_DESPESAS} radius={[5, 5, 0, 0]} maxBarSize={24} />
        </BarChart>
      </div>

      <table className="sr-only">
        <caption>Receitas, despesas e saldo por mês</caption>
        <thead>
          <tr><th scope="col">Mês</th><th scope="col">Receitas</th><th scope="col">Despesas</th><th scope="col">Saldo</th></tr>
        </thead>
        <tbody>
          {meses.map((mes) => (
            <tr key={mes.mes}>
              <th scope="row">{formatarMes(mes.mes)}</th>
              <td>{formatarMoeda(mes.receitas)}</td>
              <td>{formatarMoeda(mes.despesas)}</td>
              <td>{formatarMoeda(mes.saldo)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  )
}

function Legenda({ cor, rotulo }: { cor: string; rotulo: string }) {
  return (
    <span className="flex items-center gap-1.5">
      <span aria-hidden="true" className="size-2.5 rounded-sm" style={{ backgroundColor: cor }} />
      {rotulo}
    </span>
  )
}
