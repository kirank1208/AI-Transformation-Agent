package com.example.mcpclient.config;

import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;


@Configuration
@Log4j2
public class AiConfig {

    @Value("${spring.ai.azure.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.azure.openai.endpoint}")
    private String endpoint;

    @Value("${spring.ai.azure.openai.chat.options.model}")
    private String model;

    @Value("${spring.ai.azure.openai.chat.options.version}")
    private String version;

    @Value("${spring.ai.azure.openai.chat.options.temperature}")
    private Double temperature;

    @Value("${spring.ai.azure.openai.chat.options.max-tokens}")
    private Integer maxTokens;

    @Value("${spring.ai.azure.openai.chat.options.timeout-seconds}")
    private Integer timeoutSeconds;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        
        return AzureOpenAiChatModel.builder()
        .endpoint(endpoint)
        .apiKey(apiKey)
        .serviceVersion(version)
        .deploymentName(model)
        .maxTokens(maxTokens)
        .temperature(temperature)
        .timeout(Duration.ofSeconds(timeoutSeconds))
        .build();
    }
}