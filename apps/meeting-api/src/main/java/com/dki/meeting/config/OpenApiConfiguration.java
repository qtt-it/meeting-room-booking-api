package com.dki.meeting.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfiguration {
  @Bean
  org.springdoc.core.customizers.OpenApiCustomizer publicAuthenticationRoutes() {
    return api -> {
      for (String name : java.util.List.of("login", "refresh", "logout")) {
        var path = api.getPaths().get("/api/v1/auth/" + name);
        if (path != null && path.getPost() != null) path.getPost().setSecurity(java.util.List.of());
      }
    };
  }

  @Bean
  OpenAPI api() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Meeting room booking API")
                .version("1.0")
                .description(
                    "Internal room reservations with approval, recurrence and employee invitations. See docs/ for business rules and examples."))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }
}
