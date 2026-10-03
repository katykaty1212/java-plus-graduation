package ru.practicum.request;

import lombok.experimental.UtilityClass;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.model.ParticipationRequest;

@UtilityClass
public class RequestMapper {

    public static ParticipationRequestDto toParticipationRequestDto(ParticipationRequest request) {
        if (request == null) {
            return null;
        }

        ParticipationRequestDto dto = new ParticipationRequestDto();

        dto.setId(request.getId());
        dto.setCreatedDate(request.getCreatedDate());

        // Получаем ID события, если само событие существует
        if (request.getEvent() != null) {
            dto.setEvent(request.getEvent().getId());
        }

        // Получаем ID пользователя (заявителя), если он существует
        if (request.getRequester() != null) {
            dto.setRequester(request.getRequester().getId());
        }

        // Так как в DTO статус тоже является Enum (RequestStatus),
        // просто передаем его напрямую без .name()
        if (request.getStatus() != null) {
            dto.setStatus(request.getStatus());
        }

        return dto;
    }
}