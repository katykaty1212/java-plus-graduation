package ru.practicum.event;

import lombok.experimental.UtilityClass;
import ru.practicum.category.CategoryMapper;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.LocationDto;
import ru.practicum.event.model.Event;
import ru.practicum.user.UserShortDto;

@UtilityClass
public class EventMapper {

    public static EventFullDto toEventFullDto(Event event) {
        if (event == null) {
            return null;
        }

        EventFullDto dto = new EventFullDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());

        if (event.getCategory() != null) {
            dto.setCategory(CategoryMapper.toCategoryDto(event.getCategory()));
        }

        return dto;
    }

    public static EventShortDto toEventShortDto(Event event) {
        return toEventShortDtoWithStats(event, 0, 0.0);
    }

    public static EventShortDto toEventShortDtoWithStats(Event event, int confirmedRequests, Double rating) {
        if (event == null) {
            return null;
        }

        EventShortDto.EventShortDtoBuilder builder = EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .eventDate(event.getEventDate())
                .paid(event.getPaid())
                .title(event.getTitle())
                .confirmedRequests(confirmedRequests)
                .rating(rating != null ? rating : 0.0);

        if (event.getCategory() != null) {
            builder.category(CategoryMapper.toCategoryDto(event.getCategory()));
        }

        return builder.build();
    }

    public static EventShortDto toEventShortDtoWithStats(Event event, int confirmedRequests, Double rating,
                                                         UserShortDto initiator) {
        EventShortDto dto = toEventShortDtoWithStats(event, confirmedRequests, rating);
        if (dto != null) {
            dto.setInitiator(initiator);
        }
        return dto;
    }

    public static EventFullDto toEventFullDtoWithStats(Event event, int confirmedRequests, Double rating) {
        return toEventFullDtoWithStats(event, confirmedRequests, rating, null);
    }

    public static EventFullDto toEventFullDtoWithStats(Event event, int confirmedRequests, Double rating,
                                                       UserShortDto initiator) {
        if (event == null) {
            return null;
        }

        EventFullDto dto = new EventFullDto();

        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setAnnotation(event.getAnnotation());
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate());
        dto.setCreatedOn(event.getCreatedOn());
        dto.setPublishedOn(event.getPublishedOn());
        dto.setPaid(event.getPaid());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setRequestModeration(event.getRequestModeration());
        dto.setState(event.getState());

        dto.setConfirmedRequests(confirmedRequests);
        dto.setRating(rating != null ? rating : 0.0);

        if (event.getCategory() != null) {
            dto.setCategory(CategoryMapper.toCategoryDto(event.getCategory()));
        }

        dto.setInitiator(initiator);

        if (event.getLocation() != null) {
            LocationDto locationDto = new LocationDto();
            locationDto.setLat(event.getLocation().getLat());
            locationDto.setLon(event.getLocation().getLon());
            dto.setLocation(locationDto);
        }

        return dto;
    }
}