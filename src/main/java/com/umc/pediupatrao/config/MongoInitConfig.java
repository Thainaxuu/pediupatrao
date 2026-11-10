package com.umc.pediupatrao.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@Order(0)
public class MongoInitConfig implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    public MongoInitConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        for (String colecao : new String[]{"pedidos", "clientes", "usuarios", "produtos",
                "horariosAtendimento", "auditoria"}) {
            if (!mongoTemplate.collectionExists(colecao)) {
                mongoTemplate.createCollection(colecao);
            }
        }
    }
}
