package com.example.egresos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${ms.productos.url}")
    private String productosUrl;

    @Value("${ms.kardex.url}")
    private String kardexUrl;

    @Bean
    public WebClient webClientProductos(WebClient.Builder builder) {
        return builder.baseUrl(productosUrl).build();
    }

    @Bean
    public WebClient webClientKardex(WebClient.Builder builder) {
        return builder.baseUrl(kardexUrl).build();
    }
}