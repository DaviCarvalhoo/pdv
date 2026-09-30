// Cliente da API REST do Balcão PDV. Os tipos espelham os DTOs do backend.

export type FormaPagamento = 'DINHEIRO' | 'PIX' | 'CARTAO_DEBITO' | 'CARTAO_CREDITO';
export type StatusVenda = 'ABERTA' | 'FINALIZADA' | 'CANCELADA' | 'ESTORNADA';
export type StatusNota = 'PENDENTE' | 'AUTORIZADA' | 'REJEITADA' | 'CANCELADA';
export type Ambiente = 'PRODUCAO' | 'HOMOLOGACAO';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
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

export interface Caixa {
  id: number;
  status: 'ABERTO' | 'FECHADO';
  saldoInicial: number;
  saldoEsperado?: number;
  dataAbertura: string;
  dataFechamento?: string;
  valorContado?: number;
  diferenca?: number;
  situacaoConferencia?: 'CONFERE' | 'SOBRA' | 'FALTA';
}

export interface MovimentacaoCaixa {
  id: number;
  tipo: 'SUPRIMENTO' | 'SANGRIA' | 'VENDA_DINHEIRO' | 'ESTORNO_VENDA';
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
  quantidade: number;
  subtotal: number;
  estoqueDisponivel: number;
}

export interface PagamentoVenda {
  id: number;
  forma: FormaPagamento;
  valor: number;
  identificadorTransacao?: string;
  dataHora: string;
}

export interface Venda {
  id: number;
  caixaId: number;
  status: StatusVenda;
  itens: ItemVenda[];
  pagamentos: PagamentoVenda[];
  quantidadeItens: number;
  total: number;
  valorPago: number;
  restante: number;
  troco: number;
  documentoConsumidor?: string;
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
  troco: number;
  formas: FormaPagamento[];
  dataAbertura: string;
  dataFinalizacao?: string;
}

export interface Historico {
  vendas: Page<VendaResumo>;
  totalizadores: { quantidade: number; valorTotal: number };
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
  valorTotal: number;
  pagamentos: { forma: FormaPagamento; valor: number }[];
  troco: number;
  urlConsulta: string;
  urlQrCode: string;
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

async function request<T>(method: string, url: string, body?: unknown): Promise<T> {
  let resp: Response;
  try {
    resp = await fetch('/api' + url, {
      method,
      headers: body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'SEM_CONEXAO', 'Sem conexão com o servidor. Verifique se o backend está no ar.');
  }
  if (resp.status === 204) return undefined as T;
  const texto = await resp.text();
  const dados = texto ? JSON.parse(texto) : undefined;
  if (!resp.ok) {
    let mensagem: string = dados?.detail ?? `Erro ${resp.status}`;
    if (dados?.campos) mensagem = Object.values(dados.campos as Record<string, string>).join(' · ');
    throw new ApiError(resp.status, dados?.codigo ?? 'ERRO', mensagem, dados ?? {});
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
  // Produtos
  produtos: (q?: string, page = 0, todos = false) =>
    request<Page<Produto>>('GET', '/produtos' + qs({ q, page, size: 20, todos })),
  produto: (id: number) => request<Produto>('GET', `/produtos/${id}`),
  produtoPorCodigo: (codigo: string) => request<Produto>('GET', '/produtos/codigo/' + encodeURIComponent(codigo)),
  contagemAtivos: () => request<{ ativos: number }>('GET', '/produtos/contagem-ativos'),
  estoqueBaixo: () => request<Produto[]>('GET', '/produtos/estoque-baixo'),
  criarProduto: (p: ProdutoRequest) => request<Produto>('POST', '/produtos', p),
  atualizarProduto: (id: number, p: ProdutoRequest) => request<Produto>('PUT', `/produtos/${id}`, p),
  desativarProduto: (id: number) => request<Produto>('PATCH', `/produtos/${id}/desativar`),
  reativarProduto: (id: number) => request<Produto>('PATCH', `/produtos/${id}/reativar`),
  entradaEstoque: (id: number, quantidade: number, observacao?: string) =>
    request<MovimentacaoEstoque>('POST', `/produtos/${id}/estoque/entradas`, { quantidade, observacao }),
  ajusteEstoque: (id: number, quantidade: number, observacao: string) =>
    request<MovimentacaoEstoque>('POST', `/produtos/${id}/estoque/ajustes`, { quantidade, observacao }),
  movimentacoesEstoque: (id: number) =>
    request<Page<MovimentacaoEstoque>>('GET', `/produtos/${id}/estoque/movimentacoes?size=50`),

  // Caixa
  caixaAberto: () => request<Caixa | undefined>('GET', '/caixas/aberto'),
  abrirCaixa: (saldoInicial: number) => request<Caixa>('POST', '/caixas', { saldoInicial }),
  suprimento: (id: number, valor: number, descricao?: string) =>
    request<MovimentacaoCaixa>('POST', `/caixas/${id}/suprimentos`, { valor, descricao }),
  sangria: (id: number, valor: number, descricao?: string) =>
    request<MovimentacaoCaixa>('POST', `/caixas/${id}/sangrias`, { valor, descricao }),
  fecharCaixa: (id: number, valorContado: number) =>
    request<Extrato>('POST', `/caixas/${id}/fechar`, { valorContado }),
  extrato: (id: number) => request<Extrato>('GET', `/caixas/${id}/extrato`),
  caixas: (inicio?: string, fim?: string) => request<Page<Caixa>>('GET', '/caixas' + qs({ inicio, fim, size: 50 })),

  // Vendas
  vendaAberta: () => request<Venda | undefined>('GET', '/vendas/aberta'),
  iniciarVenda: () => request<Venda>('POST', '/vendas'),
  venda: (id: number) => request<Venda>('GET', `/vendas/${id}`),
  adicionarItem: (id: number, codigo: string, quantidade: number) =>
    request<Venda>('POST', `/vendas/${id}/itens`, { codigo, quantidade }),
  adicionarProduto: (id: number, produtoId: number, quantidade: number) =>
    request<Venda>('POST', `/vendas/${id}/itens`, { produtoId, quantidade }),
  alterarQuantidade: (id: number, itemId: number, quantidade: number) =>
    request<Venda>('PATCH', `/vendas/${id}/itens/${itemId}`, { quantidade }),
  removerItem: (id: number, itemId: number) => request<Venda>('DELETE', `/vendas/${id}/itens/${itemId}`),
  pagar: (id: number, forma: FormaPagamento, valor: number, identificadorTransacao?: string) =>
    request<Venda>('POST', `/vendas/${id}/pagamentos`, { forma, valor, identificadorTransacao }),
  removerPagamento: (id: number, pagamentoId: number) =>
    request<Venda>('DELETE', `/vendas/${id}/pagamentos/${pagamentoId}`),
  consumidor: (id: number, documento: string) => request<Venda>('PUT', `/vendas/${id}/consumidor`, { documento }),
  finalizar: (id: number) => request<Venda>('POST', `/vendas/${id}/finalizar`),
  cancelarVenda: (id: number, motivo?: string) => request<Venda>('POST', `/vendas/${id}/cancelar`, { motivo }),
  estornarVenda: (id: number, motivo?: string) => request<Venda>('POST', `/vendas/${id}/estornar`, { motivo }),
  historico: (f: { inicio?: string; fim?: string; status?: string; forma?: string; caixaId?: string; page?: number }) =>
    request<Historico>('GET', '/vendas' + qs({ ...f, size: 20 })),

  // Fiscal
  configuracaoFiscal: () => request<ConfiguracaoFiscal>('GET', '/fiscal/configuracao'),
  salvarConfiguracaoFiscal: (c: Partial<ConfiguracaoFiscal>) =>
    request<ConfiguracaoFiscal>('PUT', '/fiscal/configuracao', c),
  emitirNfce: (vendaId: number) => request<NotaResumo>('POST', `/fiscal/vendas/${vendaId}/nfce`),
  notas: (f: { inicio?: string; fim?: string; status?: string; page?: number }) =>
    request<Page<NotaResumo>>('GET', '/fiscal/notas' + qs({ ...f, size: 20 })),
  danfe: (notaId: number) => request<Danfe>('GET', `/fiscal/notas/${notaId}/danfe`),
  cancelarNota: (notaId: number, justificativa: string) =>
    request<NotaResumo>('POST', `/fiscal/notas/${notaId}/cancelar`, { justificativa }),
  xmlUrl: (notaId: number) => `/api/fiscal/notas/${notaId}/xml`,
};
