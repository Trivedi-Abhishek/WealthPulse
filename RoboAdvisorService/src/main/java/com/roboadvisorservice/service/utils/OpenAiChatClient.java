package com.roboadvisorservice.service.utils;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiChatClient {
    private final ChatClient chatClient;

    @CircuitBreaker(name = "openai-chat", fallbackMethod = "recommendationUnavailable")
    @Retry(name = "openai-chat")
    public String generate(String prompt) {
        return chatClient.prompt(prompt).call().content();
    }

    private String recommendationUnavailable(String prompt, Throwable t) {
        log.error("OpenAI call failed, skipping recommendation: {}", t.getMessage());
        return null;
    }
}
