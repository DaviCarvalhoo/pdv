import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import { api, ApiError, type Caixa } from './api';

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
    (e: unknown) => avisar(e instanceof ApiError ? e.message : 'Algo deu errado. Tente de novo.', 'erro'),
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

  const recarregar = useCallback(async () => {
    try {
      setCaixa((await api.caixaAberto()) ?? null);
    } catch {
      setCaixa(null);
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    recarregar();
  }, [recarregar]);

  return <CaixaContext.Provider value={{ caixa, carregando, recarregar }}>{children}</CaixaContext.Provider>;
}

export const useCaixa = () => useContext(CaixaContext);

// ---------------------------------------------------------------- Atalhos

/** Registra atalhos globais (F2, F4...). Os handlers recebem o evento e podem cancelar o padrão do navegador. */
export function useAtalhos(mapa: Record<string, (e: KeyboardEvent) => void>, ativo = true) {
  const ref = useRef(mapa);
  ref.current = mapa;
  useEffect(() => {
    if (!ativo) return;
    const ouvir = (e: KeyboardEvent) => {
      const fn = ref.current[e.key];
      // Não rouba teclas de quem está digitando num campo com conteúdo.
      const alvo = e.target as HTMLInputElement;
      const editavel = alvo?.tagName === 'INPUT' || alvo?.tagName === 'TEXTAREA' || alvo?.tagName === 'SELECT';
      if (editavel && (e.key.length === 1 || e.key === 'Delete') && alvo.value !== '') return;
      if (fn) {
        e.preventDefault();
        fn(e);
      }
    };
    window.addEventListener('keydown', ouvir);
    return () => window.removeEventListener('keydown', ouvir);
  }, [ativo]);
}
