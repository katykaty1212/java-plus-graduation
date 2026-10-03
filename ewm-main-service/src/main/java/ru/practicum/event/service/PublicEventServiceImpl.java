package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ViewStatsDto;
import ru.practicum.client.StatsClient;
import ru.practicum.event.EventMapper;
import ru.practicum.event.EventRepository;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.ParticipationRequestRepository;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.util.DateUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PublicEventServiceImpl implements PublicEventService {

    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final StatsClient statsClient;

    @Override
    public List<EventShortDto> getPublishedEvents(
            String text,
            List<Long> categories,
            Boolean paid,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable,
            String sort) {

        // 1. Берем ВСЕ опубликованные события за период
        List<Event> allEvents = eventRepository.findAllByStateAndEventDateBetween(
                EventState.PUBLISHED, start, end);

        if (allEvents.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Фильтруем в Java
        List<Event> filtered = allEvents.stream()
                .filter(e -> text == null || text.isEmpty() ||
                        e.getAnnotation().toLowerCase().contains(text.toLowerCase()) ||
                        e.getDescription().toLowerCase().contains(text.toLowerCase()))
                .filter(e -> categories == null || categories.isEmpty() ||
                        categories.contains(e.getCategory().getId()))
                .filter(e -> paid == null || e.getPaid().equals(paid))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Применяем пагинацию в Java
        int startIndex = (int) pageable.getOffset();
        int endIndex = Math.min(startIndex + pageable.getPageSize(), filtered.size());

        if (startIndex >= filtered.size()) {
            return Collections.emptyList();
        }

        List<Event> pagedEvents = filtered.subList(startIndex, endIndex);

        // 4. Получаем ID для просмотров
        List<Long> ids = pagedEvents.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        // 5. Получаем просмотры
        Map<Long, Long> views = getViewsForEvents(ids);

        // 6. Маппим в DTO
        List<EventShortDto> result = pagedEvents.stream()
                .map(e -> {
                    int confirmed = (int) requestRepository.countByEventIdAndStatus(
                            e.getId(), RequestStatus.CONFIRMED);
                    return EventMapper.toEventShortDtoWithStats(
                            e, confirmed, views.getOrDefault(e.getId(), 0L));
                })
                .collect(Collectors.toList());

        // 7. Сортировка по просмотрам если нужно
        if ("VIEWS".equals(sort)) {
            result.sort(Comparator.comparing(EventShortDto::getViews));
        }

        return result;
    }

    @Override
    public Event getPublishedEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        return event;
    }

    private Map<Long, Long> getViewsForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .collect(Collectors.toList());

            // Используем DateUtils для форматирования дат
            String start = DateUtils.format(LocalDateTime.of(2000, 1, 1, 0, 0, 0));
            String end = DateUtils.format(LocalDateTime.now());

            // StatsClient принимает String
            List<ViewStatsDto> stats = statsClient.getStats(start, end, uris, true);

            return stats.stream()
                    .collect(Collectors.toMap(
                            s -> Long.parseLong(s.getUri().replace("/events/", "")),
                            ViewStatsDto::getHits,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.error("Не удалось получить просмотры для событий: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}