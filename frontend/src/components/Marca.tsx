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
