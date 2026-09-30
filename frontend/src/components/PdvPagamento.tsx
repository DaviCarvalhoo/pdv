/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

import { QRCodeSVG } from 'qrcode.react';
import { useEffect, useRef, useState } from 'react';
import { api, type FormaPagamento, type Venda } from '../lib/api';
import { moeda, nomeForma, numero, parseValor } from '../lib/format';

const FORMAS: { forma: FormaPagamento; tecla: string }[] = [
  { forma: 'DINHEIRO', tecla: 'D' },
  { forma: 'PIX', tecla: 'P' },
  { forma: 'CARTAO_DEBITO', tecla: 'B' },
  { forma: 'CARTAO_CREDITO', tecla: 'C' },
  { forma: 'VALE_ALIMENTACAO', tecla: 'A' },
  { forma: 'VALE_REFEICAO', tecla: 'R' },
  { forma: 'CREDIARIO', tecla: 'F' },
  { forma: 'VALE_TROCA', tecla: 'V' },
];

export default function Pagamento({
  venda,
  ocupado,
  aoPagar,
  aoRemover,
  aoFinalizar,
  aoVoltar,
}: {
  venda: Venda;
  ocupado: boolean;
  aoPagar: (f: FormaPagamento, v: number, identificador?: string) => Promise<Venda | null>;
  aoRemover: (id: number) => void;
  aoFinalizar: () => void;
  aoVoltar: () => void;
}) {
  const [forma, setForma] = useState<FormaPagamento>('DINHEIRO');
  const [valor, setValor] = useState('');
  const [pix, setPix] = useState<{ valor: number; payload: string } | null>(null);
  const [pixErro, setPixErro] = useState('');
  const [codigoVale, setCodigoVale] = useState('');
  const [vale, setVale] = useState<{ codigo: string; saldo: number } | null>(null);
  const [valeErro, setValeErro] = useState('');
  const campo = useRef<HTMLInputElement>(null);
  const quitada = venda.restante <= 0;
  const disponiveis = FORMAS.filter((f) => f.forma !== 'CREDIARIO' || venda.cliente);

  useEffect(() => {
    setValor(venda.restante > 0 ? numero(venda.restante) : '');
    setTimeout(() => campo.current?.select(), 0);
  }, [venda.restante, forma]);

  // QR Code do PIX no valor digitado (BR Code estático com a chave da loja).
  const valorNum = parseValor(valor);
  useEffect(() => {
    if (forma !== 'PIX' || !Number.isFinite(valorNum) || valorNum <= 0) {
      setPix(null);
      return;
    }
    const t = setTimeout(() => {
      api
        .pix(venda.id, valorNum)
        .then((p) => {
          setPix(p);
          setPixErro('');
        })
        .catch((e) => {
          setPix(null);
          setPixErro(e.message);
        });
    }, 250);
    return () => clearTimeout(t);
  }, [forma, valorNum, venda.id]);

  // Vale-troca: consulta o saldo pelo código e sugere o menor entre saldo e restante.
  useEffect(() => {
    if (forma !== 'VALE_TROCA' || codigoVale.trim().length < 8) {
      setVale(null);
      setValeErro('');
      return;
    }
    const t = setTimeout(() => {
      api
        .vale(codigoVale)
        .then((v) => {
          setVale(v);
          setValeErro(v.saldo > 0 ? '' : 'Vale sem saldo.');
          if (v.saldo > 0) setValor(numero(Math.min(v.saldo, venda.restante)));
        })
        .catch(() => {
          setVale(null);
          setValeErro('Vale não encontrado.');
        });
    }, 250);
    return () => clearTimeout(t);
  }, [forma, codigoVale, venda.restante]);

  const escolher = (f: FormaPagamento) => {
    setForma(f);
    if (f !== 'VALE_TROCA') campo.current?.focus();
  };

  const lancar = async () => {
    if (!Number.isFinite(valorNum) || valorNum <= 0) return;
    if (forma === 'VALE_TROCA') {
      if (!vale) return;
      if (await aoPagar(forma, valorNum, vale.codigo)) setCodigoVale('');
      return;
    }
    await aoPagar(forma, valorNum);
  };

  const notas = [2, 5, 10, 20, 50, 100, 200].filter((n) => n > venda.restante).slice(0, 4);

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
            {disponiveis.map(({ forma: f, tecla }) => (
              <button key={f} role="radio" aria-checked={forma === f} className={`forma ${forma === f ? 'forma--ativa' : ''}`} onClick={() => escolher(f)}>
                <kbd>{tecla}</kbd>
                {nomeForma[f]}
              </button>
            ))}
          </div>
          {forma === 'CREDIARIO' && venda.cliente && (
            <p className="dica">
              Fiado de {venda.cliente.nome}: disponível {moeda(venda.cliente.creditoDisponivel)}.
            </p>
          )}
          {forma === 'VALE_TROCA' && (
            <div className="vale-campo">
              <label htmlFor="codigo-vale">Código do vale-troca</label>
              <input
                id="codigo-vale"
                autoFocus
                placeholder="VT······"
                maxLength={8}
                value={codigoVale}
                onChange={(e) => setCodigoVale(e.target.value.toUpperCase())}
                onKeyDown={(e) => e.key === 'Enter' && vale && campo.current?.focus()}
              />
              {vale && vale.saldo > 0 && <span className="pagamento__ok">Saldo {moeda(vale.saldo)}</span>}
              {valeErro && <span className="negativo">{valeErro}</span>}
            </div>
          )}
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
                  // Letras nunca fazem parte de um valor: trocam a forma sem atrapalhar a digitação.
                  const f = disponiveis.find((x) => x.tecla === e.key.toUpperCase() && e.key.length === 1);
                  if (f && !e.ctrlKey && !e.altKey && !e.metaKey) {
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
          </form>
          {forma === 'PIX' && (
            <div className="pix">
              {pix ? (
                <>
                  <QRCodeSVG value={pix.payload} size={148} level="M" marginSize={1} />
                  <div>
                    <strong>{moeda(pix.valor)}</strong>
                    <p>Mostre o QR Code ao cliente. Confira o recebimento no app do banco e aperte Enter.</p>
                    <button type="button" className="link" onClick={() => navigator.clipboard?.writeText(pix.payload)}>
                      Copiar PIX copia e cola
                    </button>
                  </div>
                </>
              ) : (
                <p className="dica">{pixErro || 'Gerando QR Code…'}</p>
              )}
            </div>
          )}
          <p className="dica">D dinheiro · P PIX · B débito · C crédito · A/R vales · V vale-troca{venda.cliente ? ' · F fiado' : ''}. Só dinheiro gera troco.</p>
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

      <div className="acoes__linha">
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
