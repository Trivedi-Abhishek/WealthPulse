package com.alertservice.config;

import com.alertservice.dal.enums.AlertTypeEnum;
import com.alertservice.service.evaluator.AlertEvaluator;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
@EnableJpaAuditing
public class AlertApplicationConfig {

    @Bean
    public NewTopic createAlertTriggeredTopic() {
        return new NewTopic("alert.triggered", 3, (short) 1);
    }

    @Bean
    public NewTopic createPortfolioRebalanceTriggeredTopic() {
        return new NewTopic("portfolio.rebalance.triggered", 3, (short) 1);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        return redisTemplate;
    }

    @Bean
    public Map<AlertTypeEnum, AlertEvaluator> alertEvaluators(List<AlertEvaluator> evaluators) {
        return evaluators.stream().collect(Collectors.toMap(AlertEvaluator::getType, Function.identity()));
    }
}
