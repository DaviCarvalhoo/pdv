import { useCallback, useEffect, useState } from 'react';
import Painel from '../components/Painel';
import { api, type MovimentacaoEstoque, type Page, type Produto, type ProdutoRequest } from '../lib/api';
import { useAtalhos, useAvisos } from '../lib/contexto';
import { dataHora, moeda, parseValor, qtd } from '../lib/format';

export default function PaginaProdutos() {
  const { erro } = useAvisos();
  const [busca, setBusca] = useState('');
  const [todos, setTodos] = useState(false);
  const [pagina, setPagina] = useState(0);
  const [dados, setDados] = useState<Page<Produto> | null>(null);
  const [baixos, setBaixos] = useState<Produto[]>([]);
  const [soBaixos, setSoBaixos] = useState(false);
  const [ativos, setAtivos] = useState<number | null>(null);
  const [editando, setEditando] = useState<Produto | 'novo' | null>(null);

  const carregar = useCallback(() => {
    api.produtos(busca, pagina, todos).then(setDados).catch(erro);
    api.estoqueBaixo().then(setBaixos).catch(() => undefined);
    api.contagemAtivos().then((c) => setAtivos(c.ativos)).catch(() => undefined);
  }, [busca, pagina, todos, erro]);

  useEffect(() => {
    const t = setTimeout(carregar, 200);
    return () => clearTimeout(t);
  }, [carregar]);

  useAtalhos({ Insert: () => setEditando('novo') }, !editando);

  const lista = soBaixos ? baixos : dados?.content ?? [];

  return (
    <div className="pagina">
      <header className="pagina__topo">
        <div>
          <p className="sobretitulo">Cadastro</p>
          <h1>Produtos</h1>
        </div>
        <div className="pagina__acoes">
          {ativos !== null && <span className="contador">{ativos} ativos</span>}
          <button className="botao botao--principal" onClick={() => setEditando('novo')}>
            Novo produto <kbd>Ins</kbd>
          </button>
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
            <th>Código</th>
            <th>Produto</th>
            <th className="tabela__num">Preço</th>
            <th className="tabela__num">Estoque</th>
            <th>NCM</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {lista.map((p) => (
            <tr key={p.id} className={`tabela__clicavel ${p.ativo ? '' : 'tabela__inativa'}`} onClick={() => setEditando(p)}>
              <td className="tabela__codigo">
                {p.gtin ?? '—'}
                {p.codigoInterno && <small>int. {p.codigoInterno}</small>}
              </td>
              <td>{p.nome}</td>
              <td className="tabela__num">{moeda(p.preco)}</td>
              <td className="tabela__num">
                <span className={p.estoqueBaixo || p.estoqueAtual <= 0 ? 'negativo' : ''}>
                  {qtd(p.estoqueAtual)} {p.unidade}
                </span>
              </td>
              <td>{p.ncm ?? <span className="selo selo--falta">sem NCM</span>}</td>
              <td>{!p.ativo && <span className="selo">inativo</span>}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {lista.length === 0 && dados && (
        <p className="vazio-linha">
          {busca ? `Nada encontrado para “${busca}”.` : 'Nenhum produto ainda. Cadastre o primeiro com Ins.'}
        </p>
      )}

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

      <Painel
        titulo={editando === 'novo' ? 'Novo produto' : editando ? editando.nome : ''}
        aberto={!!editando}
        aoFechar={() => setEditando(null)}
        largura={560}
      >
        {editando && (
          <EdicaoProduto
            key={editando === 'novo' ? 'novo' : editando.id}
            produto={editando === 'novo' ? null : editando}
            aoSalvar={(p) => {
              carregar();
              setEditando(p);
            }}
          />
        )}
      </Painel>
    </div>
  );
}

// ---------------------------------------------------------------------------------------------

function EdicaoProduto({ produto, aoSalvar }: { produto: Produto | null; aoSalvar: (p: Produto) => void }) {
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
      {aba === 'dados' ? <FormProduto produto={produto} aoSalvar={aoSalvar} /> : <Estoque produto={produto!} aoMudar={aoSalvar} />}
    </>
  );
}

function FormProduto({ produto, aoSalvar }: { produto: Produto | null; aoSalvar: (p: Produto) => void }) {
  const { avisar, erro } = useAvisos();
  const [f, setF] = useState({
    nome: produto?.nome ?? '',
    preco: produto ? String(produto.preco).replace('.', ',') : '',
    gtin: produto?.gtin ?? '',
    codigoInterno: produto?.codigoInterno ?? '',
    unidade: produto?.unidade ?? 'UN',
    ncm: produto?.ncm ?? '',
    cfop: produto?.cfop ?? '5102',
    csosn: produto?.csosn ?? '102',
    origem: String(produto?.origem ?? 0),
    estoqueMinimo: produto?.estoqueMinimo != null ? String(produto.estoqueMinimo).replace('.', ',') : '',
    estoqueInicial: '',
  });
  const campo = (k: keyof typeof f) => ({
    value: f[k],
    onChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => setF({ ...f, [k]: e.target.value }),
  });

  const salvar = async (e: React.FormEvent) => {
    e.preventDefault();
    const req: ProdutoRequest = {
      nome: f.nome,
      preco: parseValor(f.preco),
      gtin: f.gtin || undefined,
      codigoInterno: f.codigoInterno || undefined,
      unidade: f.unidade,
      ncm: f.ncm || undefined,
      cfop: f.cfop,
      csosn: f.csosn,
      origem: Number(f.origem),
      estoqueMinimo: f.estoqueMinimo ? parseValor(f.estoqueMinimo) : null,
      estoqueInicial: !produto && f.estoqueInicial ? parseValor(f.estoqueInicial) : null,
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
      <label className="formulario__cheio">
        Nome
        <input required maxLength={120} {...campo('nome')} />
      </label>
      <label>
        Preço (R$)
        <input required inputMode="decimal" placeholder="0,00" {...campo('preco')} />
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
        Código interno
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

      <fieldset className="formulario__cheio formulario__fiscal">
        <legend>Dados fiscais (NFC-e) · confira com o contador</legend>
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
      </fieldset>

      <div className="formulario__cheio acoes__secundarias">
        <button className="botao botao--principal">{produto ? 'Salvar alterações' : 'Cadastrar'}</button>
        {produto && (
          <button type="button" className={`botao botao--fantasma ${produto.ativo ? 'botao--perigo' : ''}`} onClick={alternarAtivo}>
            {produto.ativo ? 'Desativar' : 'Reativar'}
          </button>
        )}
      </div>
    </form>
  );
}

const NOME_MOV: Record<MovimentacaoEstoque['tipo'], string> = {
  ENTRADA: 'Entrada',
  AJUSTE: 'Ajuste',
  SAIDA_VENDA: 'Venda',
  ESTORNO_VENDA: 'Estorno',
};

function Estoque({ produto, aoMudar }: { produto: Produto; aoMudar: (p: Produto) => void }) {
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
