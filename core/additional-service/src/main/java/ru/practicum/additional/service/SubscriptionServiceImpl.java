package ru.practicum.additional.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.additional.client.UserClient;
import ru.practicum.additional.dto.NewSubscriptionDto;
import ru.practicum.additional.dto.SubscriptionDto;
import ru.practicum.additional.dto.UpdateSubscriptionDto;
import ru.practicum.additional.mapper.SubscriptionMapper;
import ru.practicum.additional.model.Subscription;
import ru.practicum.additional.model.SubscriptionStatus;
import ru.practicum.additional.repository.SubscriptionRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.UserShortDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserClient userClient;

    @Override
    @Transactional
    public SubscriptionDto subscribe(Long subscriberId, NewSubscriptionDto newSubscriptionDto) {
        if (subscriberId.equals(newSubscriptionDto.getPublisherId())) {
            throw new ConflictException("Нельзя подписаться на самого себя");
        }

        if (!Boolean.TRUE.equals(userClient.existsById(subscriberId))) {
            throw new NotFoundException("Пользователь с id=" + subscriberId + " не найден");
        }

        if (!Boolean.TRUE.equals(userClient.existsById(newSubscriptionDto.getPublisherId()))) {
            throw new NotFoundException("Пользователь с id=" + newSubscriptionDto.getPublisherId() + " не найден");
        }

        if (subscriptionRepository.existsBySubscriberIdAndPublisherId(subscriberId, newSubscriptionDto.getPublisherId())) {
            throw new ConflictException("Уже подписан на пользователя с id=" + newSubscriptionDto.getPublisherId());
        }

        Subscription subscription = Subscription.builder()
                .subscriberId(subscriberId)
                .publisherId(newSubscriptionDto.getPublisherId())
                .type(newSubscriptionDto.getType())
                .status(SubscriptionStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        subscription = subscriptionRepository.save(subscription);
        log.info("Пользователь {} подписался на {}", subscriberId, newSubscriptionDto.getPublisherId());

        UserShortDto subscriber = userClient.getUser(subscriberId);
        UserShortDto publisher = userClient.getUser(newSubscriptionDto.getPublisherId());

        return SubscriptionMapper.toSubscriptionDto(subscription, subscriber, publisher);
    }

    @Override
    @Transactional
    public void unsubscribe(Long subscriberId, Long publisherId) {
        Subscription subscription = subscriptionRepository
                .findBySubscriberIdAndPublisherId(subscriberId, publisherId)
                .orElseThrow(() -> new NotFoundException("Подписка не найдена"));

        subscriptionRepository.delete(subscription);
        log.info("Пользователь {} отписался от {}", subscriberId, publisherId);
    }

    @Override
    @Transactional
    public SubscriptionDto updateSubscription(Long subscriberId, Long publisherId, UpdateSubscriptionDto updateDto) {
        Subscription subscription = subscriptionRepository
                .findBySubscriberIdAndPublisherId(subscriberId, publisherId)
                .orElseThrow(() -> new NotFoundException("Подписка не найдена"));

        if (updateDto.getType() != null) {
            subscription.setType(updateDto.getType());
        }
        if (updateDto.getStatus() != null) {
            subscription.setStatus(updateDto.getStatus());
        }

        Subscription updated = subscriptionRepository.save(subscription);
        log.info("Подписка обновлена: subscriber={}, publisher={}", subscriberId, publisherId);

        UserShortDto subscriber = userClient.getUser(subscriberId);
        UserShortDto publisher = userClient.getUser(publisherId);

        return SubscriptionMapper.toSubscriptionDto(updated, subscriber, publisher);
    }

    @Override
    public List<SubscriptionDto> getSubscriptions(Long userId, int from, int size) {
        if (!Boolean.TRUE.equals(userClient.existsById(userId))) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        Pageable pageable = PageRequest.of(from / size, size);
        List<Subscription> subscriptions = subscriptionRepository.findAllBySubscriberId(userId, pageable).getContent();

        if (subscriptions.isEmpty()) {
            return Collections.emptyList();
        }

        // Собираем все ID пользователей (subscriber + publisher) и получаем одним batch-запросом
        List<Long> userIds = subscriptions.stream()
                .flatMap(s -> java.util.stream.Stream.of(s.getSubscriberId(), s.getPublisherId()))
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserShortDto> usersMap = userClient.getUsers(userIds).stream()
                .collect(Collectors.toMap(UserShortDto::getId, u -> u));

        return subscriptions.stream()
                .map(s -> SubscriptionMapper.toSubscriptionDto(
                        s,
                        usersMap.get(s.getSubscriberId()),
                        usersMap.get(s.getPublisherId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<SubscriptionDto> getSubscribers(Long userId, int from, int size) {
        if (!Boolean.TRUE.equals(userClient.existsById(userId))) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        Pageable pageable = PageRequest.of(from / size, size);
        List<Subscription> subscriptions = subscriptionRepository.findAllByPublisherId(userId, pageable).getContent();

        if (subscriptions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = subscriptions.stream()
                .flatMap(s -> java.util.stream.Stream.of(s.getSubscriberId(), s.getPublisherId()))
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserShortDto> usersMap = userClient.getUsers(userIds).stream()
                .collect(Collectors.toMap(UserShortDto::getId, u -> u));

        return subscriptions.stream()
                .map(s -> SubscriptionMapper.toSubscriptionDto(
                        s,
                        usersMap.get(s.getSubscriberId()),
                        usersMap.get(s.getPublisherId())))
                .collect(Collectors.toList());
    }

    @Override
    public SubscriptionDto getSubscription(Long subscriberId, Long publisherId) {
        Subscription subscription = subscriptionRepository
                .findBySubscriberIdAndPublisherId(subscriberId, publisherId)
                .orElseThrow(() -> new NotFoundException("Подписка не найдена"));

        UserShortDto subscriber = userClient.getUser(subscriberId);
        UserShortDto publisher = userClient.getUser(publisherId);

        return SubscriptionMapper.toSubscriptionDto(subscription, subscriber, publisher);
    }
}