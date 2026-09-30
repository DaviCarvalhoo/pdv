/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { api, ApiError } from '../lib/api';
import { useSessao } from '../lib/contexto';
import { iniciais } from '../lib/format';

const EVENTOS = ['mousemove', 'mousedown', 'keydown', 'touchstart', 'wheel'] as const;

/**
 * Trava a tela depois de alguns minutos sem uso (operador foi ao estoque, atender na porta...).
 * Nada se perde: a venda continua no servidor. Para voltar, o mesmo operador digita o PIN; ou troca de operador.
 */
export default function BloqueioTela({ minutos }: { minutos: number }) {
  const { operador, sair } = useSessao();
  const [bloqueado, setBloqueado] = useState(false);
  const [pin, setPin] = useState('');
  const [erro, setErro] = useState('');
  const ultimoUso = useRef(Date.now());

  // Conta o tempo sem uso.
  useEffect(() => {
    if (!minutos) return;
    const marcar = () => {
      if (!bloqueado) ultimoUso.current = Date.now();
    };
    EVENTOS.forEach((e) => window.addEventListener(e, marcar, { passive: true }));
    const t = setInterval(() => {
      if (!bloqueado && Date.now() - ultimoUso.current > minutos * 60_000) setBloqueado(true);
    }, 5000);
    return () => {
      EVENTOS.forEach((e) => window.removeEventListener(e, marcar));
      clearInterval(t);
    };
  }, [minutos, bloqueado]);

  const desbloquear = useCallback(
    async (valor: string) => {
      try {
        await api.desbloquear(valor);
        setBloqueado(false);
        setPin('');
        setErro('');
        ultimoUso.current = Date.now();
      } catch (e) {
        setErro(e instanceof ApiError ? e.message : 'Não foi possível desbloquear.');
        setPin('');
      }
    },
    [],
  );

  // Com a tela travada, o teclado só serve para o PIN: nenhum atalho do PDV passa.
  useEffect(() => {
    if (!bloqueado) return;
    const ouvir = (e: KeyboardEvent) => {
      e.stopImmediatePropagation();
      if (/^\d$/.test(e.key)) setPin((p) => (p.length < 6 ? p + e.key : p));
      else if (e.key === 'Backspace') setPin((p) => p.slice(0, -1));
      else if (e.key === 'Enter') setPin((p) => {
        if (p.length >= 4) desbloquear(p);
        return p;
      });
      e.preventDefault();
    };
    window.addEventListener('keydown', ouvir, true);
    return () => window.removeEventListener('keydown', ouvir, true);
  }, [bloqueado, desbloquear]);

  if (!bloqueado || !operador) return null;

  return (
    <div className="bloqueio" role="dialog" aria-modal="true" aria-label="Tela bloqueada">
      <div className="bloqueio__caixa">
        <span className="avatar avatar--grande">{iniciais(operador.nome)}</span>
        <h2>Tela bloqueada</h2>
        <p>
          {operador.nome}, digite o seu PIN para continuar. A venda em andamento está salva.
        </p>
        <div className={`pin__pontos ${erro ? 'pin__pontos--erro' : ''}`}>
          {Array.from({ length: Math.max(4, pin.length) }).map((_, i) => (
            <span key={i} className={i < pin.length ? 'cheio' : ''} />
          ))}
        </div>
        <p className="pin__erro">{erro || ' '}</p>
        <div className="teclado">
          {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((n) => (
            <button key={n} onClick={() => setPin((p) => (p.length < 6 ? p + n : p))}>
              {n}
            </button>
          ))}
          <button className="teclado__apagar" onClick={() => setPin((p) => p.slice(0, -1))} aria-label="Apagar">
            ⌫
          </button>
          <button onClick={() => setPin((p) => (p.length < 6 ? p + '0' : p))}>0</button>
          <button className="teclado__ok" disabled={pin.length < 4} onClick={() => desbloquear(pin)} aria-label="Desbloquear">
            →
          </button>
        </div>
        <button className="link" onClick={sair}>
          Trocar de operador
        </button>
      </div>
    </div>
  );
}
