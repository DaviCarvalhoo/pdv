package br.com.balcao.pdv.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RelogioConfig {

    @Bean
    RelogioAjustavel relogio() {
        return new RelogioAjustavel(Clock.systemDefaultZone());
    }
}
