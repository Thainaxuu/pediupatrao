package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.HistoricoStatus;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEntrega;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.service.CalculadoraPedido;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Gera dados ficticios para demonstrar o dashboard (app.seed.pedidos-demo=true).
 * So executa se ainda nao existir nenhum pedido com itens. Use apenas em banco proprio,
 * nunca no banco compartilhado da turma. Dados de demonstracao nao passam pela auditoria.
 */
@Component
@Order(10)
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final boolean habilitado;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;
    private final Clock clock;
    private final ZoneId zona;

    public DemoDataSeeder(@Value("${app.seed.pedidos-demo:false}") boolean habilitado,
                          ProdutoRepository produtoRepository, ClienteRepository clienteRepository,
                          PedidoRepository pedidoRepository, Clock clock,
                          @Value("${app.zona-horaria:America/Sao_Paulo}") String zona) {
        this.habilitado = habilitado;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
        this.clock = clock;
        this.zona = ZoneId.of(zona);
    }

    @Override
    public void run(String... args) {
        if (!habilitado) {
            return;
        }
        boolean jaTemPedidos = pedidoRepository.findAll().stream().anyMatch(p -> !p.isLegado());
        if (jaTemPedidos) {
            log.info("Seed de demonstração ignorado: já existem pedidos com itens.");
            return;
        }
        List<Produto> produtos = garantirProdutos();
        List<Cliente> clientes = garantirClientes();
        List<Pedido> pedidos = gerarPedidos(produtos, clientes, 60, new Random(42));
        pedidoRepository.saveAll(pedidos);
        log.info("Seed de demonstração: {} pedidos gerados.", pedidos.size());
    }

    private List<Produto> garantirProdutos() {
        List<Produto> existentes = produtoRepository.findAll().stream().filter(Produto::isDisponivel)
                .filter(p -> p.getPreco() != null && p.getPreco() > 0).toList();
        if (existentes.size() >= 5) {
            return existentes;
        }
        Object[][] dados = {
                {"Calabresa", "Pizza", 49.90}, {"Mussarela", "Pizza", 44.90}, {"Portuguesa", "Pizza", 54.90},
                {"Frango com Catupiry", "Pizza", 56.90}, {"Margherita", "Pizza", 52.90}, {"Quatro Queijos", "Pizza", 58.90},
                {"Esfiha de Carne", "Esfiha", 6.50}, {"Esfiha de Queijo", "Esfiha", 6.50},
                {"Refrigerante 2L", "Bebida", 14.00}, {"Suco Natural", "Bebida", 9.00}, {"Água", "Bebida", 4.00},
                {"Batata Frita", "Porção", 29.90}, {"Brownie", "Sobremesa", 15.90}};
        List<Produto> lista = new ArrayList<>();
        for (Object[] d : dados) {
            Produto p = new Produto();
            p.setNome((String) d[0]);
            p.setTipo((String) d[1]);
            p.setPreco((Double) d[2]);
            p.setAtivo(true);
            lista.add(p);
        }
        return produtoRepository.saveAll(lista);
    }

    private List<Cliente> garantirClientes() {
        List<Cliente> existentes = clienteRepository.findAll();
        if (existentes.size() >= 5) {
            return existentes;
        }
        String[] nomes = {"Ana Souza", "Bruno Lima", "Carla Dias", "Diego Alves", "Elisa Rocha", "Fábio Nunes",
                "Gabriela Melo", "Henrique Costa", "Isabela Reis", "João Pedro"};
        List<Cliente> lista = new ArrayList<>();
        for (int i = 0; i < nomes.length; i++) {
            Cliente c = new Cliente();
            c.setNome(nomes[i]);
            c.setTelefone("(11) 9" + (8000 + i) + "-" + (1000 + i * 7));
            c.setLogradouro("Rua Demonstração");
            c.setNumero(String.valueOf(100 + i));
            c.setBairro("Centro");
            c.setCidade("Mogi das Cruzes");
            c.setEstado("SP");
            lista.add(c);
        }
        return clienteRepository.saveAll(lista);
    }

    List<Pedido> gerarPedidos(List<Produto> produtos, List<Cliente> clientes, int dias, Random r) {
        List<Pedido> pedidos = new ArrayList<>();
        LocalDate hoje = LocalDate.now(clock.withZone(zona));
        Instant agora = Instant.now(clock);
        int[] horas = {11, 12, 13, 18, 19, 19, 20, 20, 20, 21, 21, 22, 23};
        String[] usuarios = {"atendente01", "gerente01"};

        for (int d = dias - 1; d >= 0; d--) {
            LocalDate dia = hoje.minusDays(d);
            boolean fimDeSemana = dia.getDayOfWeek() == DayOfWeek.FRIDAY || dia.getDayOfWeek() == DayOfWeek.SATURDAY
                    || dia.getDayOfWeek() == DayOfWeek.SUNDAY;
            int quantidade = (fimDeSemana ? 8 : 4) + r.nextInt(5) + (dias - d) / 15;
            for (int n = 0; n < quantidade; n++) {
                Instant entrada = dia.atTime(horas[r.nextInt(horas.length)], r.nextInt(60)).atZone(zona).toInstant();
                if (entrada.isAfter(agora)) {
                    continue;
                }
                Cliente cliente = clientes.get(r.nextInt(clientes.size()));
                Pedido p = new Pedido();
                p.setClienteId(cliente.getId());
                p.setClienteNome(cliente.getNome());
                p.setTipoEntrega(r.nextInt(10) < 7 ? TipoEntrega.ENTREGA : TipoEntrega.RETIRADA);
                List<ItemPedido> itens = new ArrayList<>();
                int qtdItens = 1 + r.nextInt(3);
                for (int i = 0; i < qtdItens; i++) {
                    // Pizzas (primeiros produtos) aparecem com mais frequencia
                    Produto prod = produtos.get(Math.min(produtos.size() - 1, (int) Math.abs(r.nextGaussian() * 4)));
                    itens.add(new ItemPedido(prod.getId(), prod.getNome(), prod.getTipo(), 1 + r.nextInt(2),
                            BigDecimal.valueOf(prod.getPreco()).setScale(2, RoundingMode.HALF_EVEN)));
                }
                p.setItens(itens);
                p.setPercentualDesconto(r.nextInt(10) == 0 ? new BigDecimal("10") : BigDecimal.ZERO);
                CalculadoraPedido.recalcular(p);
                String usuario = usuarios[r.nextInt(usuarios.length)];
                p.setDataHoraEntrada(entrada);
                p.setRegistradoPor(usuario);
                p.setRegistradoPorPerfil(usuario.startsWith("gerente") ? "GERENTE" : "ATENDENTE");
                p.getHistorico().add(new HistoricoStatus(null, StatusPedido.RECEBIDO, usuario,
                        p.getRegistradoPorPerfil(), entrada, "Entrada do pedido"));

                long minutosDesdeEntrada = ChronoUnit.MINUTES.between(entrada, agora);
                if (r.nextInt(100) < 6) {
                    p.setStatus(StatusPedido.CANCELADO);
                    p.setCanceladoPor("gerente01");
                    p.setDataHoraCancelamento(entrada.plus(10, ChronoUnit.MINUTES));
                    p.setMotivoCancelamento("Cliente desistiu (dado de demonstração)");
                } else if (minutosDesdeEntrada < 20) {
                    p.setStatus(StatusPedido.EM_PREPARACAO);
                } else if (minutosDesdeEntrada < 40) {
                    p.setStatus(StatusPedido.PRONTO);
                } else {
                    Instant saida = entrada.plus(25 + r.nextInt(25), ChronoUnit.MINUTES);
                    p.setDataHoraSaida(saida);
                    p.setResponsavelSaida(usuario);
                    p.setStatus(StatusPedido.FINALIZADO);
                    p.setDataHoraFinalizacao(saida.plus(20, ChronoUnit.MINUTES));
                    p.setResponsavelFinalizacao(usuario);
                }
                pedidos.add(p);
            }
        }
        return pedidos;
    }
}
