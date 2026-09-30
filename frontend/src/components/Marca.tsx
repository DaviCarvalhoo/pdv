/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

import { useSessao } from '../lib/contexto';
import { iniciais } from '../lib/format';

/** Logo + nome da loja. Sem logo cadastrado, mostra as iniciais sobre a cor da loja. */
export default function Marca({ grande = false, compacta = false }: { grande?: boolean; compacta?: boolean }) {
  const { loja } = useSessao();
  const nome = loja?.nomeFantasia ?? 'Balcão PDV';
  return (
    <div className={`marca ${grande ? 'marca--grande' : ''}`}>
      {loja?.logo ? (
        <img className="marca__logo" src={loja.logo} alt="" />
      ) : (
        <span className="marca__selo" aria-hidden>
          {iniciais(nome)}
        </span>
      )}
      {!compacta && (
        <span className="marca__nome">
          {nome}
          {!grande && <small>PDV</small>}
        </span>
      )}
    </div>
  );
}
