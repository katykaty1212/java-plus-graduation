package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.EventInternalDto;
import ru.practicum.event.EventRepository;
import ru.practicum.event.model.Event;
import ru.practicum.exception.NotFoundException;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventRepository eventRepository;

    @GetMapping("/{id}")
    public EventInternalDto getEvent(@PathVariable Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + id + " не найдено"));
        return EventInternalDto.builder()
                .id(event.getId())
                .initiatorId(event.getInitiatorId())
                .state(event.getState().name())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .build();
    }
}