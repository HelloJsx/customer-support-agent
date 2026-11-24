package com.support.agent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customerSupportOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Customer Support Agent API")
                .version("0.1.0")
                .description("多 Agent 智能客服 / 工单系统 API")
                .contact(new Contact().name("Platform Team")));
    }
}
