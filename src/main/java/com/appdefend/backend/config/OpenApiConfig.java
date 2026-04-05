package com.appdefend.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI appDefendOpenApi() {
        String securitySchemeName = "bearerAuth";
        return new OpenAPI()
            .info(new Info()
                .title("App Defend Backend API")
                .description("Enterprise application security backend APIs for authentication, RBAC, users, views, permissions and OEM integrations.")
                .version("v1")
                .contact(new Contact().name("App Defend"))
                .license(new License().name("Internal Use")))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .schemaRequirement(securitySchemeName, new SecurityScheme()
                .name(securitySchemeName)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT"));
    }
}
