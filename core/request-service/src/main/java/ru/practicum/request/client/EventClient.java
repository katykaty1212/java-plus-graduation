package ru.practicum.request.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.event.EventInternalDto;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/internal/events/{id}")
    EventInternalDto getEvent(@PathVariable Long id);
}