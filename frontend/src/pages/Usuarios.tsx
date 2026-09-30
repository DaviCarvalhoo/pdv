/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

import { useCallback, useEffect, useState } from 'react';
import BotaoExcluir from '../components/BotaoExcluir';
import Painel from '../components/Painel';
import { api, type Papel, type Usuario } from '../lib/api';
import { useAvisos, useSessao } from '../lib/contexto';
import { dataHora, iniciais, nomePapel } from '../lib/format';

const DESCRICAO: Record<Papel, string> = {
  OPERADOR: 'Vende, abre e fecha o caixa. Cancelar, estornar, sangria e desconto alto pedem o PIN do gerente.',
  GERENTE: 'Tudo do operador + autoriza ações, vê o painel, cadastra produtos e dá entrada em notas.',
  ADMIN: 'Tudo + configura a loja, a parte fiscal e os operadores.',
};

export default function PaginaUsuarios() {
  const { erro } = useAvisos();
  const { operador } = useSessao();
  const [lista, setLista] = useState<Usuario[]>([]);
  const [aberto, setAberto] = useState<Usuario | 'novo' | null>(null);

  const carregar = useCallback(() => {
    api.usuarios().then(setLista).catch(erro);
  }, [erro]);
  useEffect(carregar, [carregar]);

  return (
    <div className="pagina">
      <header className="pagina__topo">
        <div>
          <p className="sobretitulo">Administração</p>
          <h1>Operadores</h1>
        </div>
        <button className="botao botao--principal" onClick={() => setAberto('novo')}>
          Novo operador
        </button>
      </header>

      <ul className="cartoes-usuarios">
        {lista.map((u) => (
          <li key={u.id}>
            <button className={`cartao-usuario ${u.ativo ? '' : 'cartao-usuario--inativo'}`} onClick={() => setAberto(u)}>
              <span className="avatar">{iniciais(u.nome)}</span>
              <span>
                <strong>
                  {u.nome} {u.id === operador?.id && <small>(você)</small>}
                </strong>
                <small>{nomePapel[u.papel]}{!u.ativo && ' · inativo'}</small>
                <small>Último acesso: {dataHora(u.ultimoAcesso)}</small>
              </span>
            </button>
          </li>
        ))}
      </ul>

      <section className="papeis">
        {(Object.keys(DESCRICAO) as Papel[]).map((p) => (
          <div key={p}>
            <strong>{nomePapel[p]}</strong>
            <p>{DESCRICAO[p]}</p>
          </div>
        ))}
      </section>

      <Painel titulo={aberto === 'novo' ? 'Novo operador' : aberto ? aberto.nome : ''} aberto={!!aberto} aoFechar={() => setAberto(null)}>
        {aberto && (
          <FormUsuario
            key={aberto === 'novo' ? 'novo' : aberto.id}
            usuario={aberto === 'novo' ? null : aberto}
            souEu={aberto !== 'novo' && aberto.id === operador?.id}
            aoSalvar={() => {
              carregar();
              setAberto(null);
            }}
          />
        )}
      </Painel>
    </div>
  );
}

function FormUsuario({ usuario, souEu, aoSalvar }: { usuario: Usuario | null; souEu?: boolean; aoSalvar: () => void }) {
  const { avisar, erro } = useAvisos();
  const [nome, setNome] = useState(usuario?.nome ?? '');
  const [papel, setPapel] = useState<Papel>(usuario?.papel ?? 'OPERADOR');
  const [ativo, setAtivo] = useState(usuario?.ativo ?? true);
  const [pin, setPin] = useState('');

  return (
    <form
      className="formulario"
      onSubmit={async (e) => {
        e.preventDefault();
        try {
          if (usuario) {
            await api.atualizarUsuario(usuario.id, { nome, papel, ativo });
            if (pin) await api.redefinirPin(usuario.id, pin);
          } else {
            await api.criarUsuario({ nome, papel, pin });
          }
          avisar(usuario ? 'Operador atualizado.' : `${nome} pode entrar com o PIN cadastrado.`);
          aoSalvar();
        } catch (err) {
          erro(err);
        }
      }}
    >
      <label className="formulario__cheio">
        Nome
        <input autoFocus required maxLength={60} value={nome} onChange={(e) => setNome(e.target.value)} />
      </label>
      <fieldset className="formulario__cheio papel-escolha">
        <legend>Perfil</legend>
        {(Object.keys(DESCRICAO) as Papel[]).map((p) => (
          <label key={p} className={`papel-opcao ${papel === p ? 'papel-opcao--ativa' : ''}`}>
            <input type="radio" name="papel" checked={papel === p} onChange={() => setPapel(p)} />
            <strong>{nomePapel[p]}</strong>
            <small>{DESCRICAO[p]}</small>
          </label>
        ))}
      </fieldset>
      <label>
        {usuario ? 'Novo PIN (opcional)' : 'PIN (4 a 6 números)'}
        <input
          type="password"
          inputMode="numeric"
          autoComplete="new-password"
          required={!usuario}
          minLength={4}
          maxLength={6}
          value={pin}
          onChange={(e) => setPin(e.target.value.replace(/\D/g, ''))}
        />
      </label>
      {usuario && (
        <label className="alternador">
          <input type="checkbox" checked={ativo} onChange={(e) => setAtivo(e.target.checked)} />
          Ativo (pode entrar)
        </label>
      )}
      <div className="formulario__cheio acoes__linha">
        <button className="botao botao--principal">{usuario ? 'Salvar' : 'Cadastrar'}</button>
        {usuario && !souEu && (
          <BotaoExcluir
            oque="do operador"
            aoExcluir={async () => {
              try {
                avisar((await api.excluirUsuario(usuario.id)).mensagem);
                aoSalvar();
              } catch (err) {
                erro(err);
              }
            }}
          />
        )}
      </div>
    </form>
  );
}
