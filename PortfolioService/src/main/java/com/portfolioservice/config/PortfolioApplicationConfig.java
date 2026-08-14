package com.portfolioservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class PortfolioApplicationConfig {

    @Bean
    public NewTopic createOrderExecutedTopic() {
        return new NewTopic("portfolio.order.executed", 3, (short)1);
    }

    @Bean
    public NewTopic createPortfolioHoldingsUpdatedTopic() {
        return new NewTopic("portfolio.holdings.updated", 3, (short)1);
    }
}
