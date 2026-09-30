import type { FormaPagamento, Papel, StatusNota, StatusVenda } from './api';

const moedaFmt = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const moedaCurtaFmt = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', notation: 'compact', maximumFractionDigits: 1 });
const numeroFmt = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const qtdFmt = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 3 });
const pctFmt = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 });
const dataHoraFmt = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
const dataFmt = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short' });
const horaFmt = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' });

export const moeda = (v: number | undefined | null) => moedaFmt.format(v ?? 0);
export const moedaCurta = (v: number) => (Math.abs(v) >= 10000 ? moedaCurtaFmt.format(v) : moedaFmt.format(v));
export const numero = (v: number | undefined | null) => numeroFmt.format(v ?? 0);
export const qtd = (v: number) => qtdFmt.format(v);
export const pct = (v: number | undefined | null) => (v == null ? '—' : pctFmt.format(v) + '%');
export const dataHora = (iso?: string) => (iso ? dataHoraFmt.format(new Date(iso)) : '—');
export const data = (iso?: string) => (iso ? dataFmt.format(new Date(iso.length === 10 ? iso + 'T12:00:00' : iso)) : '—');
export const hora = (iso?: string) => (iso ? horaFmt.format(new Date(iso)) : '');

/** Aceita "12,50", "12.50" ou "1.234,56". */
export function parseValor(texto: string): number {
  const t = String(texto ?? '').trim();
  if (!t) return NaN;
  const normalizado = t.includes(',') ? t.replace(/\./g, '').replace(',', '.') : t;
  return Number(normalizado);
}

/** Número para campo de formulário (vírgula decimal). */
export const campoNumero = (v: number | undefined | null) => (v == null ? '' : String(v).replace('.', ','));

export function documento(d?: string) {
  if (!d) return '';
  if (d.length === 11) return d.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
  if (d.length === 14) return d.replace(/(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})/, '$1.$2.$3/$4-$5');
  return d;
}

export function telefone(t?: string) {
  if (!t) return '';
  if (t.length === 11) return t.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
  if (t.length === 10) return t.replace(/(\d{2})(\d{4})(\d{4})/, '($1) $2-$3');
  return t;
}

export const nomeForma: Record<FormaPagamento, string> = {
  DINHEIRO: 'Dinheiro',
  PIX: 'PIX',
  CARTAO_DEBITO: 'Débito',
  CARTAO_CREDITO: 'Crédito',
  VALE_ALIMENTACAO: 'Vale-alimentação',
  VALE_REFEICAO: 'Vale-refeição',
  CREDIARIO: 'Fiado',
};

export const nomeStatusVenda: Record<StatusVenda, string> = {
  ABERTA: 'Aberta',
  FINALIZADA: 'Finalizada',
  CANCELADA: 'Cancelada',
  ESTORNADA: 'Estornada',
};

export const nomeStatusNota: Record<StatusNota, string> = {
  PENDENTE: 'Pendente',
  AUTORIZADA: 'Autorizada',
  REJEITADA: 'Rejeitada',
  CANCELADA: 'Cancelada',
};

export const nomePapel: Record<Papel, string> = {
  OPERADOR: 'Operador',
  GERENTE: 'Gerente',
  ADMIN: 'Administrador',
};

export const podePapel = (atual: Papel | undefined, minimo: Papel) => {
  const ordem: Papel[] = ['OPERADOR', 'GERENTE', 'ADMIN'];
  return !!atual && ordem.indexOf(atual) >= ordem.indexOf(minimo);
};

export const hoje = () => new Date().toLocaleDateString('sv-SE');
export const diasAtras = (n: number) => new Date(Date.now() - n * 86400000).toLocaleDateString('sv-SE');

export const iniciais = (nome: string) =>
  nome
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]!.toUpperCase())
    .join('');

/** Variação percentual entre dois valores, para "vs. ontem". */
export function variacao(atual: number, anterior: number) {
  if (!anterior) return atual ? null : 0;
  return ((atual - anterior) / anterior) * 100;
}
