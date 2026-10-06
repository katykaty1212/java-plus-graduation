package ru.practicum.request;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
        "ru.practicum.request",
        "ru.practicum.exception",
        "ru.practicum.common",
        "ru.practicum.client"
})
@EnableFeignClients(basePackages = {
        "ru.practicum.request.client",
        "ru.practicum.client"
})
public class RequestServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(RequestServiceApp.class, args);
    }
}