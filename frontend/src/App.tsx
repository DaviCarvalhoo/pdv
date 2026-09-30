import { NavLink, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { useAtalhos, useCaixa } from './lib/contexto';
import { moeda } from './lib/format';
import PaginaPdv from './pages/Pdv';
import PaginaCaixa from './pages/Caixa';
import PaginaProdutos from './pages/Produtos';
import PaginaVendas from './pages/Vendas';
import PaginaFiscal from './pages/Fiscal';

const MENU = [
  { to: '/pdv', rotulo: 'Vender', tecla: 'F1' },
  { to: '/caixa', rotulo: 'Caixa', tecla: 'F6' },
  { to: '/produtos', rotulo: 'Produtos', tecla: 'F7' },
  { to: '/vendas', rotulo: 'Vendas', tecla: 'F9' },
  { to: '/fiscal', rotulo: 'Fiscal', tecla: 'F11' },
];

export default function App() {
  const navegar = useNavigate();
  const { caixa, carregando } = useCaixa();

  useAtalhos({
    F1: () => navegar('/pdv'),
    F6: () => navegar('/caixa'),
    F7: () => navegar('/produtos'),
    F9: () => navegar('/vendas'),
    F11: () => navegar('/fiscal'),
  });

  return (
    <div className="app">
      <aside className="lateral">
        <div className="marca">
          <span className="marca__selo" aria-hidden>
            B
          </span>
          <span className="marca__nome">
            Balcão<small>PDV</small>
          </span>
        </div>
        <nav className="menu">
          {MENU.map((m) => (
            <NavLink key={m.to} to={m.to} className="menu__item">
              <span>{m.rotulo}</span>
              <kbd>{m.tecla}</kbd>
            </NavLink>
          ))}
        </nav>
        <div className={`status-caixa ${caixa ? 'status-caixa--aberto' : ''}`}>
          {carregando ? (
            <span>…</span>
          ) : caixa ? (
            <>
              <span className="status-caixa__luz" aria-hidden />
              <div>
                <strong>Caixa #{caixa.id} aberto</strong>
                <span>Gaveta: {moeda(caixa.saldoEsperado)}</span>
              </div>
            </>
          ) : (
            <>
              <span className="status-caixa__luz" aria-hidden />
              <div>
                <strong>Caixa fechado</strong>
                <NavLink to="/caixa">Abrir caixa →</NavLink>
              </div>
            </>
          )}
        </div>
      </aside>
      <main className="conteudo">
        <Routes>
          <Route path="/" element={<Navigate to="/pdv" replace />} />
          <Route path="/pdv" element={<PaginaPdv />} />
          <Route path="/caixa" element={<PaginaCaixa />} />
          <Route path="/produtos" element={<PaginaProdutos />} />
          <Route path="/vendas" element={<PaginaVendas />} />
          <Route path="/fiscal" element={<PaginaFiscal />} />
        </Routes>
      </main>
    </div>
  );
}
