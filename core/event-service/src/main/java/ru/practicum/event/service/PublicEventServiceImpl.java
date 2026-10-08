package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.EndpointHitDto;
import ru.practicum.ViewStatsDto;
import ru.practicum.client.RequestClient;
import ru.practicum.client.StatsClient;
import ru.practicum.client.UserClient;
import ru.practicum.event.EventMapper;
import ru.practicum.event.EventRepository;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.PublicEventSearchParams;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.RequestStatus;
import ru.practicum.user.UserShortDto;
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
    private final RequestClient requestClient;
    private final UserClient userClient;
    private final StatsClient statsClient;

    @Value("${stats.app-name}")
    private String appName;

    @Override
    public List<EventShortDto> getPublishedEvents(PublicEventSearchParams params) {

        // 1. Валидация дат
        if (params.getRangeStart() != null && params.getRangeEnd() != null
                && params.getRangeEnd().isBefore(params.getRangeStart())) {
            throw new ValidationException("Дата окончания должна быть позже даты начала");
        }

        LocalDateTime start = params.getRangeStart() != null
                ? params.getRangeStart() : LocalDateTime.now();
        LocalDateTime end = params.getRangeEnd() != null
                ? params.getRangeEnd() : LocalDateTime.now().plusYears(100);

        // 2. Пагинация
        Sort sortBy = Sort.unsorted();
        if ("EVENT_DATE".equals(params.getSort())) {
            sortBy = Sort.by("eventDate").ascending();
        }
        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize(), sortBy);

        // 3. Берем ВСЕ опубликованные события за период
        List<Event> allEvents = eventRepository.findAllByStateAndEventDateBetween(
                EventState.PUBLISHED, start, end);

        if (allEvents.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. Фильтруем в Java
        List<Event> filtered = allEvents.stream()
                .filter(e -> params.getText() == null || params.getText().isEmpty() ||
                        e.getAnnotation().toLowerCase().contains(params.getText().toLowerCase()) ||
                        e.getDescription().toLowerCase().contains(params.getText().toLowerCase()))
                .filter(e -> params.getCategories() == null || params.getCategories().isEmpty() ||
                        params.getCategories().contains(e.getCategory().getId()))
                .filter(e -> params.getPaid() == null || e.getPaid().equals(params.getPaid()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            return Collections.emptyList();
        }

        // 5. Пагинация в Java
        int startIndex = (int) pageable.getOffset();
        int endIndex = Math.min(startIndex + pageable.getPageSize(), filtered.size());

        if (startIndex >= filtered.size()) {
            return Collections.emptyList();
        }

        List<Event> pagedEvents = filtered.subList(startIndex, endIndex);

        // 6. Получаем просмотры
        List<Long> ids = pagedEvents.stream()
                .map(Event::getId)
                .collect(Collectors.toList());
        Map<Long, Long> views = getViewsForEvents(ids);

        // 7. Батч-запросы вместо N+1
        List<Long> eventIds = pagedEvents.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Long, Long> confirmedMap = requestClient.countByEventIdsAndStatus(
                eventIds, RequestStatus.CONFIRMED);

        List<Long> initiatorIds = pagedEvents.stream()
                .map(Event::getInitiatorId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserShortDto> initiatorMap = initiatorIds.isEmpty()
                ? Collections.emptyMap()
                : userClient.getUsers(initiatorIds).stream()
                .collect(Collectors.toMap(UserShortDto::getId, u -> u));

        // 8. Маппим в DTO
        List<EventShortDto> result = pagedEvents.stream()
                .map(e -> {
                    int confirmed = confirmedMap.getOrDefault(e.getId(), 0L).intValue();
                    UserShortDto initiator = e.getInitiatorId() != null
                            ? initiatorMap.get(e.getInitiatorId())
                            : null;
                    return EventMapper.toEventShortDtoWithStats(
                            e, confirmed, views.getOrDefault(e.getId(), 0L), initiator);
                })
                .collect(Collectors.toList());

        // 9. Сортировка по просмотрам
        if ("VIEWS".equals(params.getSort())) {
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

    @Override
    public void saveHit(String uri, String ip) {
        try {
            statsClient.saveHit(EndpointHitDto.builder()
                    .app(appName)
                    .uri(uri)
                    .ip(ip)
                    .timestamp(LocalDateTime.now())
                    .build());
            log.info("Статистика для {} сохранена", uri);
        } catch (Exception e) {
            log.error("Ошибка при сохранении статистики для {}: {}", uri, e.getMessage());
        }
    }

    private Map<Long, Long> getViewsForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .collect(Collectors.toList());

            String start = DateUtils.format(LocalDateTime.of(2000, 1, 1, 0, 0, 0));
            String end = DateUtils.format(LocalDateTime.now());

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