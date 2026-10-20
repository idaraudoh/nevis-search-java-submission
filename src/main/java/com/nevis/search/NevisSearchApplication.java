package com.nevis.search;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@OpenAPIDefinition(info = @Info(
        title = "Nevis Search API",
        version = "1.0.0",
        description = """
                Search across wealth-management clients and their documents.
                Client search matches name, email, description and social links, and forgives typos.
                Document search expands the query, so `address proof` also finds a `utility bill`.
                Every document hit carries a quick summary."""))
public class NevisSearchApplication {
    public static void main(String[] args) {
        SpringApplication.run(NevisSearchApplication.class, args);
    }
}
