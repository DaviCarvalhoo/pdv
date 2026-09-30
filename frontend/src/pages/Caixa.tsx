import { useCallback, useEffect, useState } from 'react';
import ExtratoCaixa from '../components/ExtratoCaixa';
import Painel from '../components/Painel';
import { api, type Caixa, type Extrato } from '../lib/api';
import { useAvisos, useCaixa } from '../lib/contexto';
import { dataHora, moeda, parseValor } from '../lib/format';

export default function PaginaCaixa() {
  const { caixa, recarregar } = useCaixa();
  const { avisar, erro } = useAvisos();
  const [extrato, setExtrato] = useState<Extrato | null>(null);
  const [fechamento, setFechamento] = useState<Extrato | null>(null);
  const [historico, setHistorico] = useState<Caixa[]>([]);
  const [detalhe, setDetalhe] = useState<Extrato | null>(null);

  const carregarExtrato = useCallback(() => {
    if (caixa) api.extrato(caixa.id).then(setExtrato).catch(erro);
  }, [caixa, erro]);

  useEffect(carregarExtrato, [carregarExtrato]);
  useEffect(() => {
    api.caixas().then((p) => setHistorico(p.content)).catch(() => undefined);
  }, [caixa, fechamento]);

  const aposMovimento = async () => {
    await recarregar();
    carregarExtrato();
  };

  return (
    <div className="pagina">
      <header className="pagina__topo">
        <div>
          <p className="sobretitulo">Caixa</p>
          <h1>{caixa ? `Caixa #${caixa.id}` : 'Caixa fechado'}</h1>
        </div>
        {caixa && (
          <div className="gaveta">
            <span>Na gaveta agora</span>
            <strong>{moeda(caixa.saldoEsperado)}</strong>
          </div>
        )}
      </header>

      {fechamento && (
        <section className="bloco bloco--destaque">
          <h2>Fechamento do caixa #{fechamento.caixa.id}</h2>
          <ExtratoCaixa e={fechamento} />
          <button className="botao botao--fantasma" onClick={() => setFechamento(null)}>
            Ok, entendi
          </button>
        </section>
      )}

      {!caixa && !fechamento && <Abertura aoAbrir={recarregar} />}

      {caixa && (
        <div className="grade-caixa">
          <section className="bloco">
            <h2>Movimentar gaveta</h2>
            <Movimento
              titulo="Suprimento"
              descricao="Dinheiro que entra na gaveta (ex.: reforço de troco)."
              aoLancar={async (v, d) => {
                await api.suprimento(caixa.id, v, d);
                avisar(`Suprimento de ${moeda(v)} lançado.`);
                aposMovimento();
              }}
            />
            <Movimento
              titulo="Sangria"
              descricao="Dinheiro retirado da gaveta (ex.: levar ao cofre)."
              aoLancar={async (v, d) => {
                await api.sangria(caixa.id, v, d);
                avisar(`Sangria de ${moeda(v)} lançada.`);
                aposMovimento();
              }}
            />
          </section>

          <section className="bloco bloco--fechar">
            <h2>Fechar caixa</h2>
            <p className="dica">
              Conte o dinheiro da gaveta e informe o total. O sistema compara com o esperado e mostra sobra ou falta.
            </p>
            <Fechamento
              aoFechar={async (contado) => {
                const e = await api.fecharCaixa(caixa.id, contado);
                setFechamento(e);
                setExtrato(null);
                await recarregar();
              }}
            />
          </section>

          {extrato && (
            <section className="bloco bloco--largo">
              <h2>Extrato parcial</h2>
              <ExtratoCaixa e={extrato} />
            </section>
          )}
        </div>
      )}

      {historico.length > 0 && (
        <section className="bloco bloco--largo">
          <h2>Últimos caixas</h2>
          <table className="tabela">
            <thead>
              <tr>
                <th>#</th>
                <th>Abertura</th>
                <th>Fechamento</th>
                <th className="tabela__num">Esperado</th>
                <th className="tabela__num">Contado</th>
                <th>Conferência</th>
              </tr>
            </thead>
            <tbody>
              {historico.map((c) => (
                <tr key={c.id} className="tabela__clicavel" onClick={() => api.extrato(c.id).then(setDetalhe).catch(erro)}>
                  <td>{c.id}</td>
                  <td>{dataHora(c.dataAbertura)}</td>
                  <td>{c.dataFechamento ? dataHora(c.dataFechamento) : <span className="selo selo--ok">aberto</span>}</td>
                  <td className="tabela__num">{moeda(c.saldoEsperado)}</td>
                  <td className="tabela__num">{c.valorContado != null ? moeda(c.valorContado) : '—'}</td>
                  <td>
                    {c.situacaoConferencia && (
                      <span className={`selo selo--${c.situacaoConferencia.toLowerCase()}`}>
                        {c.situacaoConferencia.toLowerCase()} {c.diferenca ? moeda(Math.abs(c.diferenca)) : ''}
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      <Painel titulo={detalhe ? `Extrato do caixa #${detalhe.caixa.id}` : ''} aberto={!!detalhe} aoFechar={() => setDetalhe(null)} largura={720}>
        {detalhe && <ExtratoCaixa e={detalhe} />}
      </Painel>
    </div>
  );
}

function Abertura({ aoAbrir }: { aoAbrir: () => Promise<void> }) {
  const [valor, setValor] = useState('');
  const { avisar, erro } = useAvisos();
  return (
    <section className="abertura">
      <p className="sobretitulo">Começar o dia</p>
      <h2>Quanto tem na gaveta para troco?</h2>
      <form
        onSubmit={async (e) => {
          e.preventDefault();
          const v = parseValor(valor || '0');
          if (!Number.isFinite(v) || v < 0) return avisar('Informe um valor válido, como 150,00.', 'erro');
          try {
            const c = await api.abrirCaixa(v);
            avisar(`Caixa #${c.id} aberto com ${moeda(c.saldoInicial)}. Bom trabalho!`);
            await aoAbrir();
          } catch (err) {
            erro(err);
          }
        }}
      >
        <div className="campo-moeda campo-moeda--grande">
          <span>R$</span>
          <input autoFocus inputMode="decimal" placeholder="0,00" value={valor} onChange={(e) => setValor(e.target.value)} />
          <button className="botao botao--principal botao--grande">
            Abrir caixa <kbd>Enter</kbd>
          </button>
        </div>
      </form>
    </section>
  );
}

function Movimento({
  titulo,
  descricao,
  aoLancar,
}: {
  titulo: string;
  descricao: string;
  aoLancar: (valor: number, descricao?: string) => Promise<void>;
}) {
  const [valor, setValor] = useState('');
  const [obs, setObs] = useState('');
  const { erro } = useAvisos();
  return (
    <form
      className="movimento"
      onSubmit={async (e) => {
        e.preventDefault();
        try {
          await aoLancar(parseValor(valor), obs || undefined);
          setValor('');
          setObs('');
        } catch (err) {
          erro(err);
        }
      }}
    >
      <div className="movimento__titulo">
        <strong>{titulo}</strong>
        <span>{descricao}</span>
      </div>
      <div className="movimento__campos">
        <div className="campo-moeda">
          <span>R$</span>
          <input inputMode="decimal" placeholder="0,00" value={valor} onChange={(e) => setValor(e.target.value)} required aria-label={`Valor do ${titulo.toLowerCase()}`} />
        </div>
        <input placeholder="Observação (opcional)" value={obs} onChange={(e) => setObs(e.target.value)} />
        <button className="botao botao--secundario">Lançar</button>
      </div>
    </form>
  );
}

function Fechamento({ aoFechar }: { aoFechar: (contado: number) => Promise<void> }) {
  const [valor, setValor] = useState('');
  const [confirmar, setConfirmar] = useState(false);
  const { erro } = useAvisos();
  const contado = parseValor(valor);
  return (
    <form
      onSubmit={async (e) => {
        e.preventDefault();
        if (!Number.isFinite(contado) || contado < 0) return;
        if (!confirmar) return setConfirmar(true);
        try {
          await aoFechar(contado);
        } catch (err) {
          erro(err);
          setConfirmar(false);
        }
      }}
    >
      <div className="campo-moeda">
        <span>R$</span>
        <input
          inputMode="decimal"
          placeholder="Valor contado"
          value={valor}
          onChange={(e) => {
            setValor(e.target.value);
            setConfirmar(false);
          }}
          aria-label="Valor contado na gaveta"
        />
        <button className={`botao ${confirmar ? 'botao--perigo-cheio' : 'botao--secundario'}`}>
          {confirmar ? `Confirmar fechamento com ${moeda(contado)}` : 'Fechar caixa'}
        </button>
      </div>
    </form>
  );
}
