/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

import { useCallback, useEffect, useState } from 'react';
import Danfe from '../components/Danfe';
import Painel from '../components/Painel';
import { api, type ConfiguracaoFiscal, type Danfe as DanfeDados, type NotaResumo, type Page } from '../lib/api';
import { useAvisos } from '../lib/contexto';
import { dataHora, diasAtras, hoje, moeda, nomeStatusNota } from '../lib/format';

export default function PaginaFiscal() {
  const [aba, setAba] = useState<'notas' | 'config'>('notas');
  const [config, setConfig] = useState<ConfiguracaoFiscal | null>(null);
  const { erro } = useAvisos();

  useEffect(() => {
    api.configuracaoFiscal().then(setConfig).catch(erro);
  }, [erro]);

  return (
    <div className="pagina">
      <header className="pagina__topo">
        <div>
          <p className="sobretitulo">NFC-e · modelo 65</p>
          <h1>Fiscal</h1>
        </div>
        {config && (
          <div className="pagina__acoes">
            <span className={`selo ${config.ambiente === 'PRODUCAO' ? 'selo--ok' : 'selo--homologacao'}`}>
              {config.ambiente === 'PRODUCAO' ? 'Produção' : 'Homologação'}
            </span>
            <span className="selo">Emissor {config.emissor.toLowerCase()}</span>
            <span className="contador">
              Série {config.serie} · próximo nº {config.proximoNumero}
            </span>
          </div>
        )}
      </header>

      {config && config.pendencias.length > 0 && (
        <div className="faixa faixa--alerta">
          <strong>Emissão bloqueada.</strong> Falta configurar: {config.pendencias.join(', ')}.
        </div>
      )}
      {config?.emissor === 'SIMULADO' && (
        <div className="faixa">
          O emissor <strong>simulado</strong> autoriza as notas localmente, sem enviar à SEFAZ. Serve para testar e treinar. Para
          emitir de verdade, veja <code>docs/NFCE.md</code>.
        </div>
      )}

      <div className="abas" role="tablist">
        <button role="tab" aria-selected={aba === 'notas'} onClick={() => setAba('notas')}>
          Notas emitidas
        </button>
        <button role="tab" aria-selected={aba === 'config'} onClick={() => setAba('config')}>
          Emitente e configuração
        </button>
      </div>

      {aba === 'notas' ? <Notas /> : config && <Configuracao config={config} aoSalvar={setConfig} />}
    </div>
  );
}

function Notas() {
  const { avisar, erro } = useAvisos();
  const [filtro, setFiltro] = useState({ inicio: diasAtras(7), fim: hoje(), status: '' });
  const [pagina, setPagina] = useState(0);
  const [dados, setDados] = useState<Page<NotaResumo> | null>(null);
  const [danfe, setDanfe] = useState<DanfeDados | null>(null);
  const [cancelando, setCancelando] = useState<NotaResumo | null>(null);
  const [justificativa, setJustificativa] = useState('');

  const carregar = useCallback(() => {
    api.notas({ ...filtro, page: pagina }).then(setDados).catch(erro);
  }, [filtro, pagina, erro]);
  useEffect(carregar, [carregar]);

  const cancelar = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!cancelando) return;
    try {
      await api.cancelarNota(cancelando.id, justificativa);
      avisar(`NFC-e nº ${cancelando.numero} cancelada.`);
      setCancelando(null);
      setJustificativa('');
      carregar();
    } catch (err) {
      erro(err);
    }
  };

  return (
    <>
      <div className="filtros">
        <label>
          De
          <input type="date" value={filtro.inicio} onChange={(e) => setFiltro({ ...filtro, inicio: e.target.value })} />
        </label>
        <label>
          Até
          <input type="date" value={filtro.fim} onChange={(e) => setFiltro({ ...filtro, fim: e.target.value })} />
        </label>
        <label>
          Status
          <select value={filtro.status} onChange={(e) => setFiltro({ ...filtro, status: e.target.value })}>
            <option value="">Todos</option>
            {Object.entries(nomeStatusNota).map(([k, v]) => (
              <option key={k} value={k}>
                {v}
              </option>
            ))}
          </select>
        </label>
      </div>

      <table className="tabela">
        <thead>
          <tr>
            <th>Número</th>
            <th>Emissão</th>
            <th>Venda</th>
            <th>Status</th>
            <th>Chave de acesso</th>
            <th className="tabela__num">Valor</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {dados?.content.map((n) => (
            <tr key={n.id}>
              <td>
                {n.numero} <small>série {n.serie}</small>
              </td>
              <td>{dataHora(n.dataEmissao)}</td>
              <td>#{n.vendaId}</td>
              <td>
                <span className={`selo selo--${n.status.toLowerCase()}`} title={n.motivo}>
                  {nomeStatusNota[n.status]}
                </span>
              </td>
              <td className="tabela__chave">{n.chaveAcesso}</td>
              <td className="tabela__num">{moeda(n.valor)}</td>
              <td className="tabela__acoes">
                {(n.status === 'AUTORIZADA' || n.status === 'CANCELADA') && (
                  <button className="link" onClick={() => api.danfe(n.id).then(setDanfe).catch(erro)}>
                    DANFE
                  </button>
                )}
                <button className="link" onClick={() => api.baixarXml(n.id, n.chaveAcesso).catch(erro)}>
                  XML
                </button>
                {n.status === 'AUTORIZADA' && (
                  <button className="link link--perigo" onClick={() => setCancelando(n)}>
                    Cancelar
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {dados && dados.content.length === 0 && (
        <p className="vazio-linha">Nenhuma nota no período. As notas aparecem aqui assim que uma venda é finalizada.</p>
      )}
      {dados && dados.totalPages > 1 && (
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

      <Painel titulo={danfe ? `DANFE · NFC-e nº ${danfe.numero}` : ''} aberto={!!danfe} aoFechar={() => setDanfe(null)} largura={440}>
        {danfe && (
          <>
            <div className="impressao">
              <Danfe d={danfe} />
            </div>
            <button className="botao botao--principal" onClick={() => window.print()}>
              Imprimir
            </button>
          </>
        )}
      </Painel>

      <Painel titulo={cancelando ? `Cancelar NFC-e nº ${cancelando.numero}` : ''} aberto={!!cancelando} aoFechar={() => setCancelando(null)}>
        <form className="formulario" onSubmit={cancelar}>
          <p className="formulario__cheio dica">
            O cancelamento só é aceito dentro do prazo da SEFAZ. Depois de cancelar a nota, a venda pode ser estornada na tela
            Vendas.
          </p>
          <label className="formulario__cheio">
            Justificativa (15 a 255 caracteres)
            <textarea rows={3} minLength={15} maxLength={255} required value={justificativa} onChange={(e) => setJustificativa(e.target.value)} />
            <small>{justificativa.trim().length}/15</small>
          </label>
          <div className="formulario__cheio">
            <button className="botao botao--perigo-cheio" disabled={justificativa.trim().length < 15}>
              Cancelar nota
            </button>
          </div>
        </form>
      </Painel>
    </>
  );
}

function Configuracao({ config, aoSalvar }: { config: ConfiguracaoFiscal; aoSalvar: (c: ConfiguracaoFiscal) => void }) {
  const { avisar, erro } = useAvisos();
  const [f, setF] = useState<Partial<ConfiguracaoFiscal>>({ ...config, csc: '' });
  const texto = (k: keyof ConfiguracaoFiscal) => ({
    value: (f[k] as string | number | undefined) ?? '',
    onChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => setF({ ...f, [k]: e.target.value }),
  });

  const salvar = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const c = await api.salvarConfiguracaoFiscal({
        ...f,
        crt: Number(f.crt),
        serie: Number(f.serie),
        proximoNumero: Number(f.proximoNumero),
        prazoCancelamentoMin: Number(f.prazoCancelamentoMin),
      });
      aoSalvar(c);
      setF({ ...c, csc: '' });
      avisar('Configuração fiscal salva.');
    } catch (err) {
      erro(err);
    }
  };

  return (
    <form className="formulario formulario--largo" onSubmit={salvar}>
      <fieldset className="formulario__cheio">
        <legend>Emitente</legend>
        <label>
          CNPJ
          <input inputMode="numeric" maxLength={14} {...texto('cnpj')} />
        </label>
        <label>
          Inscrição estadual
          <input {...texto('inscricaoEstadual')} />
        </label>
        <label className="formulario__duplo">
          Razão social
          <input {...texto('razaoSocial')} />
        </label>
        <label className="formulario__duplo">
          Nome fantasia
          <input {...texto('nomeFantasia')} />
        </label>
        <label>
          Regime (CRT)
          <select {...texto('crt')}>
            <option value="1">1 · Simples Nacional</option>
            <option value="2">2 · Simples (excesso de sublimite)</option>
            <option value="3">3 · Regime Normal</option>
          </select>
        </label>
        <label>
          Telefone
          <input inputMode="tel" {...texto('telefone')} />
        </label>
      </fieldset>

      <fieldset className="formulario__cheio">
        <legend>Endereço</legend>
        <label className="formulario__duplo">
          Logradouro
          <input {...texto('logradouro')} />
        </label>
        <label>
          Número
          <input {...texto('numero')} />
        </label>
        <label>
          Bairro
          <input {...texto('bairro')} />
        </label>
        <label>
          Município
          <input {...texto('municipio')} />
        </label>
        <label>
          Código IBGE
          <input inputMode="numeric" maxLength={7} {...texto('codigoMunicipio')} />
        </label>
        <label>
          UF
          <input maxLength={2} {...texto('uf')} />
        </label>
        <label>
          CEP
          <input inputMode="numeric" maxLength={8} {...texto('cep')} />
        </label>
      </fieldset>

      <fieldset className="formulario__cheio">
        <legend>Emissão</legend>
        <label>
          Ambiente
          <select {...texto('ambiente')}>
            <option value="HOMOLOGACAO">Homologação (testes)</option>
            <option value="PRODUCAO">Produção</option>
          </select>
        </label>
        <label>
          Série
          <input inputMode="numeric" {...texto('serie')} />
        </label>
        <label>
          Próximo número
          <input inputMode="numeric" {...texto('proximoNumero')} />
        </label>
        <label>
          Prazo de cancelamento (min)
          <input inputMode="numeric" {...texto('prazoCancelamentoMin')} />
        </label>
        <label>
          ID do CSC
          <input inputMode="numeric" {...texto('cscId')} />
        </label>
        <label>
          CSC {config.cscConfigurado && <small>atual {config.cscMascarado}</small>}
          <input type="password" autoComplete="off" placeholder={config.cscConfigurado ? 'manter o atual' : ''} {...texto('csc')} />
        </label>
        <label className="formulario__duplo">
          URL do QR Code (UF)
          <input {...texto('urlQrCode')} />
        </label>
        <label className="formulario__duplo">
          URL de consulta (UF)
          <input {...texto('urlConsulta')} />
        </label>
        <label className="alternador formulario__duplo">
          <input type="checkbox" checked={!!f.emissaoAutomatica} onChange={(e) => setF({ ...f, emissaoAutomatica: e.target.checked })} />
          Emitir NFC-e automaticamente ao finalizar a venda
        </label>
      </fieldset>

      <div className="formulario__cheio">
        <button className="botao botao--principal">Salvar configuração</button>
      </div>
    </form>
  );
}
