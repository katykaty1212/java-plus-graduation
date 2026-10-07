package ru.practicum.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.*;
import ru.practicum.event.service.PrivateEventService;
import ru.practicum.request.ParticipationRequestDto;
import ru.practicum.request.RequestStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PrivateEventController {

    private final PrivateEventService eventService;

    // 1. Получение событий, добавленных текущим пользователем
    @GetMapping
    public List<EventShortDto> getEvents(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        return eventService.getEvents(userId, from, size);
    }

    // 2. Добавление нового события
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(
            @PathVariable Long userId,
            @Valid @RequestBody NewEventDto newEventDto) {
        return eventService.addEvent(userId, newEventDto);
    }

    // 3. Получение полной информации о событии, добавленном текущим пользователем
    @GetMapping("/{eventId}")
    public EventFullDto getEventById(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        return eventService.getEventById(userId, eventId);
    }

    // 4. Изменение события, добавленного текущим пользователем
    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventUserRequest updateRequest) {
        return eventService.updateEvent(userId, eventId, updateRequest);
    }

    // 5. Получение информации о запросах на участие в событии текущего пользователя
    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getEventRequests(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        return eventService.getEventRequests(userId, eventId);
    }

    // 6. Изменение статуса (подтверждение/отклонение) заявок на участие в событии
    @PatchMapping("/{eventId}/requests")
    public ResponseEntity<?> changeRequestStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            HttpServletRequest request) throws IOException {

        String contentType = request.getContentType();
        String rawBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        // Вариант 3: stdout — не зависит от логгера
        System.out.println("PATCH_DIAG contentType=" + contentType
                + " bodyLen=" + rawBody.length()
                + " body=" + rawBody);
        System.out.println("PATCH_DIAG RequestStatus.values=" + Arrays.toString(RequestStatus.values()));
        System.out.println("PATCH_DIAG RequestStatus.class=" + RequestStatus.class
                .getProtectionDomain().getCodeSource().getLocation());

        // Вариант 1: заголовки — попадут в HTML-отчёт Newman
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Diag-CT", String.valueOf(contentType));
        headers.add("X-Diag-Len", String.valueOf(rawBody.length()));
        headers.add("X-Diag-Body",
                rawBody.length() > 80 ? rawBody.substring(0, 80) : rawBody);

        try {
            EventRequestStatusUpdateRequest parsed =
                    new ObjectMapper().readValue(rawBody, EventRequestStatusUpdateRequest.class);
            headers.add("X-Diag-Parse", "OK");
            return ResponseEntity.ok().headers(headers)
                    .body(eventService.changeRequestStatus(userId, eventId, parsed));
        } catch (Exception e) {
            headers.add("X-Diag-Parse", "FAIL");
            headers.add("X-Diag-Ex", e.getClass().getSimpleName());
            headers.add("X-Diag-Msg",
                    String.valueOf(e.getMessage()).replaceAll("[\\r\\n]", " "));
            return ResponseEntity.status(409).headers(headers)
                    .body(Map.of("exception", e.getClass().getName(),
                            "message", String.valueOf(e.getMessage())));
        }
    }
}
