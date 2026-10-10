package ru.practicum.analyzer.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.analyzer.model.UserInteraction;
import ru.practicum.analyzer.repository.UserInteractionRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private final UserInteractionRepository userInteractionRepository;

    @KafkaListener(topics = "${kafka.topics.user-actions}", groupId = "analyzer")
    @Transactional
    public void consume(UserActionAvro action) {
        double newWeight = toWeight(action.getActionType());

        userInteractionRepository.findByUserIdAndEventId(action.getUserId(), action.getEventId())
                .ifPresentOrElse(
                        existing -> {
                            if (newWeight > existing.getWeight()) {
                                existing.setWeight(newWeight);
                                existing.setLastActionAt(toLocalDateTime(action.getTimestamp()));
                                userInteractionRepository.save(existing);
                            }
                        },
                        () -> {
                            UserInteraction ui = UserInteraction.builder()
                                    .userId(action.getUserId())
                                    .eventId(action.getEventId())
                                    .weight(newWeight)
                                    .lastActionAt(toLocalDateTime(action.getTimestamp()))
                                    .build();
                            userInteractionRepository.save(ui);
                        }
                );

        log.debug("Обработано действие: userId={}, eventId={}, weight={}",
                action.getUserId(), action.getEventId(), newWeight);
    }

    private double toWeight(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}