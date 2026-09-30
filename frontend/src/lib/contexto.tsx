/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import {
  api,
  ApiError,
  aoConexao,
  aoSessaoExpirar,
  guardarSessao,
  guardarTerminal,
  sessaoSalva,
  terminalSalvo,
  type Caixa,
  type EsteComputador,
  type LojaPublica,
  type Operador,
  type Papel,
  type Sessao,
} from './api';
import { podePapel } from './format';

// ---------------------------------------------------------------- Avisos (toasts)

type TipoAviso = 'ok' | 'erro' | 'info';
interface Aviso {
  id: number;
  tipo: TipoAviso;
  texto: string;
}

interface AvisosCtx {
  avisar: (texto: string, tipo?: TipoAviso) => void;
  erro: (e: unknown) => void;
}

const AvisosContext = createContext<AvisosCtx>(null!);

export function AvisosProvider({ children }: { children: ReactNode }) {
  const [avisos, setAvisos] = useState<Aviso[]>([]);
  const seq = useRef(0);

  const avisar = useCallback((texto: string, tipo: TipoAviso = 'ok') => {
    const id = ++seq.current;
    setAvisos((a) => [...a.slice(-3), { id, tipo, texto }]);
    setTimeout(() => setAvisos((a) => a.filter((x) => x.id !== id)), tipo === 'erro' ? 6000 : 3500);
  }, []);

  const erro = useCallback(
    (e: unknown) => {
      if (e instanceof ApiError && e.codigo === 'AUTORIZACAO_CANCELADA') return avisar(e.message, 'info');
      avisar(e instanceof ApiError ? e.message : 'Algo deu errado. Tente de novo.', 'erro');
    },
    [avisar],
  );

  return (
    <AvisosContext.Provider value={{ avisar, erro }}>
      {children}
      <div className="avisos" role="status" aria-live="polite">
        {avisos.map((a) => (
          <div key={a.id} className={`aviso aviso--${a.tipo}`}>
            {a.texto}
          </div>
        ))}
      </div>
    </AvisosContext.Provider>
  );
}

export const useAvisos = () => useContext(AvisosContext);

// ---------------------------------------------------------------- Sessão e identidade da loja

interface SessaoCtx {
  sessao: Sessao | null;
  operador: Operador | null;
  pode: (minimo: Papel) => boolean;
  entrar: (s: Sessao) => void;
  sair: () => void;
  loja: LojaPublica | null;
  recarregarLoja: () => Promise<void>;
  /** Por que a sessão terminou sem o operador clicar em sair (para explicar na tela de login). */
  motivoSaida: string | null;
  /** Qual caixa é este computador (ou retaguarda). Nulo = ainda não configurado. */
  terminal: EsteComputador | null;
  definirTerminal: (t: EsteComputador | null) => void;
  online: boolean;
  /** Aumenta a cada vez que a conexão volta: as telas recarregam o que estão mostrando. */
  reconexoes: number;
}

const SessaoContext = createContext<SessaoCtx>(null!);

/** Aplica a cor da loja em todo o sistema (botões, etiqueta de preço, destaques). */
function aplicarCor(cor?: string) {
  if (!cor || !/^#[0-9a-f]{6}$/i.test(cor)) return;
  const raiz = document.documentElement.style;
  raiz.setProperty('--marca', cor);
  // Texto sobre a cor da loja: escuro em cores claras (amarelo, verde-limão), claro nas demais.
  const [r, g, b] = [1, 3, 5].map((i) => parseInt(cor.slice(i, i + 2), 16) / 255).map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4));
  const luminancia = 0.2126 * r + 0.7152 * g + 0.0722 * b;
  raiz.setProperty('--marca-texto', luminancia > 0.4 ? 'oklch(22% 0.01 80)' : 'oklch(99% 0.004 85)');
}

export function SessaoProvider({ children }: { children: ReactNode }) {
  const [sessao, setSessao] = useState<Sessao | null>(sessaoSalva());
  const [loja, setLoja] = useState<LojaPublica | null>(null);
  const [motivoSaida, setMotivoSaida] = useState<string | null>(null);
  const [terminal, setTerminal] = useState<EsteComputador | null>(terminalSalvo());
  const [online, setOnline] = useState(true);
  const [reconexoes, setReconexoes] = useState(0);
  const onlineRef = useRef(true);

  const recarregarLoja = useCallback(async () => {
    try {
      const l = await api.lojaPublica();
      setLoja(l);
      aplicarCor(l.corDestaque);
      document.title = `${l.nomeFantasia} · PDV`;
      const icone = document.querySelector<HTMLLinkElement>('link[rel="icon"]');
      if (icone && l.logo) icone.href = l.logo;
    } catch {
      /* backend fora do ar: a tela de login mostra o erro */
    }
  }, []);

  useEffect(() => {
    recarregarLoja();
    aoSessaoExpirar(() => {
      setMotivoSaida('Sua sessão terminou (muito tempo sem uso ou acesso encerrado pelo administrador). Entre de novo: a venda em andamento continua salva.');
      setSessao(null);
    });
    aoConexao((agora) => {
      if (agora && !onlineRef.current) setReconexoes((n) => n + 1);
      onlineRef.current = agora;
      setOnline(agora);
    });
  }, [recarregarLoja]);

  // Sem conexão: tenta o servidor a cada 3 s até voltar.
  useEffect(() => {
    if (online) return;
    const t = setInterval(() => {
      api.lojaPublica().catch(() => undefined);
    }, 3000);
    return () => clearInterval(t);
  }, [online]);

  const definirTerminal = useCallback((t: EsteComputador | null) => {
    guardarTerminal(t);
    setTerminal(t);
  }, []);

  const entrar = useCallback((s: Sessao) => {
    guardarSessao(s);
    setMotivoSaida(null);
    setSessao(s);
  }, []);

  const sair = useCallback(() => {
    api.sair().catch(() => undefined);
    guardarSessao(null);
    setSessao(null);
  }, []);

  const operador = sessao?.operador ?? null;
  const pode = useCallback((minimo: Papel) => podePapel(operador?.papel, minimo), [operador]);

  return (
    <SessaoContext.Provider
      value={{ sessao, operador, pode, entrar, sair, loja, recarregarLoja, motivoSaida, terminal, definirTerminal, online, reconexoes }}
    >
      {children}
    </SessaoContext.Provider>
  );
}

export const useSessao = () => useContext(SessaoContext);

// ---------------------------------------------------------------- Caixa aberto

interface CaixaCtx {
  caixa: Caixa | null;
  carregando: boolean;
  recarregar: () => Promise<void>;
}

const CaixaContext = createContext<CaixaCtx>(null!);

export function CaixaProvider({ children }: { children: ReactNode }) {
  const [caixa, setCaixa] = useState<Caixa | null>(null);
  const [carregando, setCarregando] = useState(true);
  const { terminal, reconexoes } = useSessao();
  const retaguarda = !!terminal && 'retaguarda' in terminal;

  const recarregar = useCallback(async () => {
    if (retaguarda) {
      setCaixa(null);
      setCarregando(false);
      return;
    }
    try {
      setCaixa((await api.caixaAberto()) ?? null);
    } catch {
      setCaixa(null);
    } finally {
      setCarregando(false);
    }
  }, [retaguarda]);

  useEffect(() => {
    recarregar();
  }, [recarregar, reconexoes]);

  return <CaixaContext.Provider value={{ caixa, carregando, recarregar }}>{children}</CaixaContext.Provider>;
}

export const useCaixa = () => useContext(CaixaContext);

// ---------------------------------------------------------------- Atalhos

/**
 * Registra atalhos globais (F2, F4, Alt+1...). A chave é a tecla, com prefixo "Alt+" quando for o caso.
 * Não rouba teclas de quem está digitando num campo com conteúdo.
 */
export function useAtalhos(mapa: Record<string, (e: KeyboardEvent) => void>, ativo = true) {
  const ref = useRef(mapa);
  ref.current = mapa;
  useEffect(() => {
    if (!ativo) return;
    const ouvir = (e: KeyboardEvent) => {
      const chave = (e.altKey ? 'Alt+' : '') + (e.altKey && e.code.startsWith('Digit') ? e.code.slice(5) : e.key);
      const fn = ref.current[chave];
      const alvo = e.target as HTMLInputElement;
      const editavel = alvo?.tagName === 'INPUT' || alvo?.tagName === 'TEXTAREA' || alvo?.tagName === 'SELECT';
      if (editavel && !e.altKey && (e.key.length === 1 || e.key === 'Delete') && alvo.value !== '') return;
      if (fn) {
        e.preventDefault();
        fn(e);
      }
    };
    window.addEventListener('keydown', ouvir);
    return () => window.removeEventListener('keydown', ouvir);
  }, [ativo]);
}
