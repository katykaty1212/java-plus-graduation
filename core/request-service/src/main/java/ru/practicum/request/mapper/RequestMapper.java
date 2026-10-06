package ru.practicum.request.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.request.ParticipationRequestDto;
import ru.practicum.request.model.ParticipationRequest;

@UtilityClass
public class RequestMapper {

    public static ParticipationRequestDto toParticipationRequestDto(ParticipationRequest request) {
        if (request == null) {
            return null;
        }

        return ParticipationRequestDto.builder()
                .id(request.getId())
                .createdDate(request.getCreatedDate())
                .event(request.getEventId())
                .requester(request.getRequesterId())
                .status(request.getStatus())
                .build();
    }
}