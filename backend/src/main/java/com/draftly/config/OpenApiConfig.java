package com.draftly.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI draftlyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Draftly API")
                        .description("Backend APIs for Draftly, a Gmail AI Reply Agent that analyzes email context and generates AI reply drafts.")
                        .version("v1"));
    }
}
