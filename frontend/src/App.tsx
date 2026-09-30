/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

import { NavLink, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import AutorizacaoGerente from './components/AutorizacaoGerente';
import Marca from './components/Marca';
import type { Papel } from './lib/api';
import { CaixaProvider, useAtalhos, useCaixa, useSessao } from './lib/contexto';
import { iniciais, moeda, nomePapel } from './lib/format';
import PaginaCaixa from './pages/Caixa';
import PaginaClientes from './pages/Clientes';
import PaginaEntradaNfe from './pages/EntradaNfe';
import PaginaFiscal from './pages/Fiscal';
import PaginaLoja from './pages/Loja';
import Login from './pages/Login';
import PaginaPainel from './pages/Painel';
import PaginaPdv from './pages/Pdv';
import PaginaProdutos from './pages/Produtos';
import PaginaRelatorios from './pages/Relatorios';
import PaginaUsuarios from './pages/Usuarios';
import PaginaVendas from './pages/Vendas';

interface ItemMenu {
  to: string;
  rotulo: string;
  papel: Papel;
  icone: string;
}

const GRUPOS: { titulo: string; itens: ItemMenu[] }[] = [
  {
    titulo: 'Operação',
    itens: [
      { to: '/pdv', rotulo: 'Vender', papel: 'OPERADOR', icone: 'M3 5h2l2.4 10.2a2 2 0 0 0 2 1.6h7.7a2 2 0 0 0 2-1.5L21 8H6.2M9 21h.01M18 21h.01' },
      { to: '/caixa', rotulo: 'Caixa', papel: 'OPERADOR', icone: 'M3 7h18v12H3zM3 11h18M7 15h3' },
      { to: '/clientes', rotulo: 'Clientes e fiado', papel: 'OPERADOR', icone: 'M16 19v-1a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v1M9.5 10a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7M21 19v-1a4 4 0 0 0-3-3.9M16 3.1a3.5 3.5 0 0 1 0 6.8' },
    ],
  },
  {
    titulo: 'Gestão',
    itens: [
      { to: '/painel', rotulo: 'Painel', papel: 'GERENTE', icone: 'M4 20V10M10 20V4M16 20v-7M22 20H2' },
      { to: '/vendas', rotulo: 'Vendas', papel: 'OPERADOR', icone: 'M7 3h10l4 4v14H3V3h4M7 3v5h8V3M7 13h10M7 17h6' },
      { to: '/produtos', rotulo: 'Produtos', papel: 'OPERADOR', icone: 'M21 8 12 3 3 8v8l9 5 9-5zM3 8l9 5 9-5M12 13v8' },
      { to: '/entrada-nfe', rotulo: 'Entrada de nota', papel: 'GERENTE', icone: 'M12 3v12M7 10l5 5 5-5M4 21h16' },
      { to: '/relatorios', rotulo: 'Relatórios', papel: 'GERENTE', icone: 'M4 19h16M6 16l4-5 3 3 5-7' },
    ],
  },
  {
    titulo: 'Administração',
    itens: [
      { to: '/loja', rotulo: 'Loja', papel: 'ADMIN', icone: 'M4 10v10h16V10M2 10l2-6h16l2 6M2 10h20M9 20v-5h6v5' },
      { to: '/usuarios', rotulo: 'Operadores', papel: 'ADMIN', icone: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8M4 21v-1a6 6 0 0 1 6-6h4a6 6 0 0 1 6 6v1' },
      { to: '/fiscal', rotulo: 'Fiscal (NFC-e)', papel: 'OPERADOR', icone: 'M6 2h9l5 5v15H6zM14 2v6h6M9 13h7M9 17h7' },
    ],
  },
];

export default function App() {
  const { sessao } = useSessao();
  if (!sessao) return <Login />;
  return (
    <CaixaProvider key={sessao.token}>
      <Estrutura />
      <AutorizacaoGerente />
    </CaixaProvider>
  );
}

function Estrutura() {
  const navegar = useNavigate();
  const { operador, pode, sair } = useSessao();
  const { caixa, carregando } = useCaixa();

  const visiveis = GRUPOS.map((g) => ({ ...g, itens: g.itens.filter((i) => pode(i.papel)) })).filter((g) => g.itens.length);
  const planos = visiveis.flatMap((g) => g.itens);

  // Alt+1…9 navega pelas telas na ordem do menu.
  useAtalhos(Object.fromEntries(planos.slice(0, 9).map((item, i) => [`Alt+${i + 1}`, () => navegar(item.to)])));

  return (
    <div className="app">
      <aside className="lateral">
        <Marca />
        <nav className="menu" aria-label="Menu principal">
          {visiveis.map((g) => (
            <div key={g.titulo} className="menu__grupo">
              <p className="menu__titulo">{g.titulo}</p>
              {g.itens.map((m) => {
                const n = planos.indexOf(m) + 1;
                return (
                  <NavLink key={m.to} to={m.to} className="menu__item">
                    <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                      <path d={m.icone} />
                    </svg>
                    <span>{m.rotulo}</span>
                    {n <= 9 && <kbd>Alt {n}</kbd>}
                  </NavLink>
                );
              })}
            </div>
          ))}
        </nav>

        <div className="lateral__rodape">
          <NavLink to="/caixa" className={`status-caixa ${caixa ? 'status-caixa--aberto' : ''} ${caixa?.alertaSangriaLimite ? 'status-caixa--alerta' : ''}`}>
            <span className="status-caixa__luz" aria-hidden />
            {carregando ? (
              <span>…</span>
            ) : caixa ? (
              <span>
                <strong>Caixa #{caixa.id}</strong>
                <small>{caixa.alertaSangriaLimite ? 'Gaveta cheia: faça sangria' : `Gaveta ${moeda(caixa.saldoEsperado)}`}</small>
              </span>
            ) : (
              <span>
                <strong>Caixa fechado</strong>
                <small>Abrir caixa →</small>
              </span>
            )}
          </NavLink>
          {operador && (
            <div className="usuario">
              <span className="avatar">{iniciais(operador.nome)}</span>
              <span className="usuario__nome">
                <strong>{operador.nome}</strong>
                <small>{nomePapel[operador.papel]}</small>
              </span>
              <button className="usuario__sair" onClick={sair} title="Sair (trocar operador)" aria-label="Sair">
                <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
                  <path d="M15 4h4v16h-4M10 8l-4 4 4 4M6 12h10" />
                </svg>
              </button>
            </div>
          )}
          <p className="assinatura">Balcão PDV · DaviCarvalhoo</p>
        </div>
      </aside>

      <main className="conteudo">
        <Routes>
          <Route path="/" element={<Navigate to={pode('GERENTE') ? '/painel' : '/pdv'} replace />} />
          <Route path="/pdv" element={<PaginaPdv />} />
          <Route path="/caixa" element={<PaginaCaixa />} />
          <Route path="/clientes" element={<PaginaClientes />} />
          <Route path="/vendas" element={<PaginaVendas />} />
          <Route path="/produtos" element={<PaginaProdutos />} />
          <Route path="/fiscal" element={<PaginaFiscal />} />
          {pode('GERENTE') && <Route path="/painel" element={<PaginaPainel />} />}
          {pode('GERENTE') && <Route path="/entrada-nfe" element={<PaginaEntradaNfe />} />}
          {pode('GERENTE') && <Route path="/relatorios" element={<PaginaRelatorios />} />}
          {pode('ADMIN') && <Route path="/loja" element={<PaginaLoja />} />}
          {pode('ADMIN') && <Route path="/usuarios" element={<PaginaUsuarios />} />}
          <Route path="*" element={<Navigate to="/pdv" replace />} />
        </Routes>
      </main>
    </div>
  );
}
