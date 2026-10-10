package ru.practicum.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.CollectorClient;
import ru.practicum.event.EventInternalDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.ParticipationRequestDto;
import ru.practicum.request.RequestStatus;
import ru.practicum.request.client.EventClient;
import ru.practicum.request.client.UserClient;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.repository.ParticipationRequestRepository;
import ru.practicum.stats.proto.collector.ActionTypeProto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PrivateRequestServiceImpl implements PrivateRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final EventClient eventClient;
    private final UserClient userClient;
    private final CollectorClient collectorClient;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        checkUserExists(userId);
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        checkUserExists(userId);

        EventInternalDto event = eventClient.getEvent(eventId);

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Заявка уже существует для пользователя id=" + userId + " и события id=" + eventId);
        }

        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Инициатор не может подать заявку на своё событие");
        }

        if (!"PUBLISHED".equals(event.getState())) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() != null && event.getParticipantLimit() > 0
                && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников");
        }

        RequestStatus initialStatus = RequestStatus.PENDING;
        if (Boolean.FALSE.equals(event.getRequestModeration())
                || event.getParticipantLimit() == null
                || event.getParticipantLimit() == 0) {
            initialStatus = RequestStatus.CONFIRMED;
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .requesterId(userId)
                .eventId(eventId)
                .status(initialStatus)
                .createdDate(LocalDateTime.now())
                .build();

        ParticipationRequest saved = requestRepository.save(request);

        collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);

        return RequestMapper.toParticipationRequestDto(saved);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        checkUserExists(userId);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Заявка с id=" + requestId + " не найдена"));

        if (!request.getRequesterId().equals(userId)) {
            throw new NotFoundException("Заявка с id=" + requestId + " не принадлежит пользователю с id=" + userId);
        }

        request.setStatus(RequestStatus.CANCELED);
        return RequestMapper.toParticipationRequestDto(requestRepository.save(request));
    }

    private void checkUserExists(Long userId) {
        Boolean exists = userClient.existsById(userId);
        if (!Boolean.TRUE.equals(exists)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }
}