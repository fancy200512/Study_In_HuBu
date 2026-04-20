package com.cy.config;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfiguration {

    @Bean
    public ChatClient chatClient(OpenAiChatModel model) {
        return ChatClient
                .builder(model)
//                .defaultOptions(ChatOptions.builder().model("qwen-vl-ocr").build())
                .defaultAdvisors(new SimpleLoggerAdvisor())     //配置日志adviser
                .build();
    }


}
