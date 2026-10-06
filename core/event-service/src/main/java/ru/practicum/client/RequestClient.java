package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.ParticipationRequestDto;
import ru.practicum.request.RequestStatus;

import java.util.List;

@FeignClient(name = "request-service")
public interface RequestClient {

    @GetMapping("/internal/requests/count")
    Long countByEventAndStatus(@RequestParam Long eventId,
                               @RequestParam RequestStatus status);

    @GetMapping("/internal/requests")
    List<ParticipationRequestDto> findAllByEventId(@RequestParam Long eventId);

    @GetMapping("/internal/requests/by-ids")
    List<ParticipationRequestDto> findAllByEventIdAndIdIn(@RequestParam Long eventId,
                                                          @RequestParam List<Long> ids);

    @PatchMapping("/internal/requests/{id}/status")
    ParticipationRequestDto updateStatus(@PathVariable Long id,
                                         @RequestParam RequestStatus status);
}