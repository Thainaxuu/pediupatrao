package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PedidoRepository extends MongoRepository<Pedido, String> {

    List<Pedido> findByClienteId(String clienteId);

    boolean existsByClienteId(String clienteId);

    List<Pedido> findByStatusOrderByDataHoraEntradaDesc(StatusPedido status);

    List<Pedido> findAllByOrderByDataHoraEntradaDesc();
}
