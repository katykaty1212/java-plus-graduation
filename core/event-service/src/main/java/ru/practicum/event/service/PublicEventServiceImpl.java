package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.AnalyzerClient;
import ru.practicum.client.CollectorClient;
import ru.practicum.client.RequestClient;
import ru.practicum.client.UserClient;
import ru.practicum.event.EventMapper;
import ru.practicum.event.EventRepository;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.PublicEventSearchParams;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.RequestStatus;
import ru.practicum.stats.proto.collector.ActionTypeProto;
import ru.practicum.stats.proto.dashboard.RecommendedEventProto;
import ru.practicum.user.UserShortDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
    private final AnalyzerClient analyzerClient;
    private final CollectorClient collectorClient;

    @Override
    public List<EventShortDto> getPublishedEvents(PublicEventSearchParams params) {
        if (params.getRangeStart() != null && params.getRangeEnd() != null
                && params.getRangeEnd().isBefore(params.getRangeStart())) {
            throw new ValidationException("Дата окончания должна быть позже даты начала");
        }

        LocalDateTime start = params.getRangeStart() != null
                ? params.getRangeStart() : LocalDateTime.now();
        LocalDateTime end = params.getRangeEnd() != null
                ? params.getRangeEnd() : LocalDateTime.now().plusYears(100);

        Sort sortBy = Sort.unsorted();
        if ("EVENT_DATE".equals(params.getSort())) {
            sortBy = Sort.by("eventDate").ascending();
        }
        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize(), sortBy);

        List<Event> allEvents = eventRepository.findAllByStateAndEventDateBetween(
                EventState.PUBLISHED, start, end);

        if (allEvents.isEmpty()) {
            return Collections.emptyList();
        }

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

        int startIndex = (int) pageable.getOffset();
        int endIndex = Math.min(startIndex + pageable.getPageSize(), filtered.size());

        if (startIndex >= filtered.size()) {
            return Collections.emptyList();
        }

        List<Event> pagedEvents = filtered.subList(startIndex, endIndex);

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

        Map<Long, Double> ratings = fetchRatings(eventIds);

        List<EventShortDto> result = pagedEvents.stream()
                .map(e -> {
                    int confirmed = confirmedMap.getOrDefault(e.getId(), 0L).intValue();
                    UserShortDto initiator = e.getInitiatorId() != null
                            ? initiatorMap.get(e.getInitiatorId())
                            : null;
                    return EventMapper.toEventShortDtoWithStats(
                            e, confirmed, ratings.getOrDefault(e.getId(), 0.0), initiator);
                })
                .collect(Collectors.toList());

        if ("VIEWS".equals(params.getSort())) {
            result.sort(Comparator.comparing(EventShortDto::getRating));
        }

        return result;
    }

    @Override
    public EventFullDto getPublishedEventById(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        if (userId != null) {
            collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
        }

        Long confirmedLong = requestClient.countByEventAndStatus(eventId, RequestStatus.CONFIRMED);
        int confirmedRequests = confirmedLong != null ? confirmedLong.intValue() : 0;

        double rating = fetchRatings(List.of(eventId)).getOrDefault(eventId, 0.0);

        UserShortDto initiator = event.getInitiatorId() != null
                ? userClient.getUser(event.getInitiatorId())
                : null;

        return EventMapper.toEventFullDtoWithStats(event, confirmedRequests, rating, initiator);
    }

    @Override
    public List<EventShortDto> getRecommendations(Long userId, int maxResults) {
        List<Long> recommendedIds = analyzerClient
                .getRecommendationsForUser(userId, maxResults)
                .map(RecommendedEventProto::getEventId)
                .collect(Collectors.toList());

        if (recommendedIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Event> events = eventRepository.findAllById(recommendedIds);
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> confirmedMap = requestClient.countByEventIdsAndStatus(
                recommendedIds, RequestStatus.CONFIRMED);

        List<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserShortDto> initiatorMap = initiatorIds.isEmpty()
                ? Collections.emptyMap()
                : userClient.getUsers(initiatorIds).stream()
                .collect(Collectors.toMap(UserShortDto::getId, u -> u));

        Map<Long, Double> ratings = fetchRatings(recommendedIds);

        return events.stream()
                .map(e -> {
                    int confirmed = confirmedMap.getOrDefault(e.getId(), 0L).intValue();
                    UserShortDto initiator = e.getInitiatorId() != null
                            ? initiatorMap.get(e.getInitiatorId())
                            : null;
                    return EventMapper.toEventShortDtoWithStats(
                            e, confirmed, ratings.getOrDefault(e.getId(), 0.0), initiator);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void likeEvent(Long eventId, Long userId) {
        Boolean visited = requestClient.existsByRequesterIdAndEventIdAndStatus(
                userId, eventId, RequestStatus.CONFIRMED);

        if (!Boolean.TRUE.equals(visited)) {
            throw new ValidationException("Пользователь может лайкать только посещённые мероприятия");
        }

        collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    private Map<Long, Double> fetchRatings(List<Long> eventIds) {
        if (eventIds.isEmpty()) return Collections.emptyMap();
        Map<Long, Double> result = new HashMap<>();
        try {
            analyzerClient.getInteractionsCount(eventIds).forEach(
                    proto -> result.put(proto.getEventId(), proto.getScore())
            );
        } catch (Exception e) {
            log.error("Не удалось получить рейтинги: {}", e.getMessage());
        }
        return result;
    }
}