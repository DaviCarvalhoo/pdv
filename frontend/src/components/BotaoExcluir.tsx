/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

import { useEffect, useState } from 'react';

/**
 * Excluir em dois cliques: o primeiro pede confirmação no próprio botão (sem janela do navegador),
 * o segundo exclui. A confirmação some sozinha depois de alguns segundos.
 */
export default function BotaoExcluir({ oque, aoExcluir }: { oque: string; aoExcluir: () => Promise<void> }) {
  const [confirmando, setConfirmando] = useState(false);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    if (!confirmando) return;
    const t = setTimeout(() => setConfirmando(false), 5000);
    return () => clearTimeout(t);
  }, [confirmando]);

  return (
    <button
      type="button"
      className={`botao ${confirmando ? 'botao--perigo-cheio' : 'botao--fantasma botao--perigo'}`}
      disabled={enviando}
      onClick={async () => {
        if (!confirmando) return setConfirmando(true);
        setEnviando(true);
        try {
          await aoExcluir();
        } finally {
          setEnviando(false);
          setConfirmando(false);
        }
      }}
    >
      {confirmando ? `Confirmar exclusão ${oque}` : 'Excluir'}
    </button>
  );
}
