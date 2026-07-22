package com.marketdataservice.config;

import com.marketdataservice.dto.MarketStockPriceEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class MarketApplicationConfig {

    @Bean
    public NewTopic createMarketPriceUpdatedTopic() {
        return new NewTopic("market.price.updated", 3, (short)1);
    }

    @Bean
    public RedisTemplate<String, MarketStockPriceEvent> redisTemplate(RedisConnectionFactory redisConnectionFactory) {

        RedisTemplate<String, MarketStockPriceEvent> redisTemplate=new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }

    @Bean
    public WebClient webClient(@Value("alphavantage.api.baseurl") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
