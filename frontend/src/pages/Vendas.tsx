/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

import { useCallback, useEffect, useState } from 'react';
import Danfe from '../components/Danfe';
import Painel from '../components/Painel';
import { api, baixarArquivo, csv, type Danfe as DanfeDados, type Devolucao, type Historico, type Venda } from '../lib/api';
import { useAvisos, useCaixa } from '../lib/contexto';
import { dataHora, diasAtras, documento, hoje, moeda, nomeForma, nomeStatusVenda, parseValor, qtd } from '../lib/format';

export default function PaginaVendas() {
  const { erro } = useAvisos();
  const [filtro, setFiltro] = useState({ inicio: diasAtras(7), fim: hoje(), status: '', forma: '', caixaId: '' });
  const [pagina, setPagina] = useState(0);
  const [dados, setDados] = useState<Historico | null>(null);
  const [aberta, setAberta] = useState<number | null>(null);

  const carregar = useCallback(() => {
    api.historico({ ...filtro, page: pagina }).then(setDados).catch(erro);
  }, [filtro, pagina, erro]);
  useEffect(carregar, [carregar]);

  const muda = (k: keyof typeof filtro) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFiltro({ ...filtro, [k]: e.target.value });
    setPagina(0);
  };

  const vendas = dados?.vendas;

  /** Exporta todas as vendas do filtro (não só a página atual) em CSV para o Excel. */
  const exportar = async () => {
    try {
      const tudo = await api.historico({ ...filtro, page: 0, size: 5000 });
      baixarArquivo(
        `vendas-${filtro.inicio}-a-${filtro.fim}.csv`,
        csv([
          ['Venda', 'Data', 'Status', 'Operador', 'Cliente', 'Pagamento', 'Itens', 'Desconto', 'Total'],
          ...tudo.vendas.content.map((v) => [
            v.id, dataHora(v.dataFinalizacao ?? v.dataAbertura), nomeStatusVenda[v.status], v.operadorNome, v.clienteNome,
            v.formas.map((f) => nomeForma[f]).join(' + '), v.quantidadeItens, v.desconto, v.total,
          ]),
        ]),
      );
    } catch (e) {
      erro(e);
    }
  };

  return (
    <div className="pagina">
      <header className="pagina__topo">
        <div>
          <p className="sobretitulo">Histórico</p>
          <h1>Vendas</h1>
        </div>
        <div className="pagina__acoes">
          {dados && (
            <div className="gaveta">
              <span>{dados.totalizadores.quantidade} vendas no filtro</span>
              <strong>{moeda(dados.totalizadores.valorTotal)}</strong>
            </div>
          )}
          <button className="botao botao--secundario" onClick={exportar}>
            Exportar (CSV)
          </button>
        </div>
      </header>

      <div className="filtros">
        <label>
          De
          <input type="date" value={filtro.inicio} onChange={muda('inicio')} />
        </label>
        <label>
          Até
          <input type="date" value={filtro.fim} onChange={muda('fim')} />
        </label>
        <label>
          Status
          <select value={filtro.status} onChange={muda('status')}>
            <option value="">Todos</option>
            {Object.entries(nomeStatusVenda).map(([k, v]) => (
              <option key={k} value={k}>
                {v}
              </option>
            ))}
          </select>
        </label>
        <label>
          Pagamento
          <select value={filtro.forma} onChange={muda('forma')}>
            <option value="">Todos</option>
            {Object.entries(nomeForma).map(([k, v]) => (
              <option key={k} value={k}>
                {v}
              </option>
            ))}
          </select>
        </label>
        <label>
          Caixa
          <input inputMode="numeric" placeholder="#" size={4} value={filtro.caixaId} onChange={muda('caixaId')} />
        </label>
      </div>

      <table className="tabela">
        <thead>
          <tr>
            <th>#</th>
            <th>Data</th>
            <th>Status</th>
            <th>Pagamento</th>
            <th>Operador</th>
            <th className="tabela__num">Itens</th>
            <th className="tabela__num">Total</th>
          </tr>
        </thead>
        <tbody>
          {vendas?.content.map((v) => (
            <tr key={v.id} className="tabela__clicavel" onClick={() => setAberta(v.id)}>
              <td>{v.id}</td>
              <td>{dataHora(v.dataFinalizacao ?? v.dataAbertura)}</td>
              <td>
                <span className={`selo selo--${v.status.toLowerCase()}`}>{nomeStatusVenda[v.status]}</span>
              </td>
              <td>
                {v.formas.map((f) => nomeForma[f]).join(' + ') || '—'}
                {v.clienteNome && <small className="bloco-texto">{v.clienteNome}</small>}
              </td>
              <td className="tabela__fraco">{v.operadorNome ?? '—'}</td>
              <td className="tabela__num">{v.quantidadeItens}</td>
              <td className="tabela__num">{moeda(v.total)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {vendas && vendas.content.length === 0 && <p className="vazio-linha">Nenhuma venda nesse filtro. Tente ampliar o período.</p>}

      {vendas && vendas.totalPages > 1 && (
        <div className="paginacao">
          <button className="botao botao--fantasma" disabled={pagina === 0} onClick={() => setPagina((p) => p - 1)}>
            ← Anterior
          </button>
          <span>
            {pagina + 1} de {vendas.totalPages}
          </span>
          <button className="botao botao--fantasma" disabled={pagina + 1 >= vendas.totalPages} onClick={() => setPagina((p) => p + 1)}>
            Próxima →
          </button>
        </div>
      )}

      <Painel titulo={aberta ? `Venda #${aberta}` : ''} aberto={aberta !== null} aoFechar={() => setAberta(null)} largura={600}>
        {aberta !== null && <DetalheVenda id={aberta} aoMudar={carregar} />}
      </Painel>
    </div>
  );
}

function DetalheVenda({ id, aoMudar }: { id: number; aoMudar: () => void }) {
  const { avisar, erro } = useAvisos();
  const { caixa, recarregar } = useCaixa();
  const [venda, setVenda] = useState<Venda | null>(null);
  const [danfe, setDanfe] = useState<DanfeDados | null>(null);
  const [estornando, setEstornando] = useState(false);
  const [trocando, setTrocando] = useState(false);
  const [devolucoes, setDevolucoes] = useState<Devolucao[]>([]);
  const [motivo, setMotivo] = useState('');

  const carregar = useCallback(() => {
    api.venda(id).then(setVenda).catch(erro);
    api.devolucoes(id).then(setDevolucoes).catch(() => undefined);
  }, [id, erro]);
  useEffect(carregar, [carregar]);

  if (!venda) return null;
  const nota = venda.notaFiscal;
  const podeEstornar = venda.status === 'FINALIZADA' && caixa?.id === venda.caixaId && devolucoes.length === 0;
  const podeTrocar = venda.status === 'FINALIZADA' && venda.itens.some((i) => i.quantidadeDevolvida < i.quantidade);
  const podeEmitir = venda.status === 'FINALIZADA' && (!nota || nota.status === 'REJEITADA' || nota.status === 'PENDENTE');

  const emitir = async () => {
    try {
      const n = await api.emitirNfce(venda.id);
      avisar(n.status === 'AUTORIZADA' ? `NFC-e nº ${n.numero} autorizada.` : `NFC-e ${n.status.toLowerCase()}: ${n.motivo}`, n.status === 'AUTORIZADA' ? 'ok' : 'erro');
      carregar();
      aoMudar();
    } catch (e) {
      erro(e);
    }
  };

  const estornar = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.estornarVenda(venda.id, motivo || undefined);
      avisar(`Venda #${venda.id} estornada. Estoque e caixa foram devolvidos.`);
      setEstornando(false);
      carregar();
      aoMudar();
      recarregar();
    } catch (err) {
      erro(err);
    }
  };

  return (
    <div className="detalhe">
      <div className="detalhe__cabeca">
        <span className={`selo selo--${venda.status.toLowerCase()}`}>{nomeStatusVenda[venda.status]}</span>
        <span>Caixa #{venda.caixaId}</span>
        <span>{dataHora(venda.dataFinalizacao ?? venda.dataAbertura)}</span>
        {venda.operadorNome && <span>Operador {venda.operadorNome}</span>}
        {venda.cliente && <span>Cliente {venda.cliente.nome}</span>}
        {venda.documentoConsumidor && <span>CPF/CNPJ {documento(venda.documentoConsumidor)}</span>}
      </div>
      {venda.motivoCancelamento && <p className="dica">Motivo: {venda.motivoCancelamento}</p>}

      <table className="tabela tabela--compacta">
        <tbody>
          {venda.itens.map((i) => (
            <tr key={i.id}>
              <td>
                {i.descricao}
                <small>
                  {' '}
                  · {qtd(i.quantidade)} {i.unidade} × {moeda(i.precoUnitario)}
                </small>
              </td>
              <td className="tabela__num">{moeda(i.subtotal)}</td>
            </tr>
          ))}
          {venda.desconto > 0 && (
            <tr className="tabela__fraco">
              <td>Desconto</td>
              <td className="tabela__num">−{moeda(venda.desconto)}</td>
            </tr>
          )}
          <tr className="tabela__total">
            <td>Total</td>
            <td className="tabela__num">{moeda(venda.total)}</td>
          </tr>
          {venda.pagamentos.map((p) => (
            <tr key={'p' + p.id} className="tabela__fraco">
              <td>{nomeForma[p.forma]}</td>
              <td className="tabela__num">{moeda(p.valor)}</td>
            </tr>
          ))}
          {venda.troco > 0 && (
            <tr className="tabela__fraco">
              <td>Troco</td>
              <td className="tabela__num">{moeda(venda.troco)}</td>
            </tr>
          )}
        </tbody>
      </table>

      <section className="detalhe__nota">
        <h3>NFC-e</h3>
        {nota ? (
          <p className={`nota-status nota-status--${nota.status.toLowerCase()}`}>
            nº {nota.numero} · série {nota.serie} · {nota.status.toLowerCase()}
            {nota.motivo && nota.status !== 'AUTORIZADA' && <small>{nota.motivo}</small>}
          </p>
        ) : (
          <p className="dica">{venda.status === 'FINALIZADA' ? 'Sem nota emitida.' : 'Só vendas finalizadas têm nota.'}</p>
        )}
        <div className="acoes__secundarias">
          {podeEmitir && (
            <button className="botao botao--secundario" onClick={emitir}>
              {nota ? 'Reemitir NFC-e' : 'Emitir NFC-e'}
            </button>
          )}
          {nota && (nota.status === 'AUTORIZADA' || nota.status === 'CANCELADA') && (
            <button className="botao botao--fantasma" onClick={() => api.danfe(nota.id).then(setDanfe).catch(erro)}>
              Ver DANFE
            </button>
          )}
          {danfe && (
            <button className="botao botao--fantasma" onClick={() => window.print()}>
              Imprimir
            </button>
          )}
        </div>
        {danfe && (
          <div className="impressao">
            <Danfe d={danfe} />
          </div>
        )}
      </section>

      {devolucoes.length > 0 && (
        <section className="detalhe__nota">
          <h3>Trocas e devoluções</h3>
          {devolucoes.map((d) => (
            <p key={d.devolucaoId} className="dica">
              {dataHora(d.dataHora)} · {moeda(d.valor)} {d.destino === 'VALE_TROCA' ? `em vale-troca ${d.codigoVale}` : 'devolvidos em dinheiro'}
            </p>
          ))}
        </section>
      )}

      {podeTrocar && !trocando && (
        <button className="botao botao--secundario" onClick={() => setTrocando(true)}>
          Trocar / devolver itens
        </button>
      )}
      {trocando && (
        <Troca
          venda={venda}
          aoConcluir={(d) => {
            setTrocando(false);
            carregar();
            aoMudar();
            recarregar();
            avisar(d.destino === 'VALE_TROCA' ? `Vale-troca ${d.codigoVale} de ${moeda(d.valor)} gerado.` : `${moeda(d.valor)} devolvidos em dinheiro.`);
          }}
          aoVoltar={() => setTrocando(false)}
        />
      )}

      {podeEstornar &&
        (estornando ? (
          <form className="confirmacao" onSubmit={estornar}>
            <p>
              Estornar a venda devolve os itens ao estoque e retira {moeda(dinheiroLiquido(venda))} em dinheiro
              da gaveta.
              {nota?.status === 'AUTORIZADA' && ' Cancele a NFC-e antes (tela Fiscal).'}
            </p>
            <input autoFocus placeholder="Motivo" value={motivo} onChange={(e) => setMotivo(e.target.value)} />
            <div className="acoes__secundarias">
              <button className="botao botao--perigo-cheio">Confirmar estorno</button>
              <button type="button" className="botao botao--fantasma" onClick={() => setEstornando(false)}>
                Voltar
              </button>
            </div>
          </form>
        ) : (
          <button className="botao botao--fantasma botao--perigo" onClick={() => setEstornando(true)}>
            Estornar venda
          </button>
        ))}
    </div>
  );
}

/** Dinheiro que a venda deixou na gaveta: recebido em dinheiro menos o troco. */
function dinheiroLiquido(v: Venda) {
  const dinheiro = v.pagamentos.filter((p) => p.forma === 'DINHEIRO').reduce((s, p) => s + p.valor, 0);
  return Math.max(0, dinheiro - v.troco);
}

/** Escolhe os itens que voltam e o destino do valor (vale-troca ou dinheiro). */
function Troca({ venda, aoConcluir, aoVoltar }: { venda: Venda; aoConcluir: (d: Devolucao) => void; aoVoltar: () => void }) {
  const { erro } = useAvisos();
  const [qtds, setQtds] = useState<Record<number, string>>({});
  const [destino, setDestino] = useState<'VALE_TROCA' | 'DINHEIRO'>('VALE_TROCA');
  const [motivo, setMotivo] = useState('');
  const [vale, setVale] = useState<Devolucao | null>(null);
  const fator = venda.subtotal > 0 ? venda.total / venda.subtotal : 1;
  const estimativa = venda.itens.reduce((s, i) => {
    const q = parseValor(qtds[i.id] ?? '');
    return s + (Number.isFinite(q) ? (i.subtotal * fator * q) / i.quantidade : 0);
  }, 0);

  if (vale) {
    return (
      <div className="detalhe__nota">
        <div className="impressao">
          <div className="comprovante-vale">
            <strong>VALE-TROCA</strong>
            <span className="comprovante-vale__codigo">{vale.codigoVale}</span>
            <span>{moeda(vale.valor)}</span>
            <small>Venda de origem #{venda.id} · {dataHora(vale.dataHora)}</small>
            <small>Apresente este código no caixa para usar o crédito.</small>
          </div>
        </div>
        <div className="acoes__linha">
          <button className="botao botao--principal" onClick={() => window.print()}>
            Imprimir vale
          </button>
          <button className="botao botao--fantasma" onClick={() => aoConcluir(vale)}>
            Concluir
          </button>
        </div>
      </div>
    );
  }

  return (
    <form
      className="troca"
      onSubmit={async (e) => {
        e.preventDefault();
        const itens = Object.entries(qtds)
          .map(([itemId, q]) => ({ itemId: Number(itemId), quantidade: parseValor(q) }))
          .filter((i) => Number.isFinite(i.quantidade) && i.quantidade > 0);
        try {
          const d = await api.devolver(venda.id, itens, destino, motivo || undefined);
          if (d.destino === 'VALE_TROCA') setVale(d);
          else aoConcluir(d);
        } catch (err) {
          erro(err);
        }
      }}
    >
      <h3>O que está voltando?</h3>
      <table className="tabela tabela--compacta">
        <tbody>
          {venda.itens.map((i) => {
            const livre = i.quantidade - i.quantidadeDevolvida;
            return (
              <tr key={i.id} className={livre <= 0 ? 'tabela__inativa' : ''}>
                <td>
                  {i.descricao}
                  <small className="bloco-texto">
                    comprou {qtd(i.quantidade)}
                    {i.quantidadeDevolvida > 0 && ` · já voltou ${qtd(i.quantidadeDevolvida)}`}
                  </small>
                </td>
                <td className="tabela__num">
                  <input
                    className="tabela__input tabela__input--num"
                    inputMode="decimal"
                    placeholder="0"
                    disabled={livre <= 0}
                    value={qtds[i.id] ?? ''}
                    onChange={(e) => setQtds({ ...qtds, [i.id]: e.target.value })}
                    aria-label={`Quantidade de ${i.descricao} devolvida`}
                  />
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <div className="segmentado" role="radiogroup">
        <button type="button" role="radio" aria-checked={destino === 'VALE_TROCA'} onClick={() => setDestino('VALE_TROCA')}>
          Vale-troca
        </button>
        <button type="button" role="radio" aria-checked={destino === 'DINHEIRO'} onClick={() => setDestino('DINHEIRO')}>
          Dinheiro de volta
        </button>
      </div>
      <input placeholder="Motivo (ex.: tamanho errado)" value={motivo} onChange={(e) => setMotivo(e.target.value)} />
      <p className="dica">
        Valor estimado: <strong>{moeda(estimativa)}</strong> (com o desconto da venda). Os itens voltam para o estoque. Pede o PIN do gerente.
      </p>
      <div className="acoes__linha">
        <button className="botao botao--principal" disabled={estimativa <= 0}>
          Confirmar {destino === 'VALE_TROCA' ? 'troca' : 'devolução'}
        </button>
        <button type="button" className="botao botao--fantasma" onClick={aoVoltar}>
          Voltar
        </button>
      </div>
    </form>
  );
}
