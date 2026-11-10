package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.StatusPedido;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

@Configuration
public class AppConfig {

    /** Relogio injetavel: permite testes com data/hora fixa. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** Converte status gravados pela versao anterior (ex.: EM_PREPARO, ENTREGUE) sem quebrar a leitura. */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(new StatusPedidoReadingConverter()));
    }

    @ReadingConverter
    static class StatusPedidoReadingConverter implements Converter<String, StatusPedido> {
        @Override
        public StatusPedido convert(String source) {
            return StatusPedido.fromLegado(source);
        }
    }
}
