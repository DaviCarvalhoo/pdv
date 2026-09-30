import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Marca from '../components/Marca';
import { api, ApiError, type Operador } from '../lib/api';
import { useSessao } from '../lib/contexto';
import { iniciais, nomePapel } from '../lib/format';

export default function Login() {
  const { entrar: guardar, loja } = useSessao();
  const navegar = useNavigate();
  // Depois de entrar, começa pela tela inicial do perfil (e não pela última tela do operador anterior).
  const entrar: typeof guardar = (s) => {
    guardar(s);
    navegar('/', { replace: true });
  };
  const [estado, setEstado] = useState<{ precisaPrimeiroAcesso: boolean; operadores: Operador[] } | null>(null);
  const [falha, setFalha] = useState('');

  useEffect(() => {
    api
      .estadoAcesso()
      .then(setEstado)
      .catch((e) => setFalha(e instanceof ApiError ? e.message : 'Servidor indisponível.'));
  }, []);

  return (
    <div className="login">
      <aside className="login__marca">
        <Marca grande />
        <div className="login__frase">
          <h1>{loja?.slogan || 'Bom trabalho hoje.'}</h1>
          <p>Entre com o seu PIN para abrir o caixa e começar a vender.</p>
        </div>
        <p className="login__rodape">Balcão PDV</p>
      </aside>
      <main className="login__painel">
        {falha && <p className="faixa faixa--alerta">{falha}</p>}
        {estado?.precisaPrimeiroAcesso && <PrimeiroAcesso aoEntrar={entrar} />}
        {estado && !estado.precisaPrimeiroAcesso && <Operadores operadores={estado.operadores} aoEntrar={entrar} />}
      </main>
    </div>
  );
}

function Operadores({ operadores, aoEntrar }: { operadores: Operador[]; aoEntrar: ReturnType<typeof useSessao>['entrar'] }) {
  const [escolhido, setEscolhido] = useState<Operador | null>(operadores.length === 1 ? operadores[0] : null);
  const [pin, setPin] = useState('');
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);

  const enviar = async (valor = pin) => {
    if (!escolhido || valor.length < 4 || enviando) return;
    setEnviando(true);
    try {
      aoEntrar(await api.entrar(escolhido.id, valor));
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : 'Não foi possível entrar.');
      setPin('');
    } finally {
      setEnviando(false);
    }
  };

  // Digitar o PIN direto no teclado, sem precisar clicar no campo.
  useEffect(() => {
    if (!escolhido) return;
    const ouvir = (e: KeyboardEvent) => {
      if (/^\d$/.test(e.key)) setPin((p) => (p.length < 6 ? p + e.key : p));
      else if (e.key === 'Backspace') setPin((p) => p.slice(0, -1));
      else if (e.key === 'Enter') enviar();
      else if (e.key === 'Escape') {
        setEscolhido(null);
        setPin('');
      } else return;
      setErro('');
      e.preventDefault();
    };
    window.addEventListener('keydown', ouvir);
    return () => window.removeEventListener('keydown', ouvir);
  });

  if (!escolhido) {
    return (
      <section className="operadores">
        <h2>Quem está no caixa?</h2>
        <ul>
          {operadores.map((o) => (
            <li key={o.id}>
              <button className="operador" onClick={() => setEscolhido(o)}>
                <span className="avatar">{iniciais(o.nome)}</span>
                <strong>{o.nome}</strong>
                <small>{nomePapel[o.papel]}</small>
              </button>
            </li>
          ))}
        </ul>
      </section>
    );
  }

  return (
    <section className="pin">
      <button className="pin__voltar link" onClick={() => setEscolhido(null)}>
        ← Trocar operador
      </button>
      <span className="avatar avatar--grande">{iniciais(escolhido.nome)}</span>
      <h2>Olá, {escolhido.nome}</h2>
      <div className={`pin__pontos ${erro ? 'pin__pontos--erro' : ''}`} aria-label={`${pin.length} dígitos digitados`}>
        {Array.from({ length: Math.max(4, pin.length) }).map((_, i) => (
          <span key={i} className={i < pin.length ? 'cheio' : ''} />
        ))}
      </div>
      <p className="pin__erro" role="alert">
        {erro || ' '}
      </p>
      <div className="teclado" aria-label="Teclado numérico">
        {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((n) => (
          <button key={n} onClick={() => setPin((p) => (p.length < 6 ? p + n : p))}>
            {n}
          </button>
        ))}
        <button className="teclado__apagar" onClick={() => setPin((p) => p.slice(0, -1))} aria-label="Apagar">
          ⌫
        </button>
        <button onClick={() => setPin((p) => (p.length < 6 ? p + '0' : p))}>0</button>
        <button className="teclado__ok" onClick={() => enviar()} disabled={pin.length < 4 || enviando} aria-label="Entrar">
          →
        </button>
      </div>
    </section>
  );
}

function PrimeiroAcesso({ aoEntrar }: { aoEntrar: ReturnType<typeof useSessao>['entrar'] }) {
  const [nome, setNome] = useState('');
  const [pin, setPin] = useState('');
  const [confirmacao, setConfirmacao] = useState('');
  const [erro, setErro] = useState('');
  return (
    <form
      className="primeiro-acesso formulario"
      onSubmit={async (e) => {
        e.preventDefault();
        if (pin !== confirmacao) return setErro('Os PINs não conferem.');
        try {
          aoEntrar(await api.primeiroAcesso(nome, pin));
        } catch (err) {
          setErro(err instanceof ApiError ? err.message : 'Não foi possível criar o acesso.');
        }
      }}
    >
      <div className="formulario__cheio">
        <p className="sobretitulo">Primeiro acesso</p>
        <h2>Crie o administrador</h2>
        <p className="dica">Ele cadastra a loja, os produtos e os outros operadores. O PIN tem de 4 a 6 números.</p>
      </div>
      <label className="formulario__cheio">
        Seu nome
        <input autoFocus required maxLength={60} value={nome} onChange={(e) => setNome(e.target.value)} />
      </label>
      <label>
        PIN
        <input type="password" inputMode="numeric" required minLength={4} maxLength={6} value={pin} onChange={(e) => setPin(e.target.value.replace(/\D/g, ''))} />
      </label>
      <label>
        Repita o PIN
        <input type="password" inputMode="numeric" required minLength={4} maxLength={6} value={confirmacao} onChange={(e) => setConfirmacao(e.target.value.replace(/\D/g, ''))} />
      </label>
      {erro && <p className="formulario__cheio negativo">{erro}</p>}
      <div className="formulario__cheio">
        <button className="botao botao--principal botao--grande">Criar e entrar</button>
      </div>
    </form>
  );
}
