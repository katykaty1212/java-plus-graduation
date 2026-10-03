package ru.practicum.event.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.model.Event;

import java.time.LocalDateTime;
import java.util.List;

public interface PublicEventService {

    List<EventShortDto> getPublishedEvents(
            String text,
            List<Long> categories,
            Boolean paid,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable,
            String sort);

    Event getPublishedEventById(Long eventId);
}