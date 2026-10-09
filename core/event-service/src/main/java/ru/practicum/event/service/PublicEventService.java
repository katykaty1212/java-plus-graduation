package ru.practicum.event.service;

import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.PublicEventSearchParams;
import ru.practicum.event.model.Event;

import java.util.List;

public interface PublicEventService {

    List<EventShortDto> getPublishedEvents(PublicEventSearchParams params);

    Event getPublishedEventById(Long eventId);

    void saveHit(String uri, String ip);
}