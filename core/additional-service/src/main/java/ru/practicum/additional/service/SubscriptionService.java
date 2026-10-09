package ru.practicum.additional.service;

import ru.practicum.additional.dto.NewSubscriptionDto;
import ru.practicum.additional.dto.SubscriptionDto;
import ru.practicum.additional.dto.UpdateSubscriptionDto;

import java.util.List;

public interface SubscriptionService {

    SubscriptionDto subscribe(Long subscriberId, NewSubscriptionDto newSubscriptionDto);

    void unsubscribe(Long subscriberId, Long publisherId);

    SubscriptionDto updateSubscription(Long subscriberId, Long publisherId, UpdateSubscriptionDto updateDto);

    List<SubscriptionDto> getSubscriptions(Long userId, int from, int size);

    List<SubscriptionDto> getSubscribers(Long userId, int from, int size);

    SubscriptionDto getSubscription(Long subscriberId, Long publisherId);
}