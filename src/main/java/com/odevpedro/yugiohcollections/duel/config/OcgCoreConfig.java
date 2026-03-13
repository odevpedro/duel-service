package com.odevpedro.yugiohcollections.duel.config;

import com.odevpedro.yugiohcollections.duel.adapter.out.ocgcore.OcgCoreBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OcgCoreConfig {

    @Bean
    public OcgCoreBridge ocgCoreBridge() {
        return new OcgCoreBridge();
    }
}
