package ru.practicum.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.ParticipationRequestDto;
import ru.practicum.request.RequestStatus;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.repository.ParticipationRequestRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController {

    private final ParticipationRequestRepository requestRepository;

    @GetMapping("/count")
    public Long countByEventAndStatus(@RequestParam Long eventId,
                                      @RequestParam RequestStatus status) {
        return requestRepository.countByEventIdAndStatus(eventId, status);
    }

    @GetMapping
    public List<ParticipationRequestDto> findAllByEventId(@RequestParam Long eventId) {
        return requestRepository.findAllByEventId(eventId).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/by-ids")
    public List<ParticipationRequestDto> findAllByEventIdAndIdIn(@RequestParam Long eventId,
                                                                 @RequestParam List<Long> ids) {
        return requestRepository.findAllByEventIdAndIdIn(eventId, ids).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @PostMapping("/{id}/status")
    public ParticipationRequestDto updateStatus(@PathVariable Long id,
                                                @RequestParam RequestStatus status) {
        ParticipationRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Заявка с id=" + id + " не найдена"));
        request.setStatus(status);
        return RequestMapper.toParticipationRequestDto(requestRepository.save(request));
    }
}