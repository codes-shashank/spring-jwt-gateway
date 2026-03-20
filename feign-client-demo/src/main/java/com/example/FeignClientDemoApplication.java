package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients  // Scans for all @FeignClient interfaces in this package
@EnableDiscoveryClient
public class FeignClientDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeignClientDemoApplication.class, args);
    }
}
