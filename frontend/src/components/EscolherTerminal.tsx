/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

import { useEffect, useState } from 'react';
import { api, type Terminal } from '../lib/api';
import { useAvisos, useSessao } from '../lib/contexto';
import { hora, moeda } from '../lib/format';

/**
 * "Qual caixa é este computador?" Aparece uma vez por computador/navegador. A escolha fica gravada
 * no próprio aparelho: cada PC, notebook ou tablet da loja vira um ponto de venda com a sua gaveta.
 */
export default function EscolherTerminal({ aoCancelar }: { aoCancelar?: () => void }) {
  const { definirTerminal, pode, sair, operador } = useSessao();
  const { erro } = useAvisos();
  const [lista, setLista] = useState<Terminal[] | null>(null);
  const [novo, setNovo] = useState('');

  const carregar = () => api.terminais().then((t) => setLista(t.filter((x) => x.ativo))).catch(erro);
  useEffect(() => {
    carregar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="escolher-terminal">
      <div className="escolher-terminal__caixa">
        <p className="sobretitulo">Configuração deste computador</p>
        <h1>Qual caixa é este computador?</h1>
        <p className="dica">
          Cada computador, notebook ou tablet que vende é um caixa, com a sua gaveta. A escolha fica gravada neste aparelho.
          Em loja de um caixa só, é só escolher o Caixa 01.
        </p>

        <ul className="terminais">
          {lista?.map((t) => (
            <li key={t.id}>
              <button className="terminal-opcao" onClick={() => definirTerminal({ id: t.id, nome: t.nome })}>
                <span className="terminal-opcao__icone" aria-hidden>
                  <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="1.8">
                    <rect x="3" y="4" width="18" height="12" rx="2" />
                    <path d="M8 20h8M12 16v4" />
                  </svg>
                </span>
                <strong>{t.nome}</strong>
                <small>
                  {t.caixaId ? `aberto desde ${hora(t.abertoEm)} · gaveta ${moeda(t.gaveta)}` : 'fechado'}
                </small>
              </button>
            </li>
          ))}
        </ul>

        {pode('ADMIN') && (
          <form
            className="terminais__novo"
            onSubmit={async (e) => {
              e.preventDefault();
              try {
                const t = await api.criarTerminal(novo);
                setNovo('');
                definirTerminal({ id: t.id, nome: t.nome });
              } catch (err) {
                erro(err);
              }
            }}
          >
            <input placeholder="Novo caixa (ex.: Caixa 02, Balcão da rua)" maxLength={40} required value={novo} onChange={(e) => setNovo(e.target.value)} />
            <button className="botao botao--secundario">Criar e usar</button>
          </form>
        )}

        <div className="acoes__linha escolher-terminal__rodape">
          {pode('GERENTE') && (
            <button className="link" onClick={() => definirTerminal({ retaguarda: true })}>
              Este computador é só da gerência (não vende)
            </button>
          )}
          {aoCancelar ? (
            <button className="link" onClick={aoCancelar}>
              Voltar
            </button>
          ) : (
            <button className="link" onClick={sair}>
              Sair ({operador?.nome})
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
