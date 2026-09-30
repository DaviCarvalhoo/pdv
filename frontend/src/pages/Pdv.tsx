/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

import { forwardRef, useCallback, useEffect, useImperativeHandle, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import Danfe from '../components/Danfe';
import { PainelCliente, PainelConsulta, PainelDesconto, PainelEspera, CampoCpf, ConfirmarCancelamento } from '../components/PdvPaineis';
import Pagamento from '../components/PdvPagamento';
import { api, ApiError, type Danfe as DanfeDados, type Produto, type Venda, type VendaResumo } from '../lib/api';
import { useAtalhos, useAvisos, useCaixa, useSessao } from '../lib/contexto';
import { documento, moeda, numero, parseValor, qtd } from '../lib/format';

type Modo = 'itens' | 'pagamento' | 'concluida';
type Painel = 'cliente' | 'desconto' | 'espera' | 'consulta' | 'cpf' | 'cancelar' | null;

export interface LeitorHandle {
  focar: () => void;
  preencher: (texto: string, cursorNoInicio?: boolean) => void;
}

export default function PaginaPdv() {
  const { caixa, carregando, recarregar } = useCaixa();
  const { operador, terminal } = useSessao();
  const { avisar, erro } = useAvisos();

  const [venda, setVenda] = useState<Venda | null>(null);
  const [modo, setModo] = useState<Modo>('itens');
  const [painel, setPainel] = useState<Painel>(null);
  const [ocupado, setOcupado] = useState(false);
  const [selecionado, setSelecionado] = useState(-1);
  const [ultimoItem, setUltimoItem] = useState<number | null>(null);
  const [danfe, setDanfe] = useState<DanfeDados | null>(null);
  const [atalhos, setAtalhos] = useState<Produto[]>([]);
  const [emEspera, setEmEspera] = useState<VendaResumo[]>([]);

  const leitor = useRef<LeitorHandle>(null);
  const focarLeitor = () => setTimeout(() => leitor.current?.focar(), 0);

  const caixaId = caixa?.id;
  const recarregarEspera = useCallback(() => {
    api.vendasEmEspera().then(setEmEspera).catch(() => undefined);
  }, []);

  // Retoma a venda em andamento só quando o caixa muda (não a cada atualização do saldo da gaveta).
  useEffect(() => {
    if (!caixaId) return;
    api.vendaAberta().then((v) => setVenda(v ?? null)).catch(erro);
    api.atalhos().then(setAtalhos).catch(() => undefined);
    recarregarEspera();
  }, [caixaId, erro, recarregarEspera]);

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
    if (venda && venda.status === 'ABERTA' && !venda.emEspera) return venda;
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
    async (entrada: { codigo?: string; produtoId?: number; quantidade?: number }) => {
      const v = await vendaAtual();
      if (!v) return false;
      const antes = new Map(v.itens.map((i) => [i.id, i.quantidade]));
      const resultado = await executar(() =>
        entrada.produtoId
          ? api.adicionarProduto(v.id, entrada.produtoId, entrada.quantidade ?? 1)
          : api.adicionarItem(v.id, entrada.codigo!, entrada.quantidade),
      );
      if (resultado) {
        const mudou = resultado.itens.find((i) => antes.get(i.id) !== i.quantidade) ?? resultado.itens.at(-1);
        setUltimoItem(mudou?.id ?? null);
        setSelecionado(resultado.itens.findIndex((i) => i.id === mudou?.id));
      }
      return !!resultado;
    },
    [vendaAtual, executar],
  );

  const vendaAberta = !!venda && venda.status === 'ABERTA' && !venda.emEspera;
  const podeReceber = vendaAberta && venda!.itens.length > 0;

  const abrirPainel = (p: Painel) => {
    if (p && p !== 'consulta' && p !== 'espera' && !podeReceber) {
      return avisar('Passe ao menos um produto primeiro.', 'info');
    }
    setPainel(p);
  };

  const fecharPainel = () => {
    setPainel(null);
    if (modo === 'itens') focarLeitor();
  };

  const irParaPagamento = () => {
    if (!podeReceber) return avisar('Passe ao menos um produto antes de receber.', 'info');
    setPainel(null);
    setModo('pagamento');
  };

  const finalizar = async () => {
    if (!venda || venda.restante > 0 || ocupado) return;
    let v = await executar(() => api.finalizar(venda.id));
    if (!v) {
      // A resposta pode ter se perdido com a venda já gravada: confere antes de pedir para repetir.
      const atual = await api.venda(venda.id).catch(() => null);
      if (atual?.status !== 'FINALIZADA') return;
      setVenda(atual);
      v = atual;
    }
    setModo('concluida');
    v.avisos.forEach((a) => avisar(a, 'info'));
    recarregar();
    if (v.notaFiscal?.status === 'AUTORIZADA') api.danfe(v.notaFiscal.id).then(setDanfe).catch(erro);
  };

  const novaVenda = () => {
    setVenda(null);
    setDanfe(null);
    setModo('itens');
    setPainel(null);
    setSelecionado(-1);
    setUltimoItem(null);
    recarregarEspera();
    focarLeitor();
  };

  const alterarQtd = (indice: number, delta: number) => {
    if (!venda || modo !== 'itens') return;
    const item = venda.itens[indice];
    if (!item || item.quantidade + delta <= 0) return;
    executar(() => api.alterarQuantidade(venda.id, item.id, item.quantidade + delta));
  };

  const removerSelecionado = () => {
    if (!venda || modo !== 'itens') return;
    const item = venda.itens[selecionado];
    if (item) executar(() => api.removerItem(venda.id, item.id), (v) => setSelecionado(Math.min(selecionado, v.itens.length - 1)));
  };

  useAtalhos(
    {
      F2: focarLeitor,
      F3: () => abrirPainel('cpf'),
      F4: () => (modo === 'itens' ? irParaPagamento() : undefined),
      F5: () => abrirPainel('cliente'),
      F6: () => abrirPainel('desconto'),
      F7: () => setPainel('espera'),
      F8: () => vendaAberta && setPainel('cancelar'),
      F9: () => setPainel('consulta'),
      F10: () => (modo === 'pagamento' ? finalizar() : undefined),
      Escape: () => {
        if (modo === 'pagamento') {
          setModo('itens');
          focarLeitor();
        }
      },
      ArrowUp: () => modo === 'itens' && venda && setSelecionado((s) => Math.max(0, s - 1)),
      ArrowDown: () => modo === 'itens' && venda && setSelecionado((s) => Math.min(venda.itens.length - 1, s + 1)),
      Delete: removerSelecionado,
      '+': () => alterarQtd(selecionado, 1),
      '-': () => alterarQtd(selecionado, -1),
    },
    !!caixa && painel === null && modo !== 'concluida',
  );

  useAtalhos({ Enter: novaVenda, p: () => danfe && window.print(), P: () => danfe && window.print() }, modo === 'concluida');

  if (carregando) return <div className="pdv" />;
  if (terminal && 'retaguarda' in terminal) return <CaixaFechado retaguarda />;
  if (!caixa) return <CaixaFechado nome={terminal && 'nome' in terminal ? terminal.nome : 'O caixa'} />;

  const itens = vendaAberta || modo !== 'itens' ? (venda?.itens ?? []) : [];
  const mostrarAtalhos = modo === 'itens' && atalhos.length > 0;

  return (
    <div className="pdv">
      <header className="pdv__topo">
        <div className="pdv__topo-info">
          <span className="pdv__venda">{vendaAberta || modo !== 'itens' ? `Venda #${venda!.id}` : 'Nova venda'}</span>
          <span>Operador: {operador?.nome}</span>
          {venda?.cliente && (
            <button className="chip chip--cliente" onClick={() => abrirPainel('cliente')} disabled={modo !== 'itens'}>
              {venda.cliente.nome}
            </button>
          )}
          {!venda?.cliente && venda?.documentoConsumidor && <span className="chip">CPF {documento(venda.documentoConsumidor)}</span>}
        </div>
        <div className="pdv__topo-acoes">
          {emEspera.length > 0 && (
            <button className="chip chip--espera" onClick={() => setPainel('espera')}>
              {emEspera.length} em espera <kbd>F7</kbd>
            </button>
          )}
          <button className="chip" onClick={() => setPainel('consulta')}>
            Consultar preço <kbd>F9</kbd>
          </button>
        </div>
      </header>

      {caixa.alertaSangriaLimite && (
        <div className="faixa faixa--alerta pdv__faixa">
          A gaveta passou de {moeda(caixa.alertaSangriaLimite)}. Faça uma sangria para guardar o excesso no cofre.{' '}
          <Link to="/caixa">Ir para o caixa →</Link>
        </div>
      )}

      <section className="pdv__fita">
        {modo === 'itens' && <Leitor ref={leitor} ocupado={ocupado} aoLancar={lancar} />}

        <div className="cupom" aria-label="Itens da venda">
          {itens.length === 0 ? (
            <div className="cupom__vazio">
              <p className="cupom__vazio-titulo">Passe o primeiro produto no leitor.</p>
              <ul>
                <li>
                  <kbd>3*</kbd> antes do código lança 3 unidades · <kbd>0,350*</kbd> lança 350 g
                </li>
                <li>Etiqueta da balança e parte do nome também funcionam</li>
                <li>
                  <kbd>↑</kbd>
                  <kbd>↓</kbd> escolhem a linha · <kbd>+</kbd>
                  <kbd>−</kbd> mudam a quantidade · <kbd>Del</kbd> remove
                </li>
              </ul>
            </div>
          ) : (
            <ol className="cupom__linhas">
              {itens.map((item, i) => (
                <li
                  key={item.id}
                  className={['linha', i === selecionado && modo === 'itens' ? 'linha--selecionada' : '', item.id === ultimoItem ? 'linha--nova' : ''].join(' ')}
                  onClick={() => setSelecionado(i)}
                >
                  <span className="linha__n">{String(i + 1).padStart(3, '0')}</span>
                  <span className="linha__desc">
                    {item.descricao}
                    {item.promocional && <em className="selo selo--oferta">oferta</em>}
                    <small>
                      {qtd(item.quantidade)} {item.unidade} × {moeda(item.precoUnitario)}
                      {item.promocional && <s> {moeda(item.precoNormal)}</s>}
                      {item.estoqueDisponivel < item.quantidade && modo !== 'concluida' && (
                        <em className="linha__alerta"> · estoque {qtd(item.estoqueDisponivel)}</em>
                      )}
                    </small>
                  </span>
                  {modo === 'itens' ? (
                    <QuantidadeEditavel valor={item.quantidade} aoMudar={(q) => executar(() => api.alterarQuantidade(venda!.id, item.id, q))} />
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
          {venda && venda.desconto > 0 && itens.length > 0 && (
            <div className="cupom__desconto">
              <span>Subtotal {moeda(venda.subtotal)}</span>
              <span>
                Desconto {venda.descontoPercentual ? `${numero(venda.descontoPercentual)}%` : ''} −{moeda(venda.desconto)}
              </span>
            </div>
          )}
          <div className="cupom__serrilha" aria-hidden />
        </div>

        {mostrarAtalhos && (
          <div className="atalhos" aria-label="Produtos de acesso rápido">
            {atalhos.map((p) => (
              <button
                key={p.id}
                className="atalho"
                style={p.categoriaCor ? ({ '--cat': p.categoriaCor } as React.CSSProperties) : undefined}
                onClick={() => {
                  if (p.unidade === 'KG') leitor.current?.preencher(`*${p.codigoInterno ?? p.gtin ?? ''}`, true);
                  else lancar({ produtoId: p.id, quantidade: 1 });
                }}
              >
                <strong>{p.nome}</strong>
                <span>
                  {moeda(p.precoVigente)}
                  {p.unidade === 'KG' ? '/kg' : ''}
                </span>
              </button>
            ))}
          </div>
        )}
      </section>

      <section className="pdv__lado">
        <Etiqueta total={venda && itens.length ? venda.total : 0} rotulo={modo === 'concluida' ? 'Total pago' : 'Total'} quantidade={itens.length} />

        {painel === 'cancelar' && venda && (
          <ConfirmarCancelamento
            vendaId={venda.id}
            aoConfirmar={async (motivo) => {
              const v = await executar(() => api.cancelarVenda(venda.id, motivo || undefined));
              if (v) {
                avisar(`Venda #${v.id} cancelada.`, 'info');
                novaVenda();
              }
            }}
            aoVoltar={fecharPainel}
          />
        )}
        {painel === 'cpf' && venda && (
          <CampoCpf
            atual={venda.documentoConsumidor}
            aoSalvar={async (doc) => {
              if (await executar(() => api.consumidor(venda.id, doc))) fecharPainel();
            }}
            aoVoltar={fecharPainel}
          />
        )}
        {painel === 'cliente' && venda && (
          <PainelCliente
            atual={venda.cliente}
            aoEscolher={async (id) => {
              if (await executar(() => api.vincularCliente(venda.id, id))) fecharPainel();
            }}
            aoVoltar={fecharPainel}
          />
        )}
        {painel === 'desconto' && venda && (
          <PainelDesconto
            venda={venda}
            aoAplicar={async (valor, percentual) => {
              if (await executar(() => api.desconto(venda.id, valor, percentual))) fecharPainel();
            }}
            aoVoltar={fecharPainel}
          />
        )}
        {painel === 'espera' && (
          <PainelEspera
            vendaAtual={podeReceber ? venda : null}
            emEspera={emEspera}
            aoEstacionar={async (ident) => {
              const v = await executar(() => api.espera(venda!.id, ident || undefined));
              if (v) {
                avisar(`Venda #${v.id} em espera.`, 'info');
                novaVenda();
              }
            }}
            aoRetomar={async (id) => {
              const v = await executar(() => api.retomar(id));
              if (v) {
                setModo('itens');
                setDanfe(null);
                recarregarEspera();
                fecharPainel();
              }
            }}
            aoVoltar={fecharPainel}
          />
        )}
        {painel === 'consulta' && (
          <PainelConsulta
            aoLancar={async (p) => {
              fecharPainel();
              if (modo === 'itens') await lancar({ produtoId: p.id, quantidade: 1 });
            }}
            aoVoltar={fecharPainel}
          />
        )}

        {painel === null && modo === 'itens' && (
          <div className="acoes">
            <button className="botao botao--principal botao--grande" disabled={!podeReceber} onClick={irParaPagamento}>
              Receber <kbd>F4</kbd>
            </button>
            <div className="acoes__grade">
              <button className="acao" disabled={!podeReceber} onClick={() => abrirPainel('cliente')}>
                <span>{venda?.cliente ? 'Trocar cliente' : 'Cliente'}</span>
                <kbd>F5</kbd>
              </button>
              <button className="acao" disabled={!podeReceber} onClick={() => abrirPainel('desconto')}>
                <span>{venda?.desconto ? `Desconto ${moeda(venda.desconto)}` : 'Desconto'}</span>
                <kbd>F6</kbd>
              </button>
              <button className="acao" disabled={!podeReceber} onClick={() => abrirPainel('cpf')}>
                <span>{venda?.documentoConsumidor ? 'CPF informado' : 'CPF na nota'}</span>
                <kbd>F3</kbd>
              </button>
              <button className="acao" onClick={() => setPainel('espera')}>
                <span>Espera</span>
                <kbd>F7</kbd>
              </button>
              <button className="acao acao--perigo" disabled={!vendaAberta} onClick={() => setPainel('cancelar')}>
                <span>Cancelar venda</span>
                <kbd>F8</kbd>
              </button>
            </div>
          </div>
        )}

        {painel === null && modo === 'pagamento' && venda && (
          <Pagamento
            venda={venda}
            ocupado={ocupado}
            aoPagar={(forma, valor, identificador) => executar(() => api.pagar(venda.id, forma, valor, identificador))}
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
            <div className="acoes__linha">
              <button className="botao botao--principal" onClick={novaVenda} autoFocus>
                Nova venda <kbd>Enter</kbd>
              </button>
              {danfe && (
                <button className="botao botao--fantasma" onClick={() => window.print()}>
                  Imprimir cupom <kbd>P</kbd>
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

function CaixaFechado({ nome, retaguarda }: { nome?: string; retaguarda?: boolean }) {
  if (retaguarda) {
    return (
      <div className="pdv pdv--fechado">
        <div className="fechado">
          <p className="sobretitulo">Retaguarda</p>
          <h1>Este computador não vende.</h1>
          <p>Ele está configurado como retaguarda (gerência). Para vender aqui, escolha um caixa na tela Caixa.</p>
          <Link className="botao botao--principal botao--grande" to="/caixa">
            Ir para Caixa
          </Link>
        </div>
      </div>
    );
  }
  return (
    <div className="pdv pdv--fechado">
      <div className="fechado">
        <p className="sobretitulo">{nome} · fechado</p>
        <h1>Abra o caixa para começar a vender.</h1>
        <p>Informe o dinheiro que está na gaveta (fundo de troco) e o PDV fica pronto para o leitor.</p>
        <Link className="botao botao--principal botao--grande" to="/caixa">
          Abrir caixa
        </Link>
      </div>
    </div>
  );
}

function Etiqueta({ total, rotulo, quantidade }: { total: number; rotulo: string; quantidade: number }) {
  const [inteiro, centavos] = numero(total).split(',');
  return (
    <div className="etiqueta" aria-label={`${rotulo}: ${moeda(total)}`}>
      <span className="etiqueta__furo" aria-hidden />
      <span className="etiqueta__rotulo">
        {rotulo}
        {quantidade > 0 && <span> · {quantidade} {quantidade === 1 ? 'item' : 'itens'}</span>}
      </span>
      <span className="etiqueta__valor" key={total}>
        <small>R$</small>
        {inteiro}
        <sup>,{centavos}</sup>
      </span>
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

// ------------------------------------------------------------------------- Leitor de códigos

const Leitor = forwardRef<
  LeitorHandle,
  { ocupado: boolean; aoLancar: (e: { codigo?: string; produtoId?: number; quantidade?: number }) => Promise<boolean> }
>(function Leitor({ ocupado, aoLancar }, ref) {
  const [texto, setTexto] = useState('');
  const [sugestoes, setSugestoes] = useState<Produto[]>([]);
  const [indice, setIndice] = useState(0);
  const campo = useRef<HTMLInputElement>(null);
  const { erro } = useAvisos();

  useImperativeHandle(ref, () => ({
    focar: () => campo.current?.focus(),
    preencher: (t, cursorNoInicio) => {
      setTexto(t);
      setTimeout(() => {
        campo.current?.focus();
        if (cursorNoInicio) campo.current?.setSelectionRange(0, 0);
      }, 0);
    },
  }));

  const { quantidade, termo, explicita } = interpretar(texto);
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
    }, 160);
    return () => clearTimeout(t);
  }, [termo, ehBusca]);

  const lancar = async (entrada: { codigo?: string; produtoId?: number }) => {
    if (explicita && (!Number.isFinite(quantidade) || quantidade <= 0)) {
      erro(new ApiError(422, 'QUANTIDADE_INVALIDA', 'Digite a quantidade antes do * (ex.: 0,350*015).'));
      return;
    }
    // Sem quantidade digitada, o servidor decide (1, ou o peso/preço da etiqueta de balança).
    const ok = await aoLancar({ ...entrada, quantidade: explicita ? quantidade : undefined });
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
        <svg className="leitor__icone" viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden>
          <path d="M3 5v14M6 5v14M10 5v14M13 5v14M17 5v14M21 5v14" />
        </svg>
        <input
          id="leitor"
          ref={campo}
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
        {explicita && Number.isFinite(quantidade) && <span className="leitor__qtd">× {qtd(quantidade)}</span>}
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
              <span>
                {p.nome}
                {p.emPromocao && <em className="selo selo--oferta">oferta</em>}
              </span>
              <small>{p.gtin ?? p.codigoInterno}</small>
              <small>{qtd(p.estoqueAtual)} {p.unidade}</small>
              <strong>{moeda(p.precoVigente)}</strong>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
});

/** "3*789..." → 3 unidades; "0,350*015" → 0,350 kg. */
function interpretar(texto: string) {
  const m = texto.trim().match(/^(\d*(?:[.,]\d+)?)\s*\*\s*(.*)$/);
  if (m) return { quantidade: parseValor(m[1]), termo: m[2].trim(), explicita: true };
  return { quantidade: 1, termo: texto.trim(), explicita: false };
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
