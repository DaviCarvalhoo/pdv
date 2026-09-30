import { forwardRef, useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import Danfe from '../components/Danfe';
import { api, ApiError, type Danfe as DanfeDados, type FormaPagamento, type Produto, type Venda } from '../lib/api';
import { useAtalhos, useAvisos, useCaixa } from '../lib/contexto';
import { documento, moeda, nomeForma, numero, parseValor, qtd } from '../lib/format';

type Modo = 'itens' | 'pagamento' | 'concluida';

const FORMAS: { forma: FormaPagamento; tecla: string }[] = [
  { forma: 'DINHEIRO', tecla: '1' },
  { forma: 'PIX', tecla: '2' },
  { forma: 'CARTAO_DEBITO', tecla: '3' },
  { forma: 'CARTAO_CREDITO', tecla: '4' },
];

export default function PaginaPdv() {
  const { caixa, carregando, recarregar } = useCaixa();
  const { avisar, erro } = useAvisos();

  const [venda, setVenda] = useState<Venda | null>(null);
  const [modo, setModo] = useState<Modo>('itens');
  const [ocupado, setOcupado] = useState(false);
  const [selecionado, setSelecionado] = useState<number>(-1);
  const [ultimoItem, setUltimoItem] = useState<number | null>(null);
  const [danfe, setDanfe] = useState<DanfeDados | null>(null);
  const [confirmarCancelamento, setConfirmarCancelamento] = useState(false);
  const [editandoCpf, setEditandoCpf] = useState(false);

  const leitorRef = useRef<HTMLInputElement>(null);
  const focarLeitor = () => setTimeout(() => leitorRef.current?.focus(), 0);

  useEffect(() => {
    if (!caixa) return;
    api.vendaAberta().then((v) => setVenda(v ?? null)).catch(erro);
  }, [caixa, erro]);

  const executar = useCallback(
    async (acao: () => Promise<Venda>, aoConcluir?: (v: Venda) => void) => {
      setOcupado(true);
      try {
        const v = await acao();
        setVenda(v);
        aoConcluir?.(v);
        return v;
      } catch (e) {
        erro(e);
        return null;
      } finally {
        setOcupado(false);
      }
    },
    [erro],
  );

  /** Garante uma venda aberta (cria na primeira leitura). */
  const vendaAtual = useCallback(async () => {
    if (venda && venda.status === 'ABERTA') return venda;
    try {
      const nova = await api.iniciarVenda();
      setVenda(nova);
      setDanfe(null);
      setModo('itens');
      return nova;
    } catch (e) {
      if (e instanceof ApiError && e.codigo === 'VENDA_EM_ANDAMENTO') {
        const v = await api.vendaAberta();
        if (v) {
          setVenda(v);
          return v;
        }
      }
      erro(e);
      return null;
    }
  }, [venda, erro]);

  const lancar = useCallback(
    async (entrada: { codigo?: string; produtoId?: number; quantidade: number }) => {
      const v = await vendaAtual();
      if (!v) return false;
      const resultado = await executar(() =>
        entrada.produtoId
          ? api.adicionarProduto(v.id, entrada.produtoId, entrada.quantidade)
          : api.adicionarItem(v.id, entrada.codigo!, entrada.quantidade),
      );
      if (resultado) {
        const item = resultado.itens.find((i) =>
          entrada.produtoId ? i.produtoId === entrada.produtoId : i.codigo === entrada.codigo,
        ) ?? resultado.itens[resultado.itens.length - 1];
        setUltimoItem(item?.id ?? null);
        setSelecionado(resultado.itens.findIndex((i) => i.id === item?.id));
      }
      return !!resultado;
    },
    [vendaAtual, executar],
  );

  const podeReceber = !!venda && venda.status === 'ABERTA' && venda.itens.length > 0;

  const irParaPagamento = () => {
    if (!podeReceber) return avisar('Passe ao menos um produto antes de receber.', 'info');
    setModo('pagamento');
  };

  const finalizar = async () => {
    if (!venda || venda.restante > 0) return;
    const v = await executar(() => api.finalizar(venda.id));
    if (!v) return;
    setModo('concluida');
    v.avisos.forEach((a) => avisar(a, 'info'));
    recarregar();
    if (v.notaFiscal?.status === 'AUTORIZADA') {
      api.danfe(v.notaFiscal.id).then(setDanfe).catch(erro);
    }
  };

  const novaVenda = () => {
    setVenda(null);
    setDanfe(null);
    setModo('itens');
    setSelecionado(-1);
    setUltimoItem(null);
    focarLeitor();
  };

  const cancelarVenda = async (motivo: string) => {
    if (!venda) return;
    const v = await executar(() => api.cancelarVenda(venda.id, motivo || undefined));
    if (v) {
      avisar(`Venda #${v.id} cancelada.`, 'info');
      setConfirmarCancelamento(false);
      novaVenda();
    }
  };

  const alterarQtd = (indice: number, delta: number) => {
    if (!venda || modo !== 'itens') return;
    const item = venda.itens[indice];
    if (!item) return;
    const nova = item.quantidade + delta;
    if (nova <= 0) return;
    executar(() => api.alterarQuantidade(venda.id, item.id, nova));
  };

  const removerSelecionado = () => {
    if (!venda || modo !== 'itens') return;
    const item = venda.itens[selecionado];
    if (!item) return;
    executar(
      () => api.removerItem(venda.id, item.id),
      (v) => setSelecionado(Math.min(selecionado, v.itens.length - 1)),
    );
  };

  const livre = !confirmarCancelamento && !editandoCpf;
  useAtalhos(
    {
      F2: focarLeitor,
      F3: () => podeReceber && setEditandoCpf(true),
      F4: () => (modo === 'itens' ? irParaPagamento() : undefined),
      F8: () => venda?.status === 'ABERTA' && setConfirmarCancelamento(true),
      F10: () => (modo === 'pagamento' ? finalizar() : undefined),
      Escape: () => {
        if (modo === 'pagamento') {
          setModo('itens');
          focarLeitor();
        }
      },
      ArrowUp: () => modo === 'itens' && venda && setSelecionado((s) => Math.max(0, s - 1)),
      ArrowDown: () =>
        modo === 'itens' && venda && setSelecionado((s) => Math.min(venda.itens.length - 1, s + 1)),
      Delete: removerSelecionado,
      '+': () => alterarQtd(selecionado, 1),
      '-': () => alterarQtd(selecionado, -1),
    },
    !!caixa && livre && modo !== 'concluida',
  );

  // Na tela de venda concluída, Enter começa outra e P imprime o DANFE.
  useAtalhos(
    {
      Enter: novaVenda,
      p: () => danfe && window.print(),
      P: () => danfe && window.print(),
    },
    modo === 'concluida',
  );

  if (carregando) return <div className="pdv pdv--vazio" />;
  if (!caixa) return <CaixaFechado />;

  const itens = venda?.status === 'ABERTA' || modo === 'concluida' ? venda?.itens ?? [] : [];

  return (
    <div className="pdv">
      <section className="pdv__fita">
        {modo === 'itens' && <Leitor ref={leitorRef} ocupado={ocupado} aoLancar={lancar} />}

        <div className="cupom" aria-label="Itens da venda">
          <header className="cupom__topo">
            <span>{venda ? `Venda #${venda.id}` : 'Nova venda'}</span>
            <span>{itens.length ? `${itens.length} ${itens.length === 1 ? 'item' : 'itens'}` : ''}</span>
          </header>

          {itens.length === 0 ? (
            <div className="cupom__vazio">
              <p className="cupom__vazio-titulo">Passe o primeiro produto no leitor.</p>
              <ul>
                <li>
                  <kbd>3*</kbd> antes do código lança 3 unidades
                </li>
                <li>Digite parte do nome para buscar</li>
                <li>
                  <kbd>↑</kbd>
                  <kbd>↓</kbd> escolhem a linha, <kbd>+</kbd>
                  <kbd>−</kbd> mudam a quantidade, <kbd>Del</kbd> remove
                </li>
              </ul>
            </div>
          ) : (
            <ol className="cupom__linhas">
              {itens.map((item, i) => (
                <li
                  key={item.id}
                  className={[
                    'linha',
                    i === selecionado && modo === 'itens' ? 'linha--selecionada' : '',
                    item.id === ultimoItem ? 'linha--nova' : '',
                  ].join(' ')}
                  onClick={() => setSelecionado(i)}
                >
                  <span className="linha__n">{String(i + 1).padStart(3, '0')}</span>
                  <span className="linha__desc">
                    {item.descricao}
                    <small>
                      {qtd(item.quantidade)} {item.unidade} × {moeda(item.precoUnitario)}
                      {item.estoqueDisponivel < item.quantidade && modo !== 'concluida' && (
                        <em className="linha__alerta"> · estoque {qtd(item.estoqueDisponivel)}</em>
                      )}
                    </small>
                  </span>
                  {modo === 'itens' ? (
                    <QuantidadeEditavel
                      valor={item.quantidade}
                      aoMudar={(q) => executar(() => api.alterarQuantidade(venda!.id, item.id, q))}
                    />
                  ) : (
                    <span className="linha__qtd">{qtd(item.quantidade)}</span>
                  )}
                  <span className="linha__subtotal">{numero(item.subtotal)}</span>
                  {modo === 'itens' && (
                    <button
                      className="linha__remover"
                      aria-label={`Remover ${item.descricao}`}
                      onClick={(e) => {
                        e.stopPropagation();
                        executar(() => api.removerItem(venda!.id, item.id));
                      }}
                    >
                      ×
                    </button>
                  )}
                </li>
              ))}
            </ol>
          )}
          <div className="cupom__serrilha" aria-hidden />
        </div>
      </section>

      <section className="pdv__lado">
        <Etiqueta total={venda?.total ?? 0} rotulo={modo === 'concluida' ? 'Total pago' : 'Total'} />

        {confirmarCancelamento && venda && (
          <ConfirmarCancelamento
            vendaId={venda.id}
            aoConfirmar={cancelarVenda}
            aoVoltar={() => {
              setConfirmarCancelamento(false);
              focarLeitor();
            }}
          />
        )}

        {editandoCpf && venda && (
          <CampoCpf
            atual={venda.documentoConsumidor}
            aoSalvar={async (doc) => {
              const v = await executar(() => api.consumidor(venda.id, doc));
              if (v) setEditandoCpf(false);
            }}
            aoVoltar={() => setEditandoCpf(false)}
          />
        )}

        {!confirmarCancelamento && !editandoCpf && modo === 'itens' && (
          <div className="acoes">
            <button className="botao botao--principal botao--grande" disabled={!podeReceber} onClick={irParaPagamento}>
              Receber <kbd>F4</kbd>
            </button>
            <div className="acoes__secundarias">
              <button className="botao botao--fantasma" disabled={!podeReceber} onClick={() => setEditandoCpf(true)}>
                {venda?.documentoConsumidor ? `CPF ${documento(venda.documentoConsumidor)}` : 'CPF na nota'}{' '}
                <kbd>F3</kbd>
              </button>
              <button
                className="botao botao--fantasma botao--perigo"
                disabled={venda?.status !== 'ABERTA'}
                onClick={() => setConfirmarCancelamento(true)}
              >
                Cancelar venda <kbd>F8</kbd>
              </button>
            </div>
          </div>
        )}

        {!confirmarCancelamento && !editandoCpf && modo === 'pagamento' && venda && (
          <Pagamento
            venda={venda}
            ocupado={ocupado}
            aoPagar={(forma, valor) => executar(() => api.pagar(venda.id, forma, valor))}
            aoRemover={(id) => executar(() => api.removerPagamento(venda.id, id))}
            aoFinalizar={finalizar}
            aoVoltar={() => {
              setModo('itens');
              focarLeitor();
            }}
          />
        )}

        {modo === 'concluida' && venda && (
          <div className="concluida">
            {venda.troco > 0 ? (
              <div className="concluida__troco">
                <span>Troco</span>
                <strong>{moeda(venda.troco)}</strong>
              </div>
            ) : (
              <p className="concluida__ok">Venda #{venda.id} finalizada.</p>
            )}
            <NotaStatus venda={venda} />
            <div className="acoes__secundarias">
              <button className="botao botao--principal" onClick={novaVenda} autoFocus>
                Nova venda <kbd>Enter</kbd>
              </button>
              {danfe && (
                <button className="botao botao--fantasma" onClick={() => window.print()}>
                  Imprimir DANFE <kbd>P</kbd>
                </button>
              )}
            </div>
            {danfe && (
              <div className="concluida__danfe impressao">
                <Danfe d={danfe} />
              </div>
            )}
          </div>
        )}
      </section>
    </div>
  );
}

// ---------------------------------------------------------------------------------------------

function CaixaFechado() {
  return (
    <div className="pdv pdv--fechado">
      <div className="fechado">
        <p className="sobretitulo">Caixa fechado</p>
        <h1>Abra o caixa para começar a vender.</h1>
        <p>Informe o dinheiro que está na gaveta (fundo de troco) e o PDV fica pronto para o leitor.</p>
        <Link className="botao botao--principal botao--grande" to="/caixa">
          Abrir caixa <kbd>F6</kbd>
        </Link>
      </div>
    </div>
  );
}

function Etiqueta({ total, rotulo }: { total: number; rotulo: string }) {
  const [inteiro, centavos] = numero(total).split(',');
  return (
    <div className="etiqueta" aria-label={`${rotulo}: ${moeda(total)}`}>
      <span className="etiqueta__furo" aria-hidden />
      <span className="etiqueta__rotulo">{rotulo}</span>
      <span className="etiqueta__valor" key={total}>
        <small>R$</small>
        {inteiro}
        <sup>,{centavos}</sup>
      </span>
    </div>
  );
}

// ------------------------------------------------------------------------- Leitor de códigos

const Leitor = forwardRef<
  HTMLInputElement,
  { ocupado: boolean; aoLancar: (e: { codigo?: string; produtoId?: number; quantidade: number }) => Promise<boolean> }
>(function Leitor({ ocupado, aoLancar }, ref) {
  const [texto, setTexto] = useState('');
  const [sugestoes, setSugestoes] = useState<Produto[]>([]);
  const [indice, setIndice] = useState(0);
  const { erro } = useAvisos();

  const { quantidade, termo } = interpretar(texto);
  const ehBusca = termo.length >= 2 && !/^\d+$/.test(termo);

  useEffect(() => {
    if (!ehBusca) {
      setSugestoes([]);
      return;
    }
    const t = setTimeout(() => {
      api
        .produtos(termo)
        .then((p) => {
          setSugestoes(p.content.slice(0, 7));
          setIndice(0);
        })
        .catch(() => setSugestoes([]));
    }, 180);
    return () => clearTimeout(t);
  }, [termo, ehBusca]);

  const lancar = async (entrada: { codigo?: string; produtoId?: number }) => {
    if (!Number.isFinite(quantidade) || quantidade <= 0) {
      erro(new ApiError(422, 'QUANTIDADE_INVALIDA', 'Quantidade inválida.'));
      return;
    }
    const ok = await aoLancar({ ...entrada, quantidade });
    if (ok) {
      setTexto('');
      setSugestoes([]);
    }
  };

  const enviar = () => {
    if (!termo) return;
    if (sugestoes.length > 0) lancar({ produtoId: sugestoes[indice].id });
    else if (!ehBusca) lancar({ codigo: termo });
  };

  return (
    <div className="leitor">
      <label className="leitor__rotulo" htmlFor="leitor">
        Código de barras, código ou nome <kbd>F2</kbd>
      </label>
      <div className="leitor__campo">
        <input
          id="leitor"
          ref={ref}
          autoFocus
          autoComplete="off"
          spellCheck={false}
          value={texto}
          placeholder="Passe o produto no leitor…"
          aria-busy={ocupado}
          aria-autocomplete="list"
          aria-expanded={sugestoes.length > 0}
          onChange={(e) => setTexto(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault();
              enviar();
            } else if (sugestoes.length && (e.key === 'ArrowDown' || e.key === 'ArrowUp')) {
              e.preventDefault();
              e.stopPropagation();
              setIndice((i) => (i + (e.key === 'ArrowDown' ? 1 : -1) + sugestoes.length) % sugestoes.length);
            } else if (e.key === 'Escape' && texto) {
              e.stopPropagation();
              setTexto('');
            } else if ((e.key === '+' || e.key === '-') && texto) {
              e.stopPropagation();
            }
          }}
        />
        {quantidade !== 1 && Number.isFinite(quantidade) && termo && (
          <span className="leitor__qtd">× {qtd(quantidade)}</span>
        )}
      </div>
      {sugestoes.length > 0 && (
        <ul className="sugestoes" role="listbox">
          {sugestoes.map((p, i) => (
            <li
              key={p.id}
              role="option"
              aria-selected={i === indice}
              className={i === indice ? 'sugestoes__item--ativo' : ''}
              onMouseDown={(e) => {
                e.preventDefault();
                lancar({ produtoId: p.id });
              }}
            >
              <span>{p.nome}</span>
              <small>{p.gtin ?? p.codigoInterno}</small>
              <strong>{moeda(p.preco)}</strong>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
});

/** "3*789..." → 3 unidades; "0,350*015" → 0,350 kg. */
function interpretar(texto: string) {
  const m = texto.trim().match(/^(\d+(?:[.,]\d+)?)\s*\*\s*(.*)$/);
  if (m) return { quantidade: parseValor(m[1]), termo: m[2].trim() };
  return { quantidade: 1, termo: texto.trim() };
}

function QuantidadeEditavel({ valor, aoMudar }: { valor: number; aoMudar: (q: number) => void }) {
  const [editando, setEditando] = useState(false);
  const [texto, setTexto] = useState('');
  if (!editando) {
    return (
      <button
        className="linha__qtd linha__qtd--botao"
        title="Alterar quantidade"
        onClick={(e) => {
          e.stopPropagation();
          setTexto(String(valor).replace('.', ','));
          setEditando(true);
        }}
      >
        {qtd(valor)}
      </button>
    );
  }
  const confirmar = () => {
    const q = parseValor(texto);
    setEditando(false);
    if (Number.isFinite(q) && q > 0 && q !== valor) aoMudar(q);
  };
  return (
    <input
      className="linha__qtd-input"
      autoFocus
      value={texto}
      onClick={(e) => e.stopPropagation()}
      onChange={(e) => setTexto(e.target.value)}
      onBlur={confirmar}
      onKeyDown={(e) => {
        e.stopPropagation();
        if (e.key === 'Enter') confirmar();
        if (e.key === 'Escape') setEditando(false);
      }}
      aria-label="Nova quantidade"
    />
  );
}

// ------------------------------------------------------------------------------- Pagamento

function Pagamento({
  venda,
  ocupado,
  aoPagar,
  aoRemover,
  aoFinalizar,
  aoVoltar,
}: {
  venda: Venda;
  ocupado: boolean;
  aoPagar: (f: FormaPagamento, v: number) => Promise<Venda | null>;
  aoRemover: (id: number) => void;
  aoFinalizar: () => void;
  aoVoltar: () => void;
}) {
  const [forma, setForma] = useState<FormaPagamento>('DINHEIRO');
  const [valor, setValor] = useState('');
  const campo = useRef<HTMLInputElement>(null);
  const quitada = venda.restante <= 0;

  useEffect(() => {
    setValor(venda.restante > 0 ? numero(venda.restante) : '');
    setTimeout(() => campo.current?.select(), 0);
  }, [venda.restante, forma]);

  const escolher = (f: FormaPagamento) => {
    setForma(f);
    campo.current?.focus();
  };

  const lancar = async () => {
    const v = parseValor(valor);
    if (!Number.isFinite(v) || v <= 0) return;
    await aoPagar(forma, v);
  };

  // Atalhos de valor rápido para dinheiro: notas comuns acima do restante.
  const notas = [5, 10, 20, 50, 100, 200].filter((n) => n > venda.restante).slice(0, 3);

  return (
    <div className="pagamento">
      <div className="pagamento__resumo">
        <div>
          <span>Pago</span>
          <strong>{moeda(venda.valorPago)}</strong>
        </div>
        <div className={quitada ? 'pagamento__ok' : 'pagamento__falta'}>
          <span>{quitada ? 'Quitado' : 'Falta'}</span>
          <strong>{moeda(venda.restante)}</strong>
        </div>
      </div>

      {venda.troco > 0 && (
        <div className="troco" aria-live="polite">
          <span>Troco</span>
          <strong>{moeda(venda.troco)}</strong>
        </div>
      )}

      {!quitada && (
        <>
          <div className="formas" role="radiogroup" aria-label="Forma de pagamento">
            {FORMAS.map(({ forma: f, tecla }) => (
              <button
                key={f}
                role="radio"
                aria-checked={forma === f}
                className={`forma ${forma === f ? 'forma--ativa' : ''}`}
                onClick={() => escolher(f)}
              >
                <kbd>{tecla}</kbd>
                {nomeForma[f]}
              </button>
            ))}
          </div>
          <form
            className="pagamento__valor"
            onSubmit={(e) => {
              e.preventDefault();
              lancar();
            }}
          >
            <label htmlFor="valor-pagamento">Valor em {nomeForma[forma].toLowerCase()}</label>
            <div className="campo-moeda">
              <span>R$</span>
              <input
                id="valor-pagamento"
                ref={campo}
                inputMode="decimal"
                autoFocus
                value={valor}
                onChange={(e) => setValor(e.target.value)}
                onKeyDown={(e) => {
                  const f = FORMAS.find((x) => x.tecla === e.key);
                  if (f && (e.currentTarget.selectionStart === 0 && e.currentTarget.selectionEnd === valor.length)) {
                    e.preventDefault();
                    escolher(f.forma);
                  }
                }}
              />
              <button className="botao botao--principal" disabled={ocupado}>
                Lançar <kbd>Enter</kbd>
              </button>
            </div>
            {forma === 'DINHEIRO' && notas.length > 0 && (
              <div className="notas">
                {notas.map((n) => (
                  <button type="button" key={n} className="nota" onClick={() => aoPagar('DINHEIRO', n)}>
                    {moeda(n)}
                  </button>
                ))}
              </div>
            )}
            <p className="dica">Selecione o valor e aperte 1–4 para trocar a forma. Só dinheiro gera troco.</p>
          </form>
        </>
      )}

      {venda.pagamentos.length > 0 && (
        <ul className="pagamentos">
          {venda.pagamentos.map((p) => (
            <li key={p.id}>
              <span>{nomeForma[p.forma]}</span>
              <strong>{moeda(p.valor)}</strong>
              <button aria-label={`Remover pagamento de ${moeda(p.valor)}`} onClick={() => aoRemover(p.id)}>
                ×
              </button>
            </li>
          ))}
        </ul>
      )}

      <div className="acoes__secundarias">
        <button className="botao botao--confirmar botao--grande" disabled={!quitada || ocupado} onClick={aoFinalizar}>
          Finalizar <kbd>F10</kbd>
        </button>
        <button className="botao botao--fantasma" onClick={aoVoltar}>
          Voltar aos itens <kbd>Esc</kbd>
        </button>
      </div>
    </div>
  );
}

function NotaStatus({ venda }: { venda: Venda }) {
  const nota = venda.notaFiscal;
  if (!nota) return <p className="nota-status">Sem NFC-e para esta venda.</p>;
  return (
    <p className={`nota-status nota-status--${nota.status.toLowerCase()}`}>
      NFC-e nº {nota.numero} · {nota.status.toLowerCase()}
      {nota.ambiente === 'HOMOLOGACAO' && ' · homologação'}
      {nota.status !== 'AUTORIZADA' && nota.motivo && <small>{nota.motivo}</small>}
    </p>
  );
}

function ConfirmarCancelamento({
  vendaId,
  aoConfirmar,
  aoVoltar,
}: {
  vendaId: number;
  aoConfirmar: (motivo: string) => void;
  aoVoltar: () => void;
}) {
  const [motivo, setMotivo] = useState('');
  return (
    <form
      className="confirmacao"
      onSubmit={(e) => {
        e.preventDefault();
        aoConfirmar(motivo);
      }}
      onKeyDown={(e) => e.key === 'Escape' && aoVoltar()}
    >
      <p>
        Cancelar a venda <strong>#{vendaId}</strong>? Os itens e pagamentos lançados são descartados.
      </p>
      <input autoFocus placeholder="Motivo (opcional)" value={motivo} onChange={(e) => setMotivo(e.target.value)} />
      <div className="acoes__secundarias">
        <button className="botao botao--perigo-cheio">
          Cancelar venda <kbd>Enter</kbd>
        </button>
        <button type="button" className="botao botao--fantasma" onClick={aoVoltar}>
          Voltar <kbd>Esc</kbd>
        </button>
      </div>
    </form>
  );
}

function CampoCpf({
  atual,
  aoSalvar,
  aoVoltar,
}: {
  atual?: string;
  aoSalvar: (doc: string) => void;
  aoVoltar: () => void;
}) {
  const [doc, setDoc] = useState(atual ? documento(atual) : '');
  return (
    <form
      className="confirmacao confirmacao--neutra"
      onSubmit={(e) => {
        e.preventDefault();
        aoSalvar(doc);
      }}
      onKeyDown={(e) => e.key === 'Escape' && aoVoltar()}
    >
      <label htmlFor="cpf">CPF ou CNPJ na nota</label>
      <input id="cpf" autoFocus inputMode="numeric" placeholder="000.000.000-00" value={doc} onChange={(e) => setDoc(e.target.value)} />
      <div className="acoes__secundarias">
        <button className="botao botao--principal">
          Salvar <kbd>Enter</kbd>
        </button>
        <button type="button" className="botao botao--fantasma" onClick={aoVoltar}>
          Voltar <kbd>Esc</kbd>
        </button>
        {atual && (
          <button type="button" className="botao botao--fantasma" onClick={() => aoSalvar('')}>
            Remover
          </button>
        )}
      </div>
    </form>
  );
}
