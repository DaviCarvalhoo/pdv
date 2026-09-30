/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.cliente;

import br.com.balcao.pdv.caixa.Caixa;
import br.com.balcao.pdv.caixa.CaixaService;
import br.com.balcao.pdv.comum.ConflitoException;
import br.com.balcao.pdv.comum.Dinheiro;
import br.com.balcao.pdv.comum.Documento;
import br.com.balcao.pdv.comum.Exclusao;
import br.com.balcao.pdv.comum.NaoEncontradoException;
import br.com.balcao.pdv.comum.RegraNegocioException;
import br.com.balcao.pdv.venda.FormaPagamento;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository repository;
    private final LancamentoClienteRepository lancamentos;
    private final CaixaService caixaService;
    private final Clock relogio;
    private final JdbcTemplate jdbc;

    public record ClienteRequest(String nome, String documento, String telefone, String email,
                                 BigDecimal limiteCredito, String observacao, Boolean ativo) {
    }

    @Transactional(readOnly = true)
    public Page<Cliente> pesquisar(String termo, boolean todos, boolean devedores, Pageable pageable) {
        String t = StringUtils.hasText(termo) ? termo.trim() : null;
        if (t != null && t.replaceAll("\\D", "").length() >= 11 && t.matches("[\\d.\\-/ ]+")) {
            t = Documento.somenteDigitos(t);
        }
        return repository.pesquisar(t, todos ? null : Boolean.TRUE, devedores, pageable);
    }

    @Transactional(readOnly = true)
    public Cliente buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new NaoEncontradoException("Cliente", id));
    }

    @Transactional
    public Cliente salvar(Long id, ClienteRequest r) {
        Cliente c = id == null ? new Cliente() : buscar(id);
        if (!StringUtils.hasText(r.nome())) {
            throw new RegraNegocioException("NOME_OBRIGATORIO", "Informe o nome do cliente.");
        }
        String doc = Documento.somenteDigitos(r.documento());
        if (!doc.isEmpty()) {
            if (!Documento.valido(doc)) {
                throw new RegraNegocioException("DOCUMENTO_INVALIDO", "CPF/CNPJ inválido.");
            }
            if (repository.existsByDocumentoAndIdNot(doc, id == null ? 0L : id)) {
                throw new ConflitoException("DOCUMENTO_DUPLICADO", "Já existe cliente com este CPF/CNPJ.");
            }
        }
        if (r.limiteCredito() != null && r.limiteCredito().signum() < 0) {
            throw new RegraNegocioException("LIMITE_INVALIDO", "O limite de crédito não pode ser negativo.");
        }
        c.setNome(r.nome().trim());
        c.setDocumento(doc.isEmpty() ? null : doc);
        c.setTelefone(texto(r.telefone()));
        c.setEmail(texto(r.email()));
        c.setLimiteCredito(r.limiteCredito() != null ? Dinheiro.valor(r.limiteCredito()) : Dinheiro.ZERO);
        c.setObservacao(texto(r.observacao()));
        if (r.ativo() != null) {
            c.setAtivo(r.ativo());
        }
        return repository.save(c);
    }

    @Transactional(readOnly = true)
    public Page<LancamentoCliente> extrato(Long clienteId, Pageable pageable) {
        buscar(clienteId);
        return lancamentos.findByClienteIdOrderByDataHoraDescIdDesc(clienteId, pageable);
    }

    /** Compra no crediário, dentro da transação de finalização da venda. Respeita o limite. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lancarCompra(Long clienteId, BigDecimal valor, Long vendaId, Long operadorId) {
        Cliente c = travar(clienteId);
        if (!c.isAtivo()) {
            throw new RegraNegocioException("CLIENTE_INATIVO", "O cliente " + c.getNome() + " está inativo.");
        }
        if (c.getSaldoDevedor().add(valor).compareTo(c.getLimiteCredito()) > 0) {
            throw new RegraNegocioException("LIMITE_CREDITO_EXCEDIDO",
                    c.getNome() + " tem " + c.getCreditoDisponivel() + " de crédito disponível no fiado.",
                    Map.of("disponivel", c.getCreditoDisponivel()));
        }
        registrar(c, LancamentoCliente.Tipo.COMPRA, valor, FormaPagamento.CREDIARIO.name(), vendaId, operadorId,
                "Venda #" + vendaId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lancarEstorno(Long clienteId, BigDecimal valor, Long vendaId, Long operadorId) {
        registrar(travar(clienteId), LancamentoCliente.Tipo.ESTORNO, valor, null, vendaId, operadorId,
                "Estorno da venda #" + vendaId);
    }

    /** Recebimento de fiado. Em dinheiro, entra na gaveta do caixa aberto. */
    @Transactional
    public LancamentoCliente receber(Long clienteId, BigDecimal valor, FormaPagamento forma, String observacao,
                                     Long operadorId) {
        return receber(clienteId, valor, forma, observacao, operadorId, null);
    }

    @Transactional
    public LancamentoCliente receber(Long clienteId, BigDecimal valor, FormaPagamento forma, String observacao,
                                     Long operadorId, Long terminalId) {
        if (!Dinheiro.positivo(valor)) {
            throw new RegraNegocioException("VALOR_INVALIDO", "Informe o valor recebido.");
        }
        if (forma == null || forma == FormaPagamento.CREDIARIO) {
            throw new RegraNegocioException("FORMA_INVALIDA", "Escolha como o cliente está pagando.");
        }
        Cliente c = travar(clienteId);
        BigDecimal v = Dinheiro.valor(valor);
        if (v.compareTo(c.getSaldoDevedor()) > 0) {
            throw new RegraNegocioException("PAGAMENTO_MAIOR_QUE_DIVIDA",
                    "O cliente deve " + c.getSaldoDevedor() + ".", Map.of("saldoDevedor", c.getSaldoDevedor()));
        }
        if (forma == FormaPagamento.DINHEIRO) {
            Caixa caixa = caixaService.exigirAberto(terminalId);
            caixaService.registrarRecebimentoCliente(caixaService.travar(caixa.getId()), v, c.getNome(), operadorId);
        }
        return registrar(c, LancamentoCliente.Tipo.PAGAMENTO, v, forma.name(), null, operadorId,
                StringUtils.hasText(observacao) ? observacao.trim() : "Pagamento em " + forma.name().toLowerCase());
    }

    /** Exclui o cliente. Com dívida no fiado, não deixa. Com compras antigas, some das telas e libera o CPF. */
    @Transactional
    public Exclusao excluir(Long id) {
        Cliente c = travar(id);
        if (c.getSaldoDevedor().signum() > 0) {
            throw new RegraNegocioException("CLIENTE_COM_DIVIDA",
                    c.getNome() + " ainda deve " + c.getSaldoDevedor() + " no fiado. Receba ou acerte antes de excluir.");
        }
        Integer usos = jdbc.queryForObject("""
                select (select count(*) from venda where cliente_id = ?)
                     + (select count(*) from lancamento_cliente where cliente_id = ?)
                     + (select count(*) from vale_troca where cliente_id = ?)
                """, Integer.class, id, id, id);
        if (usos == null || usos == 0) {
            repository.delete(c);
            return Exclusao.apagado("Cliente");
        }
        c.setAtivo(false);
        c.setExcluido(true);
        c.setDocumento(null);
        return Exclusao.arquivado("Cliente");
    }

    @Transactional(readOnly = true)
    public BigDecimal totalAReceber() {
        return Dinheiro.valor(repository.totalAReceber());
    }

    private LancamentoCliente registrar(Cliente c, LancamentoCliente.Tipo tipo, BigDecimal valor, String forma,
                                        Long vendaId, Long operadorId, String observacao) {
        BigDecimal v = Dinheiro.valor(valor);
        BigDecimal saldo = tipo == LancamentoCliente.Tipo.COMPRA ? c.getSaldoDevedor().add(v)
                : c.getSaldoDevedor().subtract(v).max(Dinheiro.ZERO);
        c.aplicarSaldo(Dinheiro.valor(saldo));
        return lancamentos.save(new LancamentoCliente(c, tipo, v, forma, vendaId, operadorId, observacao,
                OffsetDateTime.now(relogio)));
    }

    private Cliente travar(Long id) {
        return repository.travarPorId(id).orElseThrow(() -> new NaoEncontradoException("Cliente", id));
    }

    private static String texto(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
