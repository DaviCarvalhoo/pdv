import { useEffect, useState } from 'react';
import { api, baixarArquivo, csv, type CurvaAbc } from '../lib/api';
import { useAvisos } from '../lib/contexto';
import { data, diasAtras, hoje, moeda, pct, qtd } from '../lib/format';

const EXPLICACAO: Record<'A' | 'B' | 'C', string> = {
  A: 'Fazem 80% do faturamento. Nunca podem faltar na prateleira.',
  B: 'Os próximos 15%. Acompanhe o giro.',
  C: 'Os últimos 5%. Candidatos a promoção ou a sair do mix.',
};

export default function PaginaRelatorios() {
  const { erro } = useAvisos();
  const [inicio, setInicio] = useState(diasAtras(29));
  const [fim, setFim] = useState(hoje());
  const [abc, setAbc] = useState<CurvaAbc | null>(null);
  const [classe, setClasse] = useState<'A' | 'B' | 'C' | ''>('');

  useEffect(() => {
    api.curvaAbc(inicio, fim).then(setAbc).catch(erro);
  }, [inicio, fim, erro]);

  const itens = abc?.itens.filter((i) => !classe || i.classe === classe) ?? [];

  const exportar = () => {
    if (!abc) return;
    baixarArquivo(
      `curva-abc-${inicio}-a-${fim}.csv`,
      csv([
        ['Posição', 'Produto', 'Quantidade', 'Faturamento', '% do total', '% acumulado', 'Classe'],
        ...abc.itens.map((i, n) => [n + 1, i.nome, i.quantidade, i.valor, i.percentual, i.acumulado, i.classe]),
      ]),
    );
  };

  return (
    <div className="pagina pagina--larga">
      <header className="pagina__topo">
        <div>
          <p className="sobretitulo">Relatórios</p>
          <h1>Curva ABC de produtos</h1>
        </div>
        <div className="pagina__acoes">
          <button className="botao botao--secundario" onClick={exportar} disabled={!abc?.itens.length}>
            Exportar para Excel (CSV)
          </button>
        </div>
      </header>

      <div className="filtros">
        <label>
          De
          <input type="date" value={inicio} max={fim} onChange={(e) => setInicio(e.target.value)} />
        </label>
        <label>
          Até
          <input type="date" value={fim} max={hoje()} onChange={(e) => setFim(e.target.value)} />
        </label>
        <div className="atalhos-periodo">
          {[7, 30, 90].map((d) => (
            <button key={d} className="chip" onClick={() => { setInicio(diasAtras(d - 1)); setFim(hoje()); }}>
              {d} dias
            </button>
          ))}
        </div>
      </div>

      {abc && (
        <>
          <div className="classes-abc">
            {(['A', 'B', 'C'] as const).map((c) => (
              <button key={c} className={`classe-abc classe-abc--${c.toLowerCase()} ${classe === c ? 'classe-abc--ativa' : ''}`} onClick={() => setClasse(classe === c ? '' : c)} aria-pressed={classe === c}>
                <span className="classe-abc__letra">{c}</span>
                <strong>{abc.produtosPorClasse[c]} produtos</strong>
                <small>{EXPLICACAO[c]}</small>
              </button>
            ))}
          </div>
          <p className="dica">
            {moeda(abc.total)} vendidos de {data(abc.inicio)} a {data(abc.fim)}.
          </p>
          <table className="tabela">
            <thead>
              <tr>
                <th>#</th>
                <th>Produto</th>
                <th className="tabela__num">Qtd</th>
                <th className="tabela__num">Faturamento</th>
                <th className="tabela__num">% do total</th>
                <th className="tabela__num">Acumulado</th>
                <th>Classe</th>
              </tr>
            </thead>
            <tbody>
              {itens.map((i) => (
                <tr key={i.produtoId}>
                  <td className="tabela__fraco">{abc.itens.indexOf(i) + 1}</td>
                  <td>{i.nome}</td>
                  <td className="tabela__num">{qtd(i.quantidade)}</td>
                  <td className="tabela__num">{moeda(i.valor)}</td>
                  <td className="tabela__num">{pct(i.percentual)}</td>
                  <td className="tabela__num tabela__fraco">
                    <span className="acumulado">
                      <span style={{ width: `${i.acumulado}%` }} />
                    </span>
                    {pct(i.acumulado)}
                  </td>
                  <td>
                    <span className={`selo selo--abc-${i.classe.toLowerCase()}`}>{i.classe}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {abc.itens.length === 0 && <p className="vazio-linha">Sem vendas no período.</p>}
        </>
      )}
    </div>
  );
}
