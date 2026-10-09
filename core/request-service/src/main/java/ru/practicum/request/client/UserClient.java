package ru.practicum.request.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.user.UserShortDto;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/internal/users/{id}")
    UserShortDto getUser(@PathVariable Long id);

    @GetMapping("/internal/users/{id}/exists")
    Boolean existsById(@PathVariable Long id);
}