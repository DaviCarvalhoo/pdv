import type { FormaPagamento, StatusNota, StatusVenda } from './api';

const moedaFmt = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const numeroFmt = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const qtdFmt = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 3 });
const dataHoraFmt = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
const horaFmt = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' });

export const moeda = (v: number | undefined | null) => moedaFmt.format(v ?? 0);
export const numero = (v: number | undefined | null) => numeroFmt.format(v ?? 0);
export const qtd = (v: number) => qtdFmt.format(v);
export const dataHora = (iso?: string) => (iso ? dataHoraFmt.format(new Date(iso)) : '—');
export const hora = (iso?: string) => (iso ? horaFmt.format(new Date(iso)) : '');

/** Aceita "12,50", "12.50" ou "1.234,56". */
export function parseValor(texto: string): number {
  const t = texto.trim();
  if (!t) return NaN;
  const normalizado = t.includes(',') ? t.replace(/\./g, '').replace(',', '.') : t;
  return Number(normalizado);
}

export function documento(d?: string) {
  if (!d) return '';
  if (d.length === 11) return d.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
  if (d.length === 14) return d.replace(/(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})/, '$1.$2.$3/$4-$5');
  return d;
}

export const nomeForma: Record<FormaPagamento, string> = {
  DINHEIRO: 'Dinheiro',
  PIX: 'PIX',
  CARTAO_DEBITO: 'Débito',
  CARTAO_CREDITO: 'Crédito',
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

export const hoje = () => new Date().toLocaleDateString('sv-SE');
export const diasAtras = (n: number) => new Date(Date.now() - n * 86400000).toLocaleDateString('sv-SE');
