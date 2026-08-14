package com.pnlconsumerservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PnlDataApplicationConfig {

    @Bean
    public NewTopic createPortfolioMetricsUpdated() {
        return new NewTopic("portfolio.metrics.updated", 3, (short)1);
    }
}
