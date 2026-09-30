/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import CadastroTerminais from '../components/CadastroTerminais';
import { api, type Loja } from '../lib/api';
import { useAvisos, useSessao } from '../lib/contexto';
import { campoNumero, iniciais, moeda, parseValor } from '../lib/format';

const CORES = ['#D9482B', '#E07A1F', '#C9A227', '#3E8E4E', '#1F7A7A', '#2F6FDB', '#5B4BC4', '#B23A7A', '#2B2B2B'];

export default function PaginaLoja() {
  const { avisar, erro } = useAvisos();
  const { recarregarLoja } = useSessao();
  const [loja, setLoja] = useState<Loja | null>(null);
  const [f, setF] = useState<Record<string, string | boolean | undefined>>({});

  useEffect(() => {
    api
      .loja()
      .then((l) => {
        setLoja(l);
        setF({
          ...l,
          limiteGaveta: campoNumero(l.limiteGaveta),
          descontoMaxOperador: campoNumero(l.descontoMaxOperador),
          aliquotaTributos: campoNumero(l.aliquotaTributos),
          balancaDigitosCodigo: String(l.balancaDigitosCodigo),
          bloqueioInatividadeMin: String(l.bloqueioInatividadeMin ?? 10),
        } as unknown as Record<string, string>);
      })
      .catch(erro);
  }, [erro]);

  if (!loja) return <div className="pagina" />;

  const texto = (k: string) => ({
    value: (f[k] as string) ?? '',
    onChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => setF({ ...f, [k]: e.target.value }),
  });
  const cor = (f.corDestaque as string) || '#D9482B';

  const carregarLogo = async (arquivo: File) => {
    if (arquivo.size > 300_000) return avisar('O logo deve ter no máximo 300 KB.', 'erro');
    const leitor = new FileReader();
    leitor.onload = () => setF((atual) => ({ ...atual, logo: String(leitor.result) }));
    leitor.readAsDataURL(arquivo);
  };

  const salvar = async (e: React.FormEvent) => {
    e.preventDefault();
    const n = (k: string) => (f[k] ? parseValor(String(f[k])) : null);
    try {
      const l = await api.salvarLoja({
        ...(f as Partial<Loja>),
        logo: (f.logo as string) || '',
        limiteGaveta: n('limiteGaveta') ?? undefined,
        descontoMaxOperador: n('descontoMaxOperador') ?? 0,
        aliquotaTributos: n('aliquotaTributos') ?? undefined,
        balancaDigitosCodigo: Number(f.balancaDigitosCodigo),
        bloqueioInatividadeMin: Number(f.bloqueioInatividadeMin || 0),
      });
      setLoja(l);
      await recarregarLoja();
      avisar('Loja atualizada. A identidade já vale em todas as telas.');
    } catch (err) {
      erro(err);
    }
  };

  return (
    <div className="pagina pagina--larga">
      <header className="pagina__topo">
        <div>
          <p className="sobretitulo">Administração</p>
          <h1>Loja</h1>
        </div>
        <Link to="/fiscal" className="link">
          Dados fiscais e NFC-e →
        </Link>
      </header>

      <form className="loja" onSubmit={salvar}>
        <div className="loja__campos">
          <section className="secao-form">
            <h2>Identidade</h2>
            <p className="dica">Aparece no login, no menu, no PDV e no cupom.</p>
            <div className="formulario">
              <label className="formulario__cheio">
                Nome da loja
                <input required maxLength={60} {...texto('nomeFantasia')} />
              </label>
              <label className="formulario__cheio">
                Frase de boas-vindas
                <input maxLength={120} placeholder="Ex.: Do bairro, pro bairro" {...texto('slogan')} />
              </label>
              <div className="formulario__cheio logo-campo">
                <span className="rotulo">Logo</span>
                <div className="logo-campo__linha">
                  {f.logo ? <img src={f.logo as string} alt="Logo atual" /> : <span className="marca__selo" style={{ background: cor }}>{iniciais(String(f.nomeFantasia ?? ''))}</span>}
                  <label className="botao botao--secundario">
                    Enviar imagem
                    <input type="file" accept="image/png,image/jpeg,image/webp,image/svg+xml" hidden onChange={(e) => e.target.files?.[0] && carregarLogo(e.target.files[0])} />
                  </label>
                  {f.logo && (
                    <button type="button" className="botao botao--fantasma" onClick={() => setF({ ...f, logo: '' })}>
                      Remover
                    </button>
                  )}
                </div>
              </div>
              <div className="formulario__cheio">
                <span className="rotulo">Cor da loja</span>
                <div className="cores" role="radiogroup" aria-label="Cor da loja">
                  {CORES.map((c) => (
                    <button type="button" key={c} role="radio" aria-checked={cor.toUpperCase() === c} aria-label={c} className="cor" style={{ background: c }} onClick={() => setF({ ...f, corDestaque: c })} />
                  ))}
                  <label className="cor cor--livre" title="Outra cor">
                    <input type="color" value={cor} onChange={(e) => setF({ ...f, corDestaque: e.target.value.toUpperCase() })} />
                  </label>
                </div>
              </div>
              <label className="formulario__cheio">
                Mensagem no fim do cupom
                <input maxLength={255} placeholder="Obrigado pela preferência!" {...texto('mensagemCupom')} />
              </label>
            </div>
          </section>

          <section className="secao-form">
            <h2>Regras da venda</h2>
            <div className="formulario">
              <label>
                Estoque acabou
                <select {...texto('politicaEstoque')}>
                  <option value="PERMITIR_E_AVISAR">Vender e avisar</option>
                  <option value="BLOQUEAR">Bloquear a venda</option>
                </select>
              </label>
              <label>
                Desconto sem gerente (até %)
                <input inputMode="decimal" {...texto('descontoMaxOperador')} />
              </label>
              <label>
                Avisar sangria acima de (R$)
                <input inputMode="decimal" placeholder="sem aviso" {...texto('limiteGaveta')} />
              </label>
              <label className="alternador formulario__cheio">
                <input type="checkbox" checked={f.caixaSoGerente !== false} onChange={(e) => setF({ ...f, caixaSoGerente: e.target.checked })} />
                Só gerente ou administrador abre e fecha o caixa (o operador pede o PIN do gerente)
              </label>
              <label>
                Travar a tela após (min) <small>0 desliga · pede o PIN para voltar</small>
                <input inputMode="numeric" {...texto('bloqueioInatividadeMin')} />
              </label>
              <label>
                Tributos aproximados (%) <small>Lei 12.741 · IBPT</small>
                <input inputMode="decimal" placeholder="ex.: 31,45" {...texto('aliquotaTributos')} />
              </label>
            </div>
          </section>

          <CadastroTerminais />

          <section className="secao-form">
            <h2>PIX</h2>
            <p className="dica">Com a chave cadastrada, o PDV mostra o QR Code já no valor da venda. A confirmação do recebimento continua no app do banco.</p>
            <div className="formulario">
              <label className="formulario__cheio">
                Chave PIX
                <input placeholder="CNPJ, e-mail, telefone ou chave aleatória" maxLength={77} {...texto('chavePix')} />
              </label>
              <label>
                Nome do recebedor
                <input maxLength={25} {...texto('pixRecebedor')} />
              </label>
              <label>
                Cidade
                <input maxLength={15} {...texto('pixCidade')} />
              </label>
            </div>
          </section>

          <section className="secao-form">
            <h2>Etiqueta de balança</h2>
            <p className="dica">Para o leitor entender a etiqueta impressa pela balança (açougue, frios, padaria). Confira na configuração da balança.</p>
            <div className="formulario">
              <label>
                Prefixo
                <input maxLength={1} inputMode="numeric" {...texto('balancaPrefixo')} />
              </label>
              <label>
                Dígitos do código
                <select {...texto('balancaDigitosCodigo')}>
                  <option value="4">4</option>
                  <option value="5">5</option>
                  <option value="6">6</option>
                </select>
              </label>
              <label className="formulario__cheio">
                A etiqueta traz
                <select {...texto('balancaTipoValor')}>
                  <option value="PRECO">o preço total (R$)</option>
                  <option value="PESO">o peso (kg)</option>
                </select>
              </label>
            </div>
          </section>

          <div className="acoes__linha barra-salvar">
            <button className="botao botao--principal botao--grande">Salvar loja</button>
          </div>
        </div>

        <aside className="loja__previa" aria-label="Prévia">
          <p className="rotulo">Prévia</p>
          <div className="previa-pdv" style={{ '--marca': cor } as React.CSSProperties}>
            <div className="previa-pdv__lateral">
              {f.logo ? <img src={f.logo as string} alt="" /> : <span className="marca__selo">{iniciais(String(f.nomeFantasia ?? ''))}</span>}
              <strong>{String(f.nomeFantasia || 'Minha loja')}</strong>
            </div>
            <div className="previa-pdv__corpo">
              <div className="etiqueta etiqueta--mini">
                <span className="etiqueta__furo" />
                <span className="etiqueta__rotulo">Total</span>
                <span className="etiqueta__valor">
                  <small>R$</small>42<sup>,90</sup>
                </span>
              </div>
              <span className="botao botao--principal">Receber</span>
            </div>
          </div>
          <div className="previa-cupom">
            {f.logo && <img src={f.logo as string} alt="" />}
            <strong>{String(f.nomeFantasia || 'Minha loja')}</strong>
            <span>Valor a pagar {moeda(42.9)}</span>
            {f.aliquotaTributos && <span>Tributos aprox. {moeda((42.9 * parseValor(String(f.aliquotaTributos))) / 100)}</span>}
            <em>{String(f.mensagemCupom || 'Obrigado pela preferência!')}</em>
          </div>
        </aside>
      </form>
    </div>
  );
}
