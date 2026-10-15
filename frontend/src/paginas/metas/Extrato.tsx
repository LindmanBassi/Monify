import { useState } from 'react'
import { Botao } from '../../componentes/ui'
import { useAportes } from '../../consultas/metas'
import { mensagemDeErro } from '../../lib/erros'
import { formatarData, formatarMoeda } from '../../lib/formatacao'

// aportes são imutáveis: correção é feita com um lançamento contrário
export function Extrato({ metaId, nome }: { metaId: string; nome: string }) {
  const [pagina, setPagina] = useState(0)
  const consulta = useAportes(metaId, pagina)
  const resultado = consulta.data

  return (
    <div className="flex flex-col gap-2 border-t border-borda pt-3">
      <h4 className="text-[13px] font-bold">Extrato</h4>
      {consulta.isPending && <p role="status" className="text-[13px] text-texto-suave">Carregando extrato…</p>}
      {consulta.isError && <p role="alert" className="text-[13px] text-erro-texto">{mensagemDeErro(consulta.error)}</p>}
      {resultado && resultado.conteudo.length === 0 && (
        <p className="text-[13px] text-texto-suave">Nenhum lançamento ainda.</p>
      )}
      {resultado && resultado.conteudo.length > 0 && (
        <>
          <ul aria-label={`Extrato de ${nome}`} className="flex flex-col">
            {resultado.conteudo.map((aporte) => (
              <li key={aporte.id} className="flex items-center justify-between gap-3 border-t border-borda py-2 text-[13px] first:border-t-0">
                <span className="flex min-w-0 flex-col">
                  <span className="font-semibold">{aporte.valor >= 0 ? 'Aporte' : 'Resgate'} · {formatarData(aporte.data)}</span>
                  {aporte.observacao && <span className="truncate text-texto-suave">{aporte.observacao}</span>}
                </span>
                <span className={`font-bold whitespace-nowrap ${aporte.valor >= 0 ? 'text-receita' : ''}`}>
                  {aporte.valor >= 0 ? '+ ' : '− '}{formatarMoeda(Math.abs(aporte.valor))}
                </span>
              </li>
            ))}
          </ul>
          {resultado.totalPaginas > 1 && (
            <div className="flex gap-2">
              <Botao variante="secundario" disabled={pagina === 0} onClick={() => setPagina(pagina - 1)}>Mais recentes</Botao>
              <Botao variante="secundario" disabled={pagina + 1 >= resultado.totalPaginas} onClick={() => setPagina(pagina + 1)}>Mais antigos</Botao>
            </div>
          )}
        </>
      )}
    </div>
  )
}
