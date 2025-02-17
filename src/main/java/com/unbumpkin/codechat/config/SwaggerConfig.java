package com.unbumpkin.codechat.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CodeChat API")
                        .version("1.0.0")
                        .description("API for CodeChat application"))
                // .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                // .components(new io.swagger.v3.oas.models.Components()
                //         .addSecuritySchemes("bearerAuth",
                //                 new SecurityScheme()
                //                         .type(SecurityScheme.Type.HTTP)
                //                         .scheme("bearer")
                //                         .bearerFormat("JWT")));
                ;
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/**")
                .build();
    }
}