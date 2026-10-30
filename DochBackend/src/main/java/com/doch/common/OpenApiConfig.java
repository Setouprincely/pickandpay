package com.doch.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(
                title = "DOCH Platform API",
                version = "1.0.0",
                description = "This API provides all backend services for the DOCH digital ecosystem, including e-commerce, logistics, and user management."
        ),
        servers = {
                // Resolved from the environment so Swagger "Execute" hits the right host.
                // Set SWAGGER_SERVER_URL=http://<deployed-host>:8080 on the server.
                @Server(url = "${application.swagger.server-url:http://localhost:8080}", description = "Current Environment")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT Authentication Token",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
