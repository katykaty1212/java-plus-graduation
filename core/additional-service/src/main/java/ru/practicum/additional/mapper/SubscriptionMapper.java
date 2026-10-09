package ru.practicum.additional.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.additional.dto.SubscriptionDto;
import ru.practicum.additional.model.Subscription;
import ru.practicum.user.UserShortDto;

@UtilityClass
public class SubscriptionMapper {

    public static SubscriptionDto toSubscriptionDto(Subscription subscription,
                                                    UserShortDto subscriber,
                                                    UserShortDto publisher) {
        if (subscription == null) {
            return null;
        }

        return SubscriptionDto.builder()
                .id(subscription.getId())
                .subscriber(subscriber)
                .publisher(publisher)
                .status(subscription.getStatus())
                .type(subscription.getType())
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}