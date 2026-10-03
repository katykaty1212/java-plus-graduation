package ru.practicum.event;

import lombok.experimental.UtilityClass;
import ru.practicum.category.CategoryMapper;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.LocationDto;
import ru.practicum.event.model.Event;
import ru.practicum.user.UserMapper;

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

        if (event.getInitiator() != null) {
            dto.setInitiator(UserMapper.toUserShortDto(event.getInitiator()));
        }

        return dto;
    }

    public static EventShortDto toEventShortDto(Event event) {
        if (event == null) {
            return null;
        }

        EventShortDto.EventShortDtoBuilder builder = EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .eventDate(event.getEventDate())
                .paid(event.getPaid())
                .title(event.getTitle())
                .confirmedRequests(0)
                .views(0L);

        if (event.getCategory() != null) {
            builder.category(CategoryMapper.toCategoryDto(event.getCategory()));
        }

        if (event.getInitiator() != null) {
            builder.initiator(UserMapper.toUserShortDto(event.getInitiator()));
        }

        return builder.build();
    }

    public static EventShortDto toEventShortDtoWithStats(Event event, int confirmedRequests, Long views) {
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
                .views(views != null ? views : 0L);

        if (event.getCategory() != null) {
            builder.category(CategoryMapper.toCategoryDto(event.getCategory()));
        }

        if (event.getInitiator() != null) {
            builder.initiator(UserMapper.toUserShortDto(event.getInitiator()));
        }

        return builder.build();
    }

    public static EventFullDto toEventFullDtoWithStats(Event event, int confirmedRequests, Long views) {
        if (event == null) {
            return null;
        }

        EventFullDto dto = new EventFullDto();

        // Все поля из Event
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

        // Реальные значения статистики
        dto.setConfirmedRequests(confirmedRequests);
        dto.setViews(views != null ? views : 0L);

        // Категория
        if (event.getCategory() != null) {
            dto.setCategory(CategoryMapper.toCategoryDto(event.getCategory()));
        }

        // Инициатор
        if (event.getInitiator() != null) {
            dto.setInitiator(UserMapper.toUserShortDto(event.getInitiator()));
        }

        // Локация
        if (event.getLocation() != null) {
            LocationDto locationDto = new LocationDto();
            locationDto.setLat(event.getLocation().getLat());
            locationDto.setLon(event.getLocation().getLon());
            dto.setLocation(locationDto);
        }

        return dto;
    }
}