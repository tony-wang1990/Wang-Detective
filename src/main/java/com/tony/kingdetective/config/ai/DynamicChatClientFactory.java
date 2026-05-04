package com.tony.kingdetective.config.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

/**
 * @ClassName DynamicChatClientFactory
 * @Description: Dynamic ChatClient factory for Spring AI 1.0.0-M5
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-09-23 14:42
 **/
@Component
public class DynamicChatClientFactory {
    public ChatClient create(String apiKey, String baseUrl, String model) {
        // Create OpenAiApi instance using constructor
        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey);
        
        // Create OpenAiChatOptions
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(model)
                .build();
        
        // Create OpenAiChatModel using constructor
        OpenAiChatModel chatModel = new OpenAiChatModel(openAiApi, options);
        
        // Create ChatClient
        return ChatClient.builder(chatModel).build();
    }
}