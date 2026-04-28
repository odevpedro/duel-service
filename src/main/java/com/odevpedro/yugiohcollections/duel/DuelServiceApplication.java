package com.odevpedro.yugiohcollections.duel;

import com.odevpedro.yugiohcollections.duel.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableFeignClients
@EnableConfigurationProperties(JwtProperties.class)
public class DuelServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DuelServiceApplication.class, args);
    }
}
