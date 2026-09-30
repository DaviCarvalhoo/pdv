import { useEffect, useRef, type ReactNode } from 'react';

/**
 * Gaveta lateral para formulários secundários (editar produto, detalhe de venda...). Fecha com Esc
 * e devolve o foco para onde estava.
 */
export default function Painel({
  titulo,
  aberto,
  aoFechar,
  children,
  largura = 460,
}: {
  titulo: string;
  aberto: boolean;
  aoFechar: () => void;
  children: ReactNode;
  largura?: number;
}) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!aberto) return;
    const anterior = document.activeElement as HTMLElement | null;
    const primeiro = ref.current?.querySelector<HTMLElement>('input, select, textarea, button:not(.painel__fechar)');
    primeiro?.focus();
    const esc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.stopPropagation();
        aoFechar();
      }
    };
    window.addEventListener('keydown', esc, true);
    return () => {
      window.removeEventListener('keydown', esc, true);
      anterior?.focus?.();
    };
  }, [aberto, aoFechar]);

  if (!aberto) return null;
  return (
    <div className="painel-fundo" onMouseDown={(e) => e.target === e.currentTarget && aoFechar()}>
      <div className="painel" ref={ref} role="dialog" aria-modal="true" aria-label={titulo} style={{ width: largura }}>
        <header className="painel__topo">
          <h2>{titulo}</h2>
          <button className="painel__fechar" onClick={aoFechar} aria-label="Fechar">
            Esc
          </button>
        </header>
        <div className="painel__corpo">{children}</div>
      </div>
    </div>
  );
}
