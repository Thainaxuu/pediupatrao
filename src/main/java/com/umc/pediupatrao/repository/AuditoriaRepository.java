package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.RegistroAuditoria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/**
 * Repositorio somente leitura. Estende Repository (e nao MongoRepository/CrudRepository)
 * justamente para NAO expor save, delete ou deleteAll. A inclusao e feita pelo
 * AuditoriaService com MongoTemplate.insert, que falha caso o registro ja exista.
 */
public interface AuditoriaRepository extends Repository<RegistroAuditoria, String> {

    Optional<RegistroAuditoria> findById(String id);

    Optional<RegistroAuditoria> findTopByOrderBySequenciaDesc();

    List<RegistroAuditoria> findByEntidadeAndEntidadeIdOrderBySequenciaAsc(String entidade, String entidadeId);

    List<RegistroAuditoria> findAllByOrderBySequenciaAsc();

    long count();
}
