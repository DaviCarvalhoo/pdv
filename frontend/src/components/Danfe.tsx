import { QRCodeSVG } from 'qrcode.react';
import type { Danfe as DanfeDados } from '../lib/api';
import { dataHora, documento, nomeForma, numero, qtd } from '../lib/format';

/** DANFE NFC-e no formato de cupom 80 mm. Na impressão, só este bloco aparece. */
export default function Danfe({ d }: { d: DanfeDados }) {
  const homologacao = d.ambiente === 'HOMOLOGACAO';
  return (
    <article className="danfe" aria-label={`DANFE NFC-e número ${d.numero}`}>
      <header className="danfe__topo">
        {d.logo && <img className="danfe__logo" src={d.logo} alt="" />}
        <strong>{d.emitente.nomeFantasia || d.emitente.razaoSocial}</strong>
        <span>{d.emitente.razaoSocial}</span>
        <span>
          CNPJ {documento(d.emitente.cnpj)} · IE {d.emitente.inscricaoEstadual}
        </span>
        <span>{d.emitente.endereco}</span>
      </header>

      <p className="danfe__titulo">DANFE NFC-e · Documento Auxiliar da Nota Fiscal de Consumidor Eletrônica</p>
      {homologacao && <p className="danfe__alerta">EMITIDA EM AMBIENTE DE HOMOLOGAÇÃO · SEM VALOR FISCAL</p>}
      {d.status === 'CANCELADA' && <p className="danfe__alerta">NOTA CANCELADA</p>}

      <table className="danfe__itens">
        <thead>
          <tr>
            <th>#</th>
            <th>Descrição</th>
            <th>Qtd</th>
            <th>Vl un</th>
            <th>Total</th>
          </tr>
        </thead>
        <tbody>
          {d.itens.map((i) => (
            <tr key={i.numero}>
              <td>{String(i.numero).padStart(3, '0')}</td>
              <td>
                {i.descricao}
                <small>{i.codigo}</small>
              </td>
              <td>
                {qtd(i.quantidade)} {i.unidade}
              </td>
              <td>{numero(i.valorUnitario)}</td>
              <td>{numero(i.valorTotal)}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <dl className="danfe__totais">
        <dt>Qtd. total de itens</dt>
        <dd>{d.quantidadeItens}</dd>
        {d.desconto > 0 && (
          <>
            <dt>Valor total R$</dt>
            <dd>{numero(d.subtotal)}</dd>
            <dt>Desconto R$</dt>
            <dd>-{numero(d.desconto)}</dd>
          </>
        )}
        <dt className="danfe__forte">Valor a pagar R$</dt>
        <dd className="danfe__forte">{numero(d.valorTotal)}</dd>
        <dt>Forma de pagamento</dt>
        <dd>Valor pago R$</dd>
        {d.pagamentos.map((p, idx) => (
          <FragmentoPagamento key={idx} forma={nomeForma[p.forma]} valor={p.valor} />
        ))}
        {d.troco > 0 && (
          <>
            <dt>Troco R$</dt>
            <dd>{numero(d.troco)}</dd>
          </>
        )}
      </dl>

      <section className="danfe__consulta">
        <p>Consulte pela chave de acesso em</p>
        <p className="danfe__url">{d.urlConsulta}</p>
        <p className="danfe__chave">{d.chaveFormatada}</p>
      </section>

      <p className="danfe__consumidor">
        {d.consumidor ? `CONSUMIDOR · ${d.consumidor.length === 11 ? 'CPF' : 'CNPJ'} ${documento(d.consumidor)}` : 'CONSUMIDOR NÃO IDENTIFICADO'}
      </p>

      {d.tributosAprox > 0 && (
        <p className="danfe__tributos">
          Tributos totais incidentes (Lei Federal 12.741/2012): R$ {numero(d.tributosAprox)} (fonte: IBPT)
        </p>
      )}

      <div className="danfe__rodape">
        <QRCodeSVG value={d.urlQrCode} size={132} level="M" marginSize={0} bgColor="transparent" fgColor="currentColor" />
        <div>
          <p>
            <strong>NFC-e nº {String(d.numero).padStart(9, '0')}</strong> · Série {String(d.serie).padStart(3, '0')}
          </p>
          <p>Emissão {dataHora(d.dataEmissao)}</p>
          {d.protocolo && (
            <p>
              Protocolo de autorização
              <br />
              {d.protocolo}
            </p>
          )}
          {d.dataAutorizacao && <p>Autorização {dataHora(d.dataAutorizacao)}</p>}
          {d.operador && <p>Operador: {d.operador}</p>}
        </div>
      </div>
      {d.mensagemCupom && <p className="danfe__mensagem">{d.mensagemCupom}</p>}
    </article>
  );
}

function FragmentoPagamento({ forma, valor }: { forma: string; valor: number }) {
  return (
    <>
      <dt>{forma}</dt>
      <dd>{numero(valor)}</dd>
    </>
  );
}
