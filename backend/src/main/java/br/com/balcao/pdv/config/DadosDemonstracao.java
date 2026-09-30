package br.com.balcao.pdv.config;

import br.com.balcao.pdv.fiscal.Ambiente;
import br.com.balcao.pdv.fiscal.ConfiguracaoFiscalDto;
import br.com.balcao.pdv.fiscal.ConfiguracaoFiscalService;
import br.com.balcao.pdv.fiscal.TipoEmissor;
import br.com.balcao.pdv.produto.ProdutoRepository;
import br.com.balcao.pdv.produto.ProdutoRequest;
import br.com.balcao.pdv.produto.ProdutoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Popula um banco vazio com produtos e um emitente fictício em homologação, para testar o PDV de ponta a ponta.
 * Só roda com o perfil {@code demo}. Os GTINs usam o prefixo 200 (uso interno GS1), então não colidem com
 * produtos reais.
 */
@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DadosDemonstracao implements ApplicationRunner {

    private final ProdutoRepository produtoRepository;
    private final ProdutoService produtoService;
    private final ConfiguracaoFiscalService configuracaoFiscalService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (produtoRepository.count() > 0) {
            return;
        }
        log.info("Perfil demo: cadastrando produtos e emitente de exemplo");
        produto("Refrigerante Cola 2L", "10.99", "2000000000015", "001", "UN", "22021000", 48, 12);
        produto("Água Mineral 500ml", "2.50", "2000000000022", "002", "UN", "22011000", 120, 24);
        produto("Café Torrado e Moído 500g", "18.90", "2000000000039", "003", "UN", "09012100", 30, 10);
        produto("Arroz Branco Tipo 1 5kg", "27.90", "2000000000046", "004", "UN", "10063021", 25, 8);
        produto("Feijão Carioca 1kg", "8.49", "2000000000053", "005", "UN", "07133319", 40, 10);
        produto("Açúcar Refinado 1kg", "4.99", "2000000000060", "006", "UN", "17019900", 35, 10);
        produto("Leite Integral 1L", "5.29", "2000000000077", "007", "UN", "04012010", 60, 24);
        produto("Biscoito Recheado 130g", "3.49", "2000000000084", "008", "UN", "19053100", 50, 12);
        produto("Chocolate ao Leite 90g", "6.99", "2000000000091", "009", "UN", "18063210", 3, 6);
        produto("Óleo de Soja 900ml", "7.89", "2000000000107", "010", "UN", "15079011", 28, 10);
        produto("Macarrão Espaguete 500g", "4.59", "2000000000114", "011", "UN", "19021900", 45, 12);
        produto("Detergente Neutro 500ml", "2.79", "2000000000121", "012", "UN", "34022000", 36, 12);
        produto("Papel Higiênico 12 rolos", "21.90", "2000000000138", "013", "UN", "48181000", 20, 6);
        produto("Sabão em Pó 1kg", "14.50", "2000000000145", "014", "UN", "34022000", 18, 6);
        produto("Pão Francês (kg)", "16.90", null, "015", "KG", "19059090", 12, null);
        produto("Banana Prata (kg)", "6.49", null, "016", "KG", "08039000", 30, null);

        configuracaoFiscalService.atualizar(new ConfiguracaoFiscalDto.Request(
                "11222333000181", "111111111111", "MERCADINHO EXEMPLO LTDA", "Mercadinho Exemplo", 1,
                "Rua das Flores", "100", "Centro", "3550308", "São Paulo", "SP", "01001000", "1130000000",
                Ambiente.HOMOLOGACAO, 1, 1, "1", "DEMO0000CSC0000HOMOLOGACAO000001",
                "https://www.homologacao.nfce.fazenda.sp.gov.br/qrcode",
                "https://www.homologacao.nfce.fazenda.sp.gov.br/consulta",
                true, 30, TipoEmissor.SIMULADO));
    }

    private void produto(String nome, String preco, String gtin, String codigo, String unidade, String ncm,
                         int estoque, Integer minimo) {
        produtoService.cadastrar(new ProdutoRequest(nome, new BigDecimal(preco), codigo, gtin, unidade, ncm,
                "5102", 0, "102", minimo != null ? BigDecimal.valueOf(minimo) : null, BigDecimal.valueOf(estoque)));
    }
}
