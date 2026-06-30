package com.example.egresos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class EgresoApplication {
    public static void main(String[] args) {
        SpringApplication.run(EgresoApplication.class, args);
    }
}