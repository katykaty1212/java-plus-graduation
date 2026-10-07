package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.CategoryRepository;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.model.Category;
import ru.practicum.client.RequestClient;
import ru.practicum.client.UserClient;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.EventRepository;
import ru.practicum.event.dto.*;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.model.Location;
import ru.practicum.event.model.StateActionUser;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.ParticipationRequestDto;
import ru.practicum.request.RequestStatus;
import ru.practicum.user.UserShortDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrivateEventServiceImpl implements PrivateEventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserClient userClient;
    private final RequestClient requestClient;

    @Override
    public List<EventShortDto> getEvents(Long userId, int from, int size) {
        checkUserExists(userId);
        PageRequest page = PageRequest.of(from / size, size);

        UserShortDto initiator = userClient.getUser(userId);

        return eventRepository.findAllByInitiatorId(userId, page).stream()
                .map(e -> toEventShortDto(e, initiator))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto newEventDto) {
        if (!userClient.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Field: eventDate. Error: must be at least 2 hours in the future");
        }

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category with id=" + newEventDto.getCategory() + " was not found"));

        Event event = Event.builder()
                .annotation(newEventDto.getAnnotation())
                .category(category)
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())
                .location(new Location(newEventDto.getLocation().getLat(), newEventDto.getLocation().getLon()))
                .paid(newEventDto.getPaid() != null ? newEventDto.getPaid() : false)
                .participantLimit(newEventDto.getParticipantLimit() != null ? newEventDto.getParticipantLimit() : 0)
                .requestModeration(newEventDto.getRequestModeration() != null ? newEventDto.getRequestModeration() : true)
                .title(newEventDto.getTitle())
                .initiatorId(userId)
                .createdOn(LocalDateTime.now())
                .state(EventState.PENDING)
                .build();

        Event saved = eventRepository.save(event);
        UserShortDto initiator = userClient.getUser(userId);
        return toEventFullDto(saved, initiator);
    }

    @Override
    public EventFullDto getEventById(Long userId, Long eventId) {
        checkUserExists(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        UserShortDto initiator = userClient.getUser(userId);
        return toEventFullDto(event, initiator);
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        checkUserExists(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (updateRequest.getEventDate() != null) {
            if (updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("Field: eventDate. Error: must be at least 2 hours in the future");
            }
            event.setEventDate(updateRequest.getEventDate());
        }

        if (updateRequest.getAnnotation() != null) event.setAnnotation(updateRequest.getAnnotation());
        if (updateRequest.getDescription() != null) event.setDescription(updateRequest.getDescription());
        if (updateRequest.getPaid() != null) event.setPaid(updateRequest.getPaid());
        if (updateRequest.getParticipantLimit() != null) event.setParticipantLimit(updateRequest.getParticipantLimit());
        if (updateRequest.getRequestModeration() != null) event.setRequestModeration(updateRequest.getRequestModeration());
        if (updateRequest.getTitle() != null) event.setTitle(updateRequest.getTitle());

        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id=" + updateRequest.getCategory() + " was not found"));
            event.setCategory(category);
        }

        if (updateRequest.getLocation() != null) {
            event.setLocation(new Location(updateRequest.getLocation().getLat(), updateRequest.getLocation().getLon()));
        }

        if (updateRequest.getStateAction() != null) {
            if (updateRequest.getStateAction() == StateActionUser.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
            } else if (updateRequest.getStateAction() == StateActionUser.CANCEL_REVIEW) {
                event.setState(EventState.CANCELED);
            }
        }

        Event saved = eventRepository.save(event);
        UserShortDto initiator = userClient.getUser(userId);
        return toEventFullDto(saved, initiator);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        checkUserExists(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        return requestClient.findAllByEventId(event.getId());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(
            Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest) {

        checkUserExists(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (updateRequest.getRequestIds() == null || updateRequest.getRequestIds().isEmpty()) {
            throw new ValidationException("requestIds must not be empty");
        }
        if (updateRequest.getStatus() == null) {
            throw new ValidationException("status must not be null");
        }

        long confirmedCount = requestClient.countByEventAndStatus(eventId, RequestStatus.CONFIRMED);

        if (updateRequest.getStatus() == RequestStatus.CONFIRMED
                && event.getParticipantLimit() != null
                && event.getParticipantLimit() > 0
                && confirmedCount >= event.getParticipantLimit()) {
            throw new ConflictException("The participant limit has been reached");
        }

        List<ParticipationRequestDto> requests =
                requestClient.findAllByEventIdAndIdIn(eventId, updateRequest.getRequestIds());

        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        for (ParticipationRequestDto request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Status can be changed only for requests with PENDING status");
            }

            if (updateRequest.getStatus() == RequestStatus.CONFIRMED) {
                if (event.getParticipantLimit() == null
                        || event.getParticipantLimit() == 0
                        || confirmedCount < event.getParticipantLimit()) {
                    ParticipationRequestDto updated =
                            requestClient.updateStatus(request.getId(), RequestStatus.CONFIRMED);
                    confirmedRequests.add(updated);
                    confirmedCount++;
                } else {
                    ParticipationRequestDto updated =
                            requestClient.updateStatus(request.getId(), RequestStatus.REJECTED);
                    rejectedRequests.add(updated);
                }
            } else {
                ParticipationRequestDto updated =
                        requestClient.updateStatus(request.getId(), RequestStatus.REJECTED);
                rejectedRequests.add(updated);
            }
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests)
                .rejectedRequests(rejectedRequests)
                .build();
    }

    private void checkUserExists(Long userId) {
        if (!userClient.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }

    private EventShortDto toEventShortDto(Event event, UserShortDto initiator) {
        int confirmed = requestClient.countByEventAndStatus(event.getId(), RequestStatus.CONFIRMED).intValue();
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(new CategoryDto(event.getCategory().getId(), event.getCategory().getName()))
                .confirmedRequests(confirmed)
                .eventDate(event.getEventDate())
                .initiator(initiator)
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(0L)
                .build();
    }

    private EventFullDto toEventFullDto(Event event, UserShortDto initiator) {
        int confirmed = requestClient.countByEventAndStatus(event.getId(), RequestStatus.CONFIRMED).intValue();
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(new CategoryDto(event.getCategory().getId(), event.getCategory().getName()))
                .confirmedRequests(confirmed)
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(initiator)
                .location(new LocationDto(event.getLocation().getLat(), event.getLocation().getLon()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .views(0L)
                .build();
    }
}