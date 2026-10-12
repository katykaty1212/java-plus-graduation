package ru.practicum.event.service;

import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.PublicEventSearchParams;

import java.util.List;

public interface PublicEventService {

    List<EventShortDto> getPublishedEvents(PublicEventSearchParams params);

    EventFullDto getPublishedEventById(Long eventId, Long userId);

    List<EventShortDto> getRecommendations(Long userId, int maxResults);

    void likeEvent(Long eventId, Long userId);
}