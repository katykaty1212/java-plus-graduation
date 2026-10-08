package ru.practicum.additional.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.additional.dto.NewSubscriptionDto;
import ru.practicum.additional.dto.SubscriptionDto;
import ru.practicum.additional.dto.UpdateSubscriptionDto;
import ru.practicum.additional.service.SubscriptionService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionDto subscribe(
            @PathVariable Long userId,
            @Valid @RequestBody NewSubscriptionDto newSubscriptionDto) {
        return subscriptionService.subscribe(userId, newSubscriptionDto);
    }

    @DeleteMapping("/{publisherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(
            @PathVariable Long userId,
            @PathVariable Long publisherId) {
        subscriptionService.unsubscribe(userId, publisherId);
    }

    @PatchMapping("/{publisherId}")
    public SubscriptionDto updateSubscription(
            @PathVariable Long userId,
            @PathVariable Long publisherId,
            @Valid @RequestBody UpdateSubscriptionDto updateDto) {
        return subscriptionService.updateSubscription(userId, publisherId, updateDto);
    }

    @GetMapping
    public List<SubscriptionDto> getSubscriptions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        return subscriptionService.getSubscriptions(userId, from, size);
    }

    @GetMapping("/subscribers")
    public List<SubscriptionDto> getSubscribers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        return subscriptionService.getSubscribers(userId, from, size);
    }

    @GetMapping("/{publisherId}/status")
    public SubscriptionDto getSubscriptionStatus(
            @PathVariable Long userId,
            @PathVariable Long publisherId) {
        return subscriptionService.getSubscription(userId, publisherId);
    }
}