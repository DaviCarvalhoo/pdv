/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

// Cliente da API REST do Balcão PDV. Os tipos espelham os DTOs do backend.

export type FormaPagamento =
  | 'DINHEIRO'
  | 'PIX'
  | 'CARTAO_DEBITO'
  | 'CARTAO_CREDITO'
  | 'VALE_ALIMENTACAO'
  | 'VALE_REFEICAO'
  | 'CREDIARIO'
  | 'VALE_TROCA';
export type StatusVenda = 'ABERTA' | 'FINALIZADA' | 'CANCELADA' | 'ESTORNADA';
export type StatusNota = 'PENDENTE' | 'AUTORIZADA' | 'REJEITADA' | 'CANCELADA';
export type Ambiente = 'PRODUCAO' | 'HOMOLOGACAO';
export type Papel = 'OPERADOR' | 'GERENTE' | 'ADMIN';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface Operador {
  id: number;
  nome: string;
  papel: Papel;
}

export interface Sessao {
  token: string;
  operador: Operador;
  expiraEm: string;
}

export interface LojaPublica {
  nomeFantasia: string;
  slogan?: string;
  logo?: string;
  corDestaque: string;
}

export interface Loja extends LojaPublica {
  mensagemCupom?: string;
  politicaEstoque: 'PERMITIR_E_AVISAR' | 'BLOQUEAR';
  limiteGaveta?: number;
  descontoMaxOperador: number;
  balancaPrefixo: string;
  balancaDigitosCodigo: number;
  balancaTipoValor: 'PRECO' | 'PESO';
  chavePix?: string;
  pixRecebedor?: string;
  pixCidade?: string;
  pixConfigurado: boolean;
  aliquotaTributos?: number;
  bloqueioInatividadeMin: number;
  caixaSoGerente: boolean;
}

export interface Categoria {
  id: number;
  nome: string;
  cor?: string;
}

export interface Produto {
  id: number;
  codigoInterno?: string;
  gtin?: string;
  nome: string;
  preco: number;
  unidade: string;
  ncm?: string;
  cfop: string;
  origem: number;
  csosn: string;
  estoqueAtual: number;
  estoqueMinimo?: number;
  estoqueBaixo: boolean;
  ativo: boolean;
  categoriaId?: number;
  categoriaNome?: string;
  categoriaCor?: string;
  precoCusto?: number;
  margem?: number;
  precoPromocional?: number;
  promocaoInicio?: string;
  promocaoFim?: string;
  emPromocao: boolean;
  precoVigente: number;
  atalhoRapido: boolean;
  aliquotaTributos?: number;
}

export interface ProdutoRequest {
  nome: string;
  preco: number;
  codigoInterno?: string;
  gtin?: string;
  unidade?: string;
  ncm?: string;
  cfop?: string;
  origem?: number;
  csosn?: string;
  estoqueMinimo?: number | null;
  estoqueInicial?: number | null;
  categoriaId?: number | null;
  precoCusto?: number | null;
  precoPromocional?: number | null;
  promocaoInicio?: string | null;
  promocaoFim?: string | null;
  atalhoRapido?: boolean;
  aliquotaTributos?: number | null;
}

export interface MovimentacaoEstoque {
  id: number;
  tipo: 'ENTRADA' | 'AJUSTE' | 'SAIDA_VENDA' | 'ESTORNO_VENDA';
  quantidade: number;
  saldoAnterior: number;
  saldoPosterior: number;
  vendaId?: number;
  observacao?: string;
  dataHora: string;
}

export interface Terminal {
  id: number;
  nome: string;
  ativo: boolean;
  ultimoUso?: string;
  caixaId?: number;
  abertoEm?: string;
  gaveta?: number;
}

/** O que este computador é: um caixa (terminal) ou só retaguarda (gerência, sem vender). */
export type EsteComputador = { id: number; nome: string } | { retaguarda: true };

export interface Caixa {
  id: number;
  terminalId: number;
  terminalNome: string;
  status: 'ABERTO' | 'FECHADO';
  saldoInicial: number;
  saldoEsperado?: number;
  dataAbertura: string;
  dataFechamento?: string;
  valorContado?: number;
  diferenca?: number;
  situacaoConferencia?: 'CONFERE' | 'SOBRA' | 'FALTA';
  alertaSangriaLimite?: number;
}

export interface MovimentacaoCaixa {
  id: number;
  tipo: 'SUPRIMENTO' | 'SANGRIA' | 'VENDA_DINHEIRO' | 'RECEBIMENTO_CLIENTE' | 'ESTORNO_VENDA';
  valor: number;
  descricao?: string;
  vendaId?: number;
  dataHora: string;
}

export interface Extrato {
  caixa: Caixa;
  saldoInicial: number;
  suprimentos: number;
  sangrias: number;
  vendasDinheiro: number;
  recebimentosClientes: number;
  estornos: number;
  saldoEsperado: number;
  valorContado?: number;
  diferenca?: number;
  situacaoConferencia?: 'CONFERE' | 'SOBRA' | 'FALTA';
  totaisPorForma: Record<FormaPagamento, number>;
  vendasFinalizadas: number;
  vendasCanceladas: number;
  vendasEstornadas: number;
  totalVendido: number;
  ticketMedio: number;
  movimentacoes: MovimentacaoCaixa[];
}

export interface NotaResumo {
  id: number;
  vendaId: number;
  serie: number;
  numero: number;
  chaveAcesso: string;
  ambiente: Ambiente;
  status: StatusNota;
  protocolo?: string;
  motivo?: string;
  dataEmissao: string;
  dataAutorizacao?: string;
  dataCancelamento?: string;
  valor: number;
}

export interface ItemVenda {
  id: number;
  produtoId: number;
  codigo?: string;
  descricao: string;
  unidade: string;
  precoUnitario: number;
  precoNormal: number;
  promocional: boolean;
  quantidade: number;
  subtotal: number;
  estoqueDisponivel: number;
  quantidadeDevolvida: number;
}

export interface Devolucao {
  devolucaoId: number;
  vendaId: number;
  valor: number;
  destino: 'DINHEIRO' | 'VALE_TROCA';
  codigoVale?: string;
  dataHora: string;
}

export interface PagamentoVenda {
  id: number;
  forma: FormaPagamento;
  valor: number;
  identificadorTransacao?: string;
  dataHora: string;
}

export interface ClienteVenda {
  id: number;
  nome: string;
  documento?: string;
  saldoDevedor: number;
  creditoDisponivel: number;
}

export interface Venda {
  id: number;
  caixaId: number;
  status: StatusVenda;
  itens: ItemVenda[];
  pagamentos: PagamentoVenda[];
  quantidadeItens: number;
  subtotal: number;
  desconto: number;
  descontoPercentual?: number;
  total: number;
  valorPago: number;
  restante: number;
  troco: number;
  tributosAprox: number;
  documentoConsumidor?: string;
  cliente?: ClienteVenda;
  operadorNome?: string;
  emEspera: boolean;
  identificacao?: string;
  dataAbertura: string;
  dataFinalizacao?: string;
  dataCancelamento?: string;
  motivoCancelamento?: string;
  notaFiscal?: NotaResumo;
  avisos: string[];
}

export interface VendaResumo {
  id: number;
  caixaId: number;
  status: StatusVenda;
  quantidadeItens: number;
  total: number;
  desconto: number;
  troco: number;
  formas: FormaPagamento[];
  operadorNome?: string;
  clienteNome?: string;
  dataAbertura: string;
  dataFinalizacao?: string;
}

export interface Historico {
  vendas: Page<VendaResumo>;
  totalizadores: { quantidade: number; valorTotal: number };
}

export interface Cliente {
  id: number;
  nome: string;
  documento?: string;
  telefone?: string;
  email?: string;
  limiteCredito: number;
  saldoDevedor: number;
  creditoDisponivel: number;
  observacao?: string;
  ativo: boolean;
  criadoEm: string;
}

export interface LancamentoCliente {
  id: number;
  tipo: 'COMPRA' | 'PAGAMENTO' | 'ESTORNO';
  valor: number;
  forma?: string;
  vendaId?: number;
  observacao?: string;
  saldoApos: number;
  dataHora: string;
}

export interface Usuario {
  id: number;
  nome: string;
  papel: Papel;
  ativo: boolean;
  criadoEm: string;
  ultimoAcesso?: string;
}

export interface Serie {
  rotulo: string;
  valor: number;
  quantidade: number;
}

export interface Resumo {
  vendas: number;
  faturamento: number;
  ticketMedio: number;
  itens: number;
  descontos: number;
  lucroBruto: number;
  margem?: number;
  coberturaCusto?: number;
  canceladas: number;
}

export interface Painel {
  dia: string;
  hoje: Resumo;
  ontem: Resumo;
  porHora: Serie[];
  porForma: Serie[];
  porCategoria: Serie[];
  porOperador: Serie[];
  porCaixa: Serie[];
  ultimos30Dias: Serie[];
  maisVendidos: { produtoId: number; nome: string; quantidade: number; valor: number; lucro?: number }[];
  alertas: {
    estoqueBaixo: number;
    estoqueZerado: number;
    notasComProblema: number;
    fiadoAReceber: number;
    gaveta?: number;
    limiteGaveta?: number;
    vendasEmEspera: number;
    caixasAbertos: number;
    caixasAcimaDoLimite: number;
  };
}

export interface CurvaAbc {
  inicio: string;
  fim: string;
  total: number;
  itens: { produtoId: number; nome: string; quantidade: number; valor: number; percentual: number; acumulado: number; classe: 'A' | 'B' | 'C' }[];
  produtosPorClasse: Record<'A' | 'B' | 'C', number>;
}

export interface PreviaNfe {
  fornecedor?: string;
  cnpjFornecedor?: string;
  numero?: string;
  serie?: string;
  chave?: string;
  valorTotal?: number;
  itens: {
    numero: number;
    codigoFornecedor?: string;
    gtin?: string;
    descricao: string;
    ncm?: string;
    unidade: string;
    quantidade: number;
    custoUnitario: number;
    valorTotal: number;
    precoSugerido?: number;
    produto?: { id: number; nome: string; preco: number; precoCusto?: number; unidade: string };
  }[];
}

export interface ItemEntradaNfe {
  produtoId?: number | null;
  gtin?: string | null;
  nome: string;
  ncm?: string | null;
  unidade: string;
  quantidade: number;
  custoUnitario: number;
  precoVenda?: number | null;
  ignorar: boolean;
}

export interface ConfiguracaoFiscal {
  cnpj?: string;
  inscricaoEstadual?: string;
  razaoSocial?: string;
  nomeFantasia?: string;
  crt?: number;
  logradouro?: string;
  numero?: string;
  bairro?: string;
  codigoMunicipio?: string;
  municipio?: string;
  uf?: string;
  cep?: string;
  telefone?: string;
  ambiente: Ambiente;
  serie: number;
  proximoNumero: number;
  cscId?: string;
  cscMascarado?: string;
  cscConfigurado: boolean;
  csc?: string;
  urlQrCode?: string;
  urlConsulta?: string;
  emissaoAutomatica: boolean;
  prazoCancelamentoMin: number;
  emissor: 'SIMULADO';
  pendencias: string[];
}

export interface Danfe {
  emitente: { razaoSocial: string; nomeFantasia?: string; cnpj: string; inscricaoEstadual: string; endereco: string };
  notaId: number;
  vendaId: number;
  serie: number;
  numero: number;
  status: StatusNota;
  ambiente: Ambiente;
  dataEmissao: string;
  chaveAcesso: string;
  chaveFormatada: string;
  protocolo?: string;
  dataAutorizacao?: string;
  consumidor?: string;
  itens: { numero: number; codigo: string; descricao: string; quantidade: number; unidade: string; valorUnitario: number; valorTotal: number }[];
  quantidadeItens: number;
  subtotal: number;
  desconto: number;
  valorTotal: number;
  pagamentos: { forma: FormaPagamento; valor: number }[];
  troco: number;
  tributosAprox: number;
  urlConsulta: string;
  urlQrCode: string;
  logo?: string;
  mensagemCupom?: string;
  operador?: string;
}

/** Resultado de uma exclusão: apagado de vez, ou arquivado para preservar o histórico. */
export interface Exclusao {
  apagado: boolean;
  mensagem: string;
}

/** Erro no formato Problem Details do backend. */
export class ApiError extends Error {
  constructor(
    public status: number,
    public codigo: string,
    message: string,
    public extra: Record<string, unknown> = {},
  ) {
    super(message);
  }
}

// ------------------------------------------------------------------------------ Sessão

const CHAVE_SESSAO = 'balcao.sessao';

export function sessaoSalva(): Sessao | null {
  try {
    const s = localStorage.getItem(CHAVE_SESSAO);
    const sessao = s ? (JSON.parse(s) as Sessao) : null;
    return sessao && new Date(sessao.expiraEm) > new Date() ? sessao : null;
  } catch {
    return null;
  }
}

let tokenAtual: string | null = sessaoSalva()?.token ?? null;

const CHAVE_TERMINAL = 'balcao.terminal';

export function terminalSalvo(): EsteComputador | null {
  try {
    const t = localStorage.getItem(CHAVE_TERMINAL);
    return t ? (JSON.parse(t) as EsteComputador) : null;
  } catch {
    return null;
  }
}

export function guardarTerminal(t: EsteComputador | null) {
  try {
    if (t) localStorage.setItem(CHAVE_TERMINAL, JSON.stringify(t));
    else localStorage.removeItem(CHAVE_TERMINAL);
  } catch {
    /* sem armazenamento: vale só nesta aba */
  }
  terminalAtual = t && 'id' in t ? t.id : null;
}

let terminalAtual: number | null = (() => {
  const t = terminalSalvo();
  return t && 'id' in t ? t.id : null;
})();

/** Avisos de conexão: a tela mostra "sem conexão" e tenta reconectar sozinha. */
let aoMudarConexao: (online: boolean) => void = () => undefined;
export function aoConexao(fn: (online: boolean) => void) {
  aoMudarConexao = fn;
}
let aoExpirar: () => void = () => undefined;
let pedirAutorizacao: ((motivo: string) => Promise<string | null>) | null = null;

export function guardarSessao(s: Sessao | null) {
  tokenAtual = s?.token ?? null;
  try {
    if (s) localStorage.setItem(CHAVE_SESSAO, JSON.stringify(s));
    else localStorage.removeItem(CHAVE_SESSAO);
  } catch {
    /* navegação privada: a sessão vale só enquanto a aba estiver aberta */
  }
}

export function aoSessaoExpirar(fn: () => void) {
  aoExpirar = fn;
}

/** A tela registra quem pede o PIN do gerente; a API chama quando o backend exige autorização. */
export function definirPedidoDeAutorizacao(fn: ((motivo: string) => Promise<string | null>) | null) {
  pedirAutorizacao = fn;
}

async function request<T>(method: string, url: string, body?: unknown, autorizacao?: string): Promise<T> {
  const headers: Record<string, string> = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (tokenAtual) headers.Authorization = 'Bearer ' + tokenAtual;
  if (autorizacao) headers['X-Autorizacao'] = autorizacao;
  if (terminalAtual) headers['X-Terminal'] = String(terminalAtual);
  let resp: Response;
  try {
    resp = await fetch('/api' + url, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
  } catch {
    aoMudarConexao(false);
    throw new ApiError(0, 'SEM_CONEXAO', 'Sem conexão com o servidor. A operação não foi concluída; tente de novo quando a conexão voltar.');
  }
  aoMudarConexao(true);
  if (resp.status === 204) return undefined as T;
  const texto = await resp.text();
  const dados = texto ? JSON.parse(texto) : undefined;
  if (!resp.ok) {
    let mensagem: string = dados?.detail ?? `Erro ${resp.status}`;
    if (dados?.campos) mensagem = Object.values(dados.campos as Record<string, string>).join(' · ');
    const erro = new ApiError(resp.status, dados?.codigo ?? 'ERRO', mensagem, dados ?? {});
    if (resp.status === 401 && tokenAtual && !url.startsWith('/auth/')) {
      guardarSessao(null);
      aoExpirar();
    }
    // Ação que precisa de gerente: pede o PIN e tenta de novo, uma vez.
    if (erro.codigo === 'AUTORIZACAO_NECESSARIA' && !autorizacao && pedirAutorizacao) {
      const token = await pedirAutorizacao(mensagem);
      if (token) return request<T>(method, url, body, token);
      throw new ApiError(403, 'AUTORIZACAO_CANCELADA', 'Operação cancelada: sem autorização do gerente.');
    }
    throw erro;
  }
  return dados as T;
}

const qs = (params: Record<string, string | number | boolean | undefined | null>) => {
  const s = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') s.set(k, String(v));
  });
  const t = s.toString();
  return t ? '?' + t : '';
};

export const api = {
  // Acesso
  estadoAcesso: () => request<{ precisaPrimeiroAcesso: boolean; operadores: Operador[] }>('GET', '/auth/estado'),
  primeiroAcesso: (nome: string, pin: string) => request<Sessao>('POST', '/auth/primeiro-acesso', { nome, pin }),
  entrar: (usuarioId: number, pin: string) => request<Sessao>('POST', '/auth/entrar', { usuarioId, pin }),
  sair: () => request<void>('POST', '/auth/sair'),
  autorizar: (pin: string) => request<{ autorizacao: string }>('POST', '/auth/autorizar', { pin }),
  desbloquear: (pin: string) => request<void>('POST', '/usuarios/eu/desbloquear', { pin }),
  usuarios: () => request<Usuario[]>('GET', '/usuarios'),
  criarUsuario: (u: { nome: string; papel: Papel; pin: string }) => request<Usuario>('POST', '/usuarios', u),
  atualizarUsuario: (id: number, u: { nome: string; papel: Papel; ativo: boolean }) =>
    request<Usuario>('PUT', `/usuarios/${id}`, u),
  excluirUsuario: (id: number) => request<Exclusao>('DELETE', `/usuarios/${id}`),
  redefinirPin: (id: number, pin: string) => request<void>('PUT', `/usuarios/${id}/pin`, { pin }),

  // Loja
  lojaPublica: () => request<LojaPublica>('GET', '/loja/publica'),
  loja: () => request<Loja>('GET', '/loja'),
  salvarLoja: (l: Partial<Loja>) => request<Loja>('PUT', '/loja', l),

  // Produtos e categorias
  produtos: (q?: string, page = 0, todos = false, size = 20) =>
    request<Page<Produto>>('GET', '/produtos' + qs({ q, page, size, todos })),
  produto: (id: number) => request<Produto>('GET', `/produtos/${id}`),
  produtoPorCodigo: (codigo: string) => request<Produto>('GET', '/produtos/codigo/' + encodeURIComponent(codigo)),
  atalhos: () => request<Produto[]>('GET', '/produtos/atalhos'),
  contagemAtivos: () => request<{ ativos: number }>('GET', '/produtos/contagem-ativos'),
  estoqueBaixo: () => request<Produto[]>('GET', '/produtos/estoque-baixo'),
  criarProduto: (p: ProdutoRequest) => request<Produto>('POST', '/produtos', p),
  atualizarProduto: (id: number, p: ProdutoRequest) => request<Produto>('PUT', `/produtos/${id}`, p),
  desativarProduto: (id: number) => request<Produto>('PATCH', `/produtos/${id}/desativar`),
  excluirProduto: (id: number) => request<Exclusao>('DELETE', `/produtos/${id}`),
  reativarProduto: (id: number) => request<Produto>('PATCH', `/produtos/${id}/reativar`),
  entradaEstoque: (id: number, quantidade: number, observacao?: string) =>
    request<MovimentacaoEstoque>('POST', `/produtos/${id}/estoque/entradas`, { quantidade, observacao }),
  ajusteEstoque: (id: number, quantidade: number, observacao: string) =>
    request<MovimentacaoEstoque>('POST', `/produtos/${id}/estoque/ajustes`, { quantidade, observacao }),
  movimentacoesEstoque: (id: number) =>
    request<Page<MovimentacaoEstoque>>('GET', `/produtos/${id}/estoque/movimentacoes?size=50`),
  categorias: () => request<Categoria[]>('GET', '/categorias'),
  criarCategoria: (c: { nome: string; cor?: string }) => request<Categoria>('POST', '/categorias', c),
  atualizarCategoria: (id: number, c: { nome: string; cor?: string }) => request<Categoria>('PUT', `/categorias/${id}`, c),
  excluirCategoria: (id: number) => request<void>('DELETE', `/categorias/${id}`),
  lerNfe: (xml: string) => request<PreviaNfe>('POST', '/estoque/nfe/ler', { xml }),
  confirmarNfe: (itens: ItemEntradaNfe[], referencia?: string) =>
    request<{ atualizados: number; criados: number; ignorados: number }>('POST', '/estoque/nfe/confirmar', { itens, referencia }),

  // Clientes
  clientes: (q?: string, page = 0, devedores = false, todos = false) =>
    request<Page<Cliente>>('GET', '/clientes' + qs({ q, page, devedores, todos, size: 20 })),
  cliente: (id: number) => request<Cliente>('GET', `/clientes/${id}`),
  salvarCliente: (id: number | null, c: Partial<Cliente>) =>
    id ? request<Cliente>('PUT', `/clientes/${id}`, c) : request<Cliente>('POST', '/clientes', c),
  excluirCliente: (id: number) => request<Exclusao>('DELETE', `/clientes/${id}`),
  extratoCliente: (id: number) => request<Page<LancamentoCliente>>('GET', `/clientes/${id}/extrato`),
  receberCliente: (id: number, valor: number, forma: FormaPagamento, observacao?: string) =>
    request<LancamentoCliente>('POST', `/clientes/${id}/recebimentos`, { valor, forma, observacao }),
  aReceber: () => request<{ total: number }>('GET', '/clientes/a-receber'),

  // Caixa
  caixaAberto: () => request<Caixa | undefined>('GET', '/caixas/aberto'),
  caixasAbertos: () => request<Caixa[]>('GET', '/caixas/abertos'),
  terminais: () => request<Terminal[]>('GET', '/terminais'),
  criarTerminal: (nome: string) => request<Terminal>('POST', '/terminais', { nome }),
  atualizarTerminal: (id: number, nome: string, ativo: boolean) => request<Terminal>('PUT', `/terminais/${id}`, { nome, ativo }),
  excluirTerminal: (id: number) => request<Exclusao>('DELETE', `/terminais/${id}`),
  abrirCaixa: (saldoInicial: number) => request<Caixa>('POST', '/caixas', { saldoInicial }),
  suprimento: (id: number, valor: number, descricao?: string) =>
    request<MovimentacaoCaixa>('POST', `/caixas/${id}/suprimentos`, { valor, descricao }),
  sangria: (id: number, valor: number, descricao?: string) =>
    request<MovimentacaoCaixa>('POST', `/caixas/${id}/sangrias`, { valor, descricao }),
  fecharCaixa: (id: number, valorContado: number) => request<Extrato>('POST', `/caixas/${id}/fechar`, { valorContado }),
  extrato: (id: number) => request<Extrato>('GET', `/caixas/${id}/extrato`),
  caixas: (inicio?: string, fim?: string) => request<Page<Caixa>>('GET', '/caixas' + qs({ inicio, fim, size: 50 })),

  // Vendas
  vendaAberta: () => request<Venda | undefined>('GET', '/vendas/aberta'),
  vendasEmEspera: () => request<VendaResumo[]>('GET', '/vendas/em-espera'),
  iniciarVenda: () => request<Venda>('POST', '/vendas'),
  venda: (id: number) => request<Venda>('GET', `/vendas/${id}`),
  adicionarItem: (id: number, codigo: string, quantidade?: number) =>
    request<Venda>('POST', `/vendas/${id}/itens`, { codigo, quantidade }),
  adicionarProduto: (id: number, produtoId: number, quantidade: number) =>
    request<Venda>('POST', `/vendas/${id}/itens`, { produtoId, quantidade }),
  alterarQuantidade: (id: number, itemId: number, quantidade: number) =>
    request<Venda>('PATCH', `/vendas/${id}/itens/${itemId}`, { quantidade }),
  removerItem: (id: number, itemId: number) => request<Venda>('DELETE', `/vendas/${id}/itens/${itemId}`),
  desconto: (id: number, valor?: number | null, percentual?: number | null) =>
    request<Venda>('PUT', `/vendas/${id}/desconto`, { valor, percentual }),
  pagar: (id: number, forma: FormaPagamento, valor: number, identificadorTransacao?: string | null) =>
    request<Venda>('POST', `/vendas/${id}/pagamentos`, { forma, valor, identificadorTransacao }),
  removerPagamento: (id: number, pagamentoId: number) => request<Venda>('DELETE', `/vendas/${id}/pagamentos/${pagamentoId}`),
  pix: (id: number, valor?: number) => request<{ valor: number; payload: string }>('GET', `/vendas/${id}/pix` + qs({ valor })),
  consumidor: (id: number, documento: string) => request<Venda>('PUT', `/vendas/${id}/consumidor`, { documento }),
  vincularCliente: (id: number, clienteId: number | null) => request<Venda>('PUT', `/vendas/${id}/cliente`, { clienteId }),
  espera: (id: number, identificacao?: string) => request<Venda>('POST', `/vendas/${id}/espera`, { identificacao }),
  retomar: (id: number) => request<Venda>('POST', `/vendas/${id}/retomar`),
  finalizar: (id: number) => request<Venda>('POST', `/vendas/${id}/finalizar`),
  cancelarVenda: (id: number, motivo?: string) => request<Venda>('POST', `/vendas/${id}/cancelar`, { motivo }),
  estornarVenda: (id: number, motivo?: string) => request<Venda>('POST', `/vendas/${id}/estornar`, { motivo }),
  devolver: (id: number, itens: { itemId: number; quantidade: number }[], destino: 'DINHEIRO' | 'VALE_TROCA', motivo?: string) =>
    request<Devolucao>('POST', `/vendas/${id}/devolucoes`, { itens, destino, motivo }),
  devolucoes: (id: number) => request<Devolucao[]>('GET', `/vendas/${id}/devolucoes`),
  vale: (codigo: string) =>
    request<{ codigo: string; valor: number; saldo: number; criadoEm: string }>('GET', `/vendas/vales/${encodeURIComponent(codigo.trim())}`),
  historico: (f: Record<string, string | number | undefined>) => request<Historico>('GET', '/vendas' + qs({ size: 20, ...f })),

  // Relatórios
  painel: (dia?: string) => request<Painel>('GET', '/relatorios/painel' + qs({ dia })),
  curvaAbc: (inicio?: string, fim?: string) => request<CurvaAbc>('GET', '/relatorios/curva-abc' + qs({ inicio, fim })),

  // Fiscal
  configuracaoFiscal: () => request<ConfiguracaoFiscal>('GET', '/fiscal/configuracao'),
  salvarConfiguracaoFiscal: (c: Partial<ConfiguracaoFiscal>) => request<ConfiguracaoFiscal>('PUT', '/fiscal/configuracao', c),
  emitirNfce: (vendaId: number) => request<NotaResumo>('POST', `/fiscal/vendas/${vendaId}/nfce`),
  notas: (f: { inicio?: string; fim?: string; status?: string; page?: number }) =>
    request<Page<NotaResumo>>('GET', '/fiscal/notas' + qs({ ...f, size: 20 })),
  danfe: (notaId: number) => request<Danfe>('GET', `/fiscal/notas/${notaId}/danfe`),
  cancelarNota: (notaId: number, justificativa: string) =>
    request<NotaResumo>('POST', `/fiscal/notas/${notaId}/cancelar`, { justificativa }),
  /** Baixa o XML com o token (o link direto não levaria o cabeçalho de autenticação). */
  baixarXml: async (notaId: number, chave: string) => {
    const resp = await fetch(`/api/fiscal/notas/${notaId}/xml`, { headers: { Authorization: 'Bearer ' + tokenAtual } });
    if (!resp.ok) throw new ApiError(resp.status, 'ERRO', 'Não foi possível baixar o XML.');
    baixarArquivo(`NFCe${chave}.xml`, await resp.blob());
  },
};

export function baixarArquivo(nome: string, conteudo: Blob | string, tipo = 'text/csv;charset=utf-8') {
  const blob = typeof conteudo === 'string' ? new Blob(['﻿' + conteudo], { type: tipo }) : conteudo;
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = nome;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

/** CSV com ponto-e-vírgula e vírgula decimal, que abre direto no Excel em português. */
export function csv(linhas: (string | number | undefined | null)[][]) {
  return linhas
    .map((l) =>
      l
        .map((c) => {
          const v = typeof c === 'number' ? String(c).replace('.', ',') : (c ?? '');
          return /[;"\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v;
        })
        .join(';'),
    )
    .join('\n');
}
