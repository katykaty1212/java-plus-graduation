package ru.practicum.additional.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.user.UserShortDto;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/internal/users/{id}")
    UserShortDto getUser(@PathVariable Long id);

    @GetMapping("/internal/users/{id}/exists")
    Boolean existsById(@PathVariable Long id);

    @GetMapping("/internal/users/by-ids")
    List<UserShortDto> getUsers(@RequestParam List<Long> ids);
}