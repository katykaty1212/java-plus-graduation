package ru.practicum.request;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import ru.practicum.client.CollectorClient;
import ru.practicum.exception.ErrorHandler;

@SpringBootApplication
@EnableFeignClients
@Import({ErrorHandler.class, CollectorClient.class})
public class RequestServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(RequestServiceApp.class, args);
    }
}