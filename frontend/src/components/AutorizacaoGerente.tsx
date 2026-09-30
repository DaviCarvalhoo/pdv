import { useEffect, useRef, useState } from 'react';
import { api, ApiError, definirPedidoDeAutorizacao } from '../lib/api';

interface Pedido {
  motivo: string;
  resolver: (token: string | null) => void;
}

/**
 * "Senha do supervisor": quando o backend exige um gerente, a API chama este componente, que pede o PIN,
 * troca por uma autorização de uso único e a devolve para a requisição ser repetida.
 */
export default function AutorizacaoGerente() {
  const [pedido, setPedido] = useState<Pedido | null>(null);
  const [pin, setPin] = useState('');
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);
  const campo = useRef<HTMLInputElement>(null);

  useEffect(() => {
    definirPedidoDeAutorizacao(
      (motivo) =>
        new Promise((resolver) => {
          setPin('');
          setErro('');
          setPedido({ motivo, resolver });
        }),
    );
    return () => definirPedidoDeAutorizacao(null);
  }, []);

  useEffect(() => {
    if (pedido) setTimeout(() => campo.current?.focus(), 0);
  }, [pedido]);

  if (!pedido) return null;

  const fechar = (token: string | null) => {
    pedido.resolver(token);
    setPedido(null);
  };

  const confirmar = async (e: React.FormEvent) => {
    e.preventDefault();
    setEnviando(true);
    try {
      const r = await api.autorizar(pin);
      fechar(r.autorizacao);
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Não foi possível autorizar.');
      setPin('');
      campo.current?.focus();
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="autorizacao-fundo" onKeyDown={(e) => e.key === 'Escape' && fechar(null)}>
      <form className="autorizacao" onSubmit={confirmar} role="dialog" aria-modal="true" aria-labelledby="aut-titulo">
        <span className="autorizacao__cadeado" aria-hidden>
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2">
            <rect x="4" y="10" width="16" height="11" rx="2.5" />
            <path d="M8 10V7a4 4 0 0 1 8 0v3" />
          </svg>
        </span>
        <h2 id="aut-titulo">Autorização do gerente</h2>
        <p>{pedido.motivo}</p>
        <input
          ref={campo}
          type="password"
          inputMode="numeric"
          autoComplete="off"
          maxLength={6}
          placeholder="PIN do gerente"
          value={pin}
          onChange={(e) => setPin(e.target.value.replace(/\D/g, ''))}
          aria-invalid={!!erro}
        />
        {erro && <p className="autorizacao__erro">{erro}</p>}
        <div className="acoes__linha">
          <button className="botao botao--principal" disabled={pin.length < 4 || enviando}>
            Autorizar <kbd>Enter</kbd>
          </button>
          <button type="button" className="botao botao--fantasma" onClick={() => fechar(null)}>
            Cancelar <kbd>Esc</kbd>
          </button>
        </div>
      </form>
    </div>
  );
}
