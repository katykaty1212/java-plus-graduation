package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.EndpointHitDto;
import ru.practicum.ViewStatsDto;
import ru.practicum.client.StatsClient;
import ru.practicum.event.EventMapper;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.model.Event;
import ru.practicum.event.service.PublicEventService;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.ParticipationRequestRepository;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.util.DateUtils;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
public class PublicEventController {

    private final PublicEventService eventService;
    private final StatsClient statsClient;
    private final ParticipationRequestRepository requestRepository;

    @GetMapping
    public List<EventShortDto> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size,
            HttpServletRequest request) {

        // ⭐ ВАЛИДАЦИЯ ДАТ ⭐
        if (rangeStart != null && rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new ValidationException("rangeEnd must be after rangeStart");
        }

        log.info("Запрос на получение событий с фильтрами: text={}, categories={}, paid={}, sort={}",
                text, categories, paid, sort);

        // Отправляем статистику
        try {
            statsClient.saveHit(EndpointHitDto.builder()
                    .app("ewm-main-service")
                    .uri("/events")
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now())
                    .build());
            log.info("Статистика для /events сохранена");
        } catch (Exception e) {
            log.error("Ошибка при сохранении статистики для /events: {}", e.getMessage());
        }

        // Сортировка
        Sort sortBy = Sort.unsorted();
        if ("EVENT_DATE".equals(sort)) {
            sortBy = Sort.by("eventDate").ascending();
        }

        Pageable pageable = PageRequest.of(from / size, size, sortBy);

        LocalDateTime start = rangeStart != null ? rangeStart : LocalDateTime.now();
        LocalDateTime end = rangeEnd != null ? rangeEnd : LocalDateTime.now().plusYears(100);

        return eventService.getPublishedEvents(text, categories, paid, start, end, pageable, sort);
    }

    @GetMapping("/{id}")
    public EventFullDto getEvent(
            @PathVariable Long id,
            HttpServletRequest request) {

        // Отправляем статистику
        log.info("Запрос на получение события с id={}", id);

        try {
            statsClient.saveHit(EndpointHitDto.builder()
                    .app("ewm-main-service")
                    .uri("/events/" + id)
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now())
                    .build());
            log.info("Статистика для /events/{} сохранена", id);
        } catch (Exception e) {
            log.error("Ошибка при сохранении статистики для /events/{}: {}", id, e.getMessage());
        }

        Event event = eventService.getPublishedEventById(id);

        int confirmedRequests = (int) requestRepository.countByEventIdAndStatus(id, RequestStatus.CONFIRMED);
        log.info("Количество подтвержденных заявок: {}", confirmedRequests);

        Long views = 0L;
        try {
            String start = DateUtils.format(LocalDateTime.of(2000, 1, 1, 0, 0, 0));
            String end = DateUtils.format(LocalDateTime.now());
            List<ViewStatsDto> stats = statsClient.getStats(start, end, List.of("/events/" + id), true);
            views = stats.isEmpty() ? 0L : stats.get(0).getHits();
            log.info("Количество просмотров: {}", views);
        } catch (Exception e) {
            log.error("Ошибка при получении просмотров: {}", e.getMessage());
        }

        return EventMapper.toEventFullDtoWithStats(event, confirmedRequests, views);
    }
}