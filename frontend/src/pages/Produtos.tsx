/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

import { useCallback, useEffect, useState } from 'react';
import BotaoExcluir from '../components/BotaoExcluir';
import Painel from '../components/Painel';
import { api, type Categoria, type MovimentacaoEstoque, type Page, type Produto, type ProdutoRequest } from '../lib/api';
import { useAtalhos, useAvisos, useSessao } from '../lib/contexto';
import { campoNumero, data, dataHora, moeda, parseValor, pct, qtd } from '../lib/format';

export default function PaginaProdutos() {
  const { erro } = useAvisos();
  const { pode } = useSessao();
  const gerente = pode('GERENTE');
  const [busca, setBusca] = useState('');
  const [todos, setTodos] = useState(false);
  const [pagina, setPagina] = useState(0);
  const [dados, setDados] = useState<Page<Produto> | null>(null);
  const [baixos, setBaixos] = useState<Produto[]>([]);
  const [soBaixos, setSoBaixos] = useState(false);
  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [editando, setEditando] = useState<Produto | 'novo' | null>(null);
  const [gerenciarCategorias, setGerenciarCategorias] = useState(false);
  const [etiquetas, setEtiquetas] = useState<Map<number, Produto>>(new Map());
  const [imprimindo, setImprimindo] = useState(false);

  const carregar = useCallback(() => {
    api.produtos(busca, pagina, todos).then(setDados).catch(erro);
    api.estoqueBaixo().then(setBaixos).catch(() => undefined);
    api.categorias().then(setCategorias).catch(() => undefined);
  }, [busca, pagina, todos, erro]);

  useEffect(() => {
    const t = setTimeout(carregar, 200);
    return () => clearTimeout(t);
  }, [carregar]);

  useAtalhos({ Insert: () => gerente && setEditando('novo') }, !editando);

  const lista = soBaixos ? baixos : (dados?.content ?? []);
  const alternarEtiqueta = (p: Produto) =>
    setEtiquetas((m) => {
      const n = new Map(m);
      if (n.has(p.id)) n.delete(p.id);
      else n.set(p.id, p);
      return n;
    });

  return (
    <div className="pagina pagina--larga">
      <header className="pagina__topo">
        <div>
          <p className="sobretitulo">Cadastro</p>
          <h1>Produtos</h1>
        </div>
        <div className="pagina__acoes">
          {etiquetas.size > 0 && (
            <button className="botao botao--secundario" onClick={() => setImprimindo(true)}>
              Imprimir {etiquetas.size} etiquetas
            </button>
          )}
          {gerente && (
            <>
              <button className="botao botao--fantasma" onClick={() => setGerenciarCategorias(true)}>
                Categorias
              </button>
              <button className="botao botao--principal" onClick={() => setEditando('novo')}>
                Novo produto <kbd>Ins</kbd>
              </button>
            </>
          )}
        </div>
      </header>

      <div className="filtros">
        <input
          className="filtros__busca"
          placeholder="Buscar por nome, GTIN ou código interno"
          value={busca}
          onChange={(e) => {
            setBusca(e.target.value);
            setPagina(0);
            setSoBaixos(false);
          }}
          autoFocus
        />
        <label className="alternador">
          <input type="checkbox" checked={todos} onChange={(e) => setTodos(e.target.checked)} />
          Mostrar inativos
        </label>
        {baixos.length > 0 && (
          <button className={`selo-botao ${soBaixos ? 'selo-botao--ativo' : ''}`} onClick={() => setSoBaixos((s) => !s)}>
            {baixos.length} com estoque baixo
          </button>
        )}
      </div>

      <table className="tabela">
        <thead>
          <tr>
            <th aria-label="Etiqueta" title="Marcar para imprimir etiqueta de gôndola">🏷</th>
            <th>Código</th>
            <th>Produto</th>
            <th className="tabela__num">Preço</th>
            <th className="tabela__num">Margem</th>
            <th className="tabela__num">Estoque</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {lista.map((p) => (
            <tr key={p.id} className={`tabela__clicavel ${p.ativo ? '' : 'tabela__inativa'}`} onClick={() => setEditando(p)}>
              <td onClick={(e) => e.stopPropagation()}>
                <input type="checkbox" checked={etiquetas.has(p.id)} onChange={() => alternarEtiqueta(p)} aria-label={`Etiqueta de ${p.nome}`} />
              </td>
              <td className="tabela__codigo">
                {p.gtin ?? '—'}
                {p.codigoInterno && <small>int. {p.codigoInterno}</small>}
              </td>
              <td>
                {p.categoriaCor && <i className="ponto-cor" style={{ background: p.categoriaCor }} title={p.categoriaNome} />}
                {p.nome}
                {p.emPromocao && <em className="selo selo--oferta">oferta</em>}
                {p.atalhoRapido && <em className="selo">atalho</em>}
              </td>
              <td className="tabela__num">
                {p.emPromocao && <s className="tabela__fraco">{moeda(p.preco)} </s>}
                {moeda(p.precoVigente)}
              </td>
              <td className={`tabela__num ${p.margem != null && p.margem < 15 ? 'negativo' : 'tabela__fraco'}`}>{pct(p.margem)}</td>
              <td className="tabela__num">
                <span className={p.estoqueBaixo || p.estoqueAtual <= 0 ? 'negativo' : ''}>
                  {qtd(p.estoqueAtual)} {p.unidade}
                </span>
              </td>
              <td>
                {!p.ncm && <span className="selo selo--falta">sem NCM</span>}
                {!p.ativo && <span className="selo">inativo</span>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {lista.length === 0 && dados && <p className="vazio-linha">{busca ? `Nada encontrado para “${busca}”.` : 'Nenhum produto ainda. Cadastre o primeiro com Ins.'}</p>}

      {!soBaixos && dados && dados.totalPages > 1 && (
        <div className="paginacao">
          <button className="botao botao--fantasma" disabled={pagina === 0} onClick={() => setPagina((p) => p - 1)}>
            ← Anterior
          </button>
          <span>
            {pagina + 1} de {dados.totalPages}
          </span>
          <button className="botao botao--fantasma" disabled={pagina + 1 >= dados.totalPages} onClick={() => setPagina((p) => p + 1)}>
            Próxima →
          </button>
        </div>
      )}

      <Painel titulo={editando === 'novo' ? 'Novo produto' : editando ? editando.nome : ''} aberto={!!editando} aoFechar={() => setEditando(null)} largura={600}>
        {editando && (
          <EdicaoProduto
            key={editando === 'novo' ? 'novo' : editando.id}
            produto={editando === 'novo' ? null : editando}
            categorias={categorias}
            podeEditar={gerente}
            aoSalvar={(p) => {
              carregar();
              setEditando(p);
            }}
            aoExcluir={() => {
              carregar();
              setEditando(null);
            }}
          />
        )}
      </Painel>

      <Painel titulo="Categorias" aberto={gerenciarCategorias} aoFechar={() => setGerenciarCategorias(false)}>
        <Categorias categorias={categorias} aoMudar={carregar} />
      </Painel>

      <Painel titulo="Etiquetas de gôndola" aberto={imprimindo} aoFechar={() => setImprimindo(false)} largura={720}>
        <p className="dica">Etiquetas de 60 × 35 mm, prontas para recortar e colocar na prateleira.</p>
        <div className="etiquetas-gondola impressao">
          {[...etiquetas.values()].map((p) => (
            <div key={p.id} className="etiqueta-gondola">
              <strong>{p.nome}</strong>
              {p.emPromocao && <s>{moeda(p.preco)}</s>}
              <span className="etiqueta-gondola__preco">
                {moeda(p.precoVigente)}
                {p.unidade !== 'UN' && <small>/{p.unidade.toLowerCase()}</small>}
              </span>
              <small>
                {p.gtin ?? p.codigoInterno} {p.emPromocao && p.promocaoFim && `· oferta até ${data(p.promocaoFim)}`}
              </small>
            </div>
          ))}
        </div>
        <div className="acoes__linha">
          <button className="botao botao--principal" onClick={() => window.print()}>
            Imprimir
          </button>
          <button className="botao botao--fantasma" onClick={() => { setEtiquetas(new Map()); setImprimindo(false); }}>
            Limpar seleção
          </button>
        </div>
      </Painel>
    </div>
  );
}

// ---------------------------------------------------------------------------------------------

function EdicaoProduto({ produto, categorias, podeEditar, aoSalvar, aoExcluir }: { produto: Produto | null; categorias: Categoria[]; podeEditar: boolean; aoSalvar: (p: Produto) => void; aoExcluir: () => void }) {
  const [aba, setAba] = useState<'dados' | 'estoque'>('dados');
  return (
    <>
      {produto && (
        <div className="abas" role="tablist">
          <button role="tab" aria-selected={aba === 'dados'} onClick={() => setAba('dados')}>
            Dados
          </button>
          <button role="tab" aria-selected={aba === 'estoque'} onClick={() => setAba('estoque')}>
            Estoque · {qtd(produto.estoqueAtual)} {produto.unidade}
          </button>
        </div>
      )}
      {aba === 'dados' ? (
        <FormProduto produto={produto} categorias={categorias} podeEditar={podeEditar} aoSalvar={aoSalvar} aoExcluir={aoExcluir} />
      ) : (
        <Estoque produto={produto!} podeEditar={podeEditar} aoMudar={aoSalvar} />
      )}
    </>
  );
}

function FormProduto({ produto, categorias, podeEditar, aoSalvar, aoExcluir }: { produto: Produto | null; categorias: Categoria[]; podeEditar: boolean; aoSalvar: (p: Produto) => void; aoExcluir: () => void }) {
  const { avisar, erro } = useAvisos();
  const [f, setF] = useState({
    nome: produto?.nome ?? '',
    preco: campoNumero(produto?.preco),
    precoCusto: campoNumero(produto?.precoCusto),
    gtin: produto?.gtin ?? '',
    codigoInterno: produto?.codigoInterno ?? '',
    unidade: produto?.unidade ?? 'UN',
    categoriaId: produto?.categoriaId ? String(produto.categoriaId) : '',
    ncm: produto?.ncm ?? '',
    cfop: produto?.cfop ?? '5102',
    csosn: produto?.csosn ?? '102',
    origem: String(produto?.origem ?? 0),
    estoqueMinimo: campoNumero(produto?.estoqueMinimo),
    estoqueInicial: '',
    precoPromocional: campoNumero(produto?.precoPromocional),
    promocaoInicio: produto?.promocaoInicio ?? '',
    promocaoFim: produto?.promocaoFim ?? '',
    aliquotaTributos: campoNumero(produto?.aliquotaTributos),
    atalhoRapido: produto?.atalhoRapido ?? false,
  });
  const campo = (k: keyof typeof f) => ({
    value: f[k] as string,
    onChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => setF({ ...f, [k]: e.target.value }),
    disabled: !podeEditar,
  });
  const preco = parseValor(f.preco);
  const custo = parseValor(f.precoCusto);
  const margem = preco > 0 && custo >= 0 ? ((preco - custo) / preco) * 100 : null;
  const num = (t: string) => (t ? parseValor(t) : null);

  const salvar = async (e: React.FormEvent) => {
    e.preventDefault();
    const req: ProdutoRequest = {
      nome: f.nome,
      preco,
      gtin: f.gtin || undefined,
      codigoInterno: f.codigoInterno || undefined,
      unidade: f.unidade,
      ncm: f.ncm || undefined,
      cfop: f.cfop,
      csosn: f.csosn,
      origem: Number(f.origem),
      estoqueMinimo: num(f.estoqueMinimo),
      estoqueInicial: !produto ? num(f.estoqueInicial) : null,
      categoriaId: f.categoriaId ? Number(f.categoriaId) : null,
      precoCusto: num(f.precoCusto),
      precoPromocional: num(f.precoPromocional),
      promocaoInicio: f.promocaoInicio || null,
      promocaoFim: f.promocaoFim || null,
      aliquotaTributos: num(f.aliquotaTributos),
      atalhoRapido: f.atalhoRapido,
    };
    try {
      const p = produto ? await api.atualizarProduto(produto.id, req) : await api.criarProduto(req);
      avisar(produto ? 'Produto atualizado.' : 'Produto cadastrado.');
      aoSalvar(p);
    } catch (err) {
      erro(err);
    }
  };

  const alternarAtivo = async () => {
    if (!produto) return;
    try {
      const p = produto.ativo ? await api.desativarProduto(produto.id) : await api.reativarProduto(produto.id);
      avisar(p.ativo ? 'Produto reativado.' : 'Produto desativado. Ele não aparece mais no PDV.');
      aoSalvar(p);
    } catch (err) {
      erro(err);
    }
  };

  return (
    <form className="formulario" onSubmit={salvar}>
      {!podeEditar && <p className="formulario__cheio faixa">Só gerentes alteram o cadastro.</p>}
      <label className="formulario__cheio">
        Nome
        <input required maxLength={120} {...campo('nome')} />
      </label>
      <label>
        Preço de venda (R$)
        <input required inputMode="decimal" placeholder="0,00" {...campo('preco')} />
      </label>
      <label>
        Custo (R$) {margem != null && Number.isFinite(margem) && <small className={margem < 15 ? 'negativo' : ''}>margem {margem.toFixed(1)}%</small>}
        <input inputMode="decimal" placeholder="opcional" {...campo('precoCusto')} />
      </label>
      <label>
        Categoria
        <select {...campo('categoriaId')}>
          <option value="">Sem categoria</option>
          {categorias.map((c) => (
            <option key={c.id} value={c.id}>
              {c.nome}
            </option>
          ))}
        </select>
      </label>
      <label>
        Unidade
        <select {...campo('unidade')}>
          <option value="UN">UN · unidade</option>
          <option value="KG">KG · quilo</option>
          <option value="LT">LT · litro</option>
          <option value="CX">CX · caixa</option>
          <option value="PCT">PCT · pacote</option>
        </select>
      </label>
      <label>
        GTIN / código de barras
        <input inputMode="numeric" maxLength={14} {...campo('gtin')} />
      </label>
      <label>
        Código interno <small>usado na etiqueta da balança</small>
        <input maxLength={30} {...campo('codigoInterno')} />
      </label>
      {!produto && (
        <label>
          Estoque inicial
          <input inputMode="decimal" placeholder="0" {...campo('estoqueInicial')} />
        </label>
      )}
      <label>
        Estoque mínimo
        <input inputMode="decimal" placeholder="opcional" {...campo('estoqueMinimo')} />
      </label>
      <label className="alternador formulario__cheio">
        <input type="checkbox" checked={f.atalhoRapido} disabled={!podeEditar} onChange={(e) => setF({ ...f, atalhoRapido: e.target.checked })} />
        Botão de acesso rápido no PDV (produto sem código de barras, como pão e cafezinho)
      </label>

      <fieldset className="formulario__cheio formulario__trio">
        <legend>Promoção</legend>
        <label>
          Preço promocional
          <input inputMode="decimal" placeholder="sem oferta" {...campo('precoPromocional')} />
        </label>
        <label>
          De
          <input type="date" {...campo('promocaoInicio')} />
        </label>
        <label>
          Até
          <input type="date" {...campo('promocaoFim')} />
        </label>
      </fieldset>

      <fieldset className="formulario__cheio formulario__fiscal">
        <legend>Fiscal (NFC-e) · confira com o contador</legend>
        <label>
          NCM
          <input inputMode="numeric" maxLength={8} placeholder="8 dígitos" {...campo('ncm')} />
        </label>
        <label>
          CFOP
          <input inputMode="numeric" maxLength={4} {...campo('cfop')} />
        </label>
        <label>
          CSOSN
          <select {...campo('csosn')}>
            <option value="102">102 · sem permissão de crédito</option>
            <option value="103">103 · isenção por faixa</option>
            <option value="300">300 · imune</option>
            <option value="400">400 · não tributada</option>
            <option value="500">500 · ICMS cobrado por ST</option>
          </select>
        </label>
        <label>
          Origem
          <select {...campo('origem')}>
            <option value="0">0 · nacional</option>
            <option value="1">1 · estrangeira (importação direta)</option>
            <option value="2">2 · estrangeira (mercado interno)</option>
          </select>
        </label>
        <label>
          Tributos aprox. (%) <small>vazio = o da loja</small>
          <input inputMode="decimal" {...campo('aliquotaTributos')} />
        </label>
      </fieldset>

      {podeEditar && (
        <div className="formulario__cheio acoes__linha">
          <button className="botao botao--principal">{produto ? 'Salvar alterações' : 'Cadastrar'}</button>
          {produto && (
            <button type="button" className="botao botao--fantasma" onClick={alternarAtivo}>
              {produto.ativo ? 'Desativar' : 'Reativar'}
            </button>
          )}
          {produto && (
            <BotaoExcluir
              oque="do produto"
              aoExcluir={async () => {
                try {
                  avisar((await api.excluirProduto(produto.id)).mensagem);
                  aoExcluir();
                } catch (err) {
                  erro(err);
                }
              }}
            />
          )}
        </div>
      )}
    </form>
  );
}

const NOME_MOV: Record<MovimentacaoEstoque['tipo'], string> = {
  ENTRADA: 'Entrada',
  AJUSTE: 'Ajuste',
  SAIDA_VENDA: 'Venda',
  ESTORNO_VENDA: 'Estorno',
};

function Estoque({ produto, podeEditar, aoMudar }: { produto: Produto; podeEditar: boolean; aoMudar: (p: Produto) => void }) {
  const { avisar, erro } = useAvisos();
  const [movs, setMovs] = useState<MovimentacaoEstoque[]>([]);
  const [tipo, setTipo] = useState<'entrada' | 'ajuste'>('entrada');
  const [quantidade, setQuantidade] = useState('');
  const [obs, setObs] = useState('');

  const carregar = useCallback(() => {
    api.movimentacoesEstoque(produto.id).then((p) => setMovs(p.content)).catch(erro);
  }, [produto.id, erro]);
  useEffect(carregar, [carregar]);

  const lancar = async (e: React.FormEvent) => {
    e.preventDefault();
    const q = parseValor(quantidade);
    try {
      if (tipo === 'entrada') await api.entradaEstoque(produto.id, q, obs || undefined);
      else await api.ajusteEstoque(produto.id, q, obs);
      avisar(tipo === 'entrada' ? 'Entrada registrada.' : 'Ajuste registrado.');
      setQuantidade('');
      setObs('');
      carregar();
      aoMudar(await api.produto(produto.id));
    } catch (err) {
      erro(err);
    }
  };

  return (
    <div className="estoque">
      <div className="estoque__saldo">
        <span>Saldo atual</span>
        <strong className={produto.estoqueAtual <= 0 ? 'negativo' : ''}>
          {qtd(produto.estoqueAtual)} <small>{produto.unidade}</small>
        </strong>
        {produto.estoqueMinimo != null && <em>mínimo {qtd(produto.estoqueMinimo)}</em>}
      </div>

      {podeEditar && (
        <form className="formulario" onSubmit={lancar}>
          <div className="segmentado formulario__cheio" role="radiogroup">
            <button type="button" role="radio" aria-checked={tipo === 'entrada'} onClick={() => setTipo('entrada')}>
              Entrada (compra)
            </button>
            <button type="button" role="radio" aria-checked={tipo === 'ajuste'} onClick={() => setTipo('ajuste')}>
              Ajuste (inventário, perda)
            </button>
          </div>
          <label>
            Quantidade {tipo === 'ajuste' && <small>(use − para baixar)</small>}
            <input required inputMode="decimal" value={quantidade} onChange={(e) => setQuantidade(e.target.value)} />
          </label>
          <label>
            {tipo === 'ajuste' ? 'Motivo (obrigatório)' : 'Observação'}
            <input required={tipo === 'ajuste'} value={obs} onChange={(e) => setObs(e.target.value)} />
          </label>
          <div className="formulario__cheio">
            <button className="botao botao--principal">Registrar</button>
          </div>
        </form>
      )}

      <h3>Histórico</h3>
      <table className="tabela tabela--compacta">
        <tbody>
          {movs.map((m) => (
            <tr key={m.id}>
              <td className="tabela__hora">{dataHora(m.dataHora)}</td>
              <td>
                {NOME_MOV[m.tipo]}
                {m.observacao && <small> · {m.observacao}</small>}
              </td>
              <td className={`tabela__num ${m.quantidade < 0 ? 'negativo' : ''}`}>
                {m.quantidade > 0 ? '+' : ''}
                {qtd(m.quantidade)}
              </td>
              <td className="tabela__num tabela__fraco">{qtd(m.saldoPosterior)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {movs.length === 0 && <p className="vazio-linha">Sem movimentações.</p>}
    </div>
  );
}

function Categorias({ categorias, aoMudar }: { categorias: Categoria[]; aoMudar: () => void }) {
  const { erro } = useAvisos();
  const [nome, setNome] = useState('');
  const [cor, setCor] = useState('#2F6FDB');
  return (
    <div className="categorias">
      <form
        className="categorias__nova"
        onSubmit={async (e) => {
          e.preventDefault();
          try {
            await api.criarCategoria({ nome, cor });
            setNome('');
            aoMudar();
          } catch (err) {
            erro(err);
          }
        }}
      >
        <input type="color" value={cor} onChange={(e) => setCor(e.target.value)} aria-label="Cor" />
        <input required placeholder="Nova categoria" maxLength={40} value={nome} onChange={(e) => setNome(e.target.value)} />
        <button className="botao botao--principal">Criar</button>
      </form>
      <p className="dica">Ao excluir uma categoria, os produtos dela ficam sem categoria. Nenhum produto é apagado.</p>
      <ul className="lista-simples">
        {categorias.map((c) => (
          <li key={c.id}>
            <i className="ponto-cor" style={{ background: c.cor }} />
            <span>{c.nome}</span>
            <BotaoExcluir
              oque="da categoria"
              aoExcluir={async () => {
                try {
                  await api.excluirCategoria(c.id);
                  aoMudar();
                } catch (err) {
                  erro(err);
                }
              }}
            />
          </li>
        ))}
      </ul>
    </div>
  );
}
